package com.github.engahmedatef.fcm.internal.service;

import com.github.engahmedatef.fcm.api.FcmTokenService;
import com.github.engahmedatef.fcm.api.ReactiveFcmClient;
import com.github.engahmedatef.fcm.exception.*;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContextFactory;
import com.github.engahmedatef.fcm.internal.observability.ObservationManager;
import com.github.engahmedatef.fcm.internal.service.sender.FcmMessageSender;
import com.github.engahmedatef.fcm.internal.service.sender.FcmSenderResponse;
import com.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import com.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import com.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import com.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.model.FcmSendResult;
import com.github.engahmedatef.fcm.spi.*;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
public class DefaultReactiveFcmClient implements ReactiveFcmClient {
    private final FcmPayloadBuilder payloadBuilder;
    private final FcmMessageSender sender;
    private final FcmResponseErrorHandler errorHandler;
    private final FcmRetryPolicy retryPolicy;
    private final FcmTokenService tokenService;
    private final int concurrency;
    private final int batchSize;
    private final double failureThresholdPerBatch;
    private final String projectId;
    private final FcmLogger fcmLogger;
    private final ValidatorChain validatorChain;
    private final EnricherChain enricherChain;
    private final InterceptorChain interceptorChain;
    private final FcmExecutionContextFactory executionContextFactory;
    private final ObservationManager observationManager;
    private final FcmMetricsRecorder metricsRecorder;

    @Override
    public Mono<FcmSendResult> sendToDevice(FcmDevice device, FcmMessage message) {
        return send(new DeviceTarget(device), message);
    }

    @Override
    public Mono<FcmSendResult> sendToTopic(String topic, FcmMessage message) {
        return send(new TopicTarget(topic), message);
    }

    @Override
    public Mono<FcmSendResult> sendToCondition(String condition, FcmMessage message) {
        return send(new ConditionTarget(condition), message);
    }

    @Override
    public Mono<Void> sendToDevices(Collection<FcmDevice> devices, FcmMessage message) {
        FcmExecutionContext context = executionContextFactory.create(projectId, null);
        return sendDevicesInternal(context, Flux.fromIterable(devices), message);
    }

    @Override
    public Mono<Void> sendToUser(Object userId, FcmMessage message) {
        if (tokenService == null)
            return Mono.error(new FcmTokenServiceNotDefinedException("FcmTokenService is not configured"));

        FcmExecutionContext context = executionContextFactory.create(projectId, null);
        return tokenService.getUserDevices(userId)
                .flatMap(devices -> {
                    if (devices.isEmpty())
                        return Mono.empty();

                    return sendDevicesInternal(context, Flux.fromIterable(devices), message);
                });
    }

    private Mono<FcmSendResult> send(FcmTarget target, FcmMessage message) {
        if (message == null)
            return Mono.error(new FcmInvalidRequestException("Message cannot be null"));

        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        return Mono.deferContextual(contextView -> {
            FcmExecutionContext parentContext = contextView.getOrDefault(FcmExecutionContext.KEY, null);
            FcmExecutionContext executionContext = executionContextFactory.create(projectId, target, parentContext);

            observationManager.start(executionContext);
            fcmLogger.info(FcmLogEvent.SEND_STARTED, executionContext.getAttributes());

            Map<String, Object> attributes = new HashMap<>();
            FcmEnricherContext enricherContext = new FcmEnricherContext(target, projectId, attributes);

            return validatorChain.apply(target, message)
                    .then(enricherChain.apply(enricherContext, message))
                    .flatMap(enrichedMessage -> {
                        FcmInterceptorContext interceptorContext = new FcmInterceptorContext(target, enrichedMessage, projectId, attributes);
                        return interceptorChain.apply(interceptorContext, payloadBuilder.build(target, enrichedMessage));
                    })
                    .flatMap(sender::send)
                    .flatMap(response -> handleResponse(target, response))
                    .retryWhen(retryPolicy.create(executionContext))
                    .doOnSuccess(result -> {
                        metricsRecorder.increment(FcmMetrics.MESSAGES_SENT, executionContext,
                                FcmMetrics.Tags.OUTCOME, "success");
                        fcmLogger.info(FcmLogEvent.SEND_SUCCEEDED, new HashMap<>(executionContext.getAttributes()) {{
                            put(FcmMetrics.Tags.MESSAGE_ID, result.getMessageId());
                        }});
                    })
                    .doOnError(ex -> {
                        errorRef.set(ex);
                        metricsRecorder.increment(FcmMetrics.MESSAGES_SENT, executionContext,
                                FcmMetrics.Tags.OUTCOME, "failure");
                        metricsRecorder.increment(FcmMetrics.ERRORS_CLASSIFIED, executionContext,
                                FcmMetrics.Tags.ERROR_CATEGORY, ex.getClass().getSimpleName());
                        fcmLogger.error(FcmLogEvent.SEND_FAILED, ex, new HashMap<>(executionContext.getAttributes()) {{
                            put(FcmMetrics.Tags.REASON, ex.getClass().getSimpleName());
                        }});
                    })
                    .doFinally(signal -> {
                        switch (signal) {
                            case ON_COMPLETE -> observationManager.success(executionContext);
                            case ON_ERROR -> observationManager.failure(executionContext, errorRef.get() != null ? errorRef.get() : new FcmUnknownException("Unknown reactive error"));
                            case CANCEL -> observationManager.cancel(executionContext);
                            default -> { }
                        }

                        observationManager.stop(executionContext);
                        metricsRecorder.recordTimer(FcmMetrics.MESSAGES_DURATION, executionContext,
                                executionContext.duration());
                    })
                    .contextWrite(c -> c.put(FcmExecutionContext.KEY, executionContext));
        });
    }

    private Mono<FcmSendResult> handleResponse(FcmTarget target, FcmSenderResponse response) {
        if (response.success())
            return Mono.just(FcmSendResult.success(response.successResponse().name()));
        else
            return errorHandler.handle(target, response.errorResponse());
    }

    private Mono<Void> sendDevicesInternal(FcmExecutionContext context, Flux<FcmDevice> devices, FcmMessage message) {
        observationManager.start(context);
        fcmLogger.info(FcmLogEvent.BATCH_SEND_STARTED, context.getAttributes());

        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        return devices
                .buffer(batchSize)
                .concatMap(batch ->
                        Flux.fromIterable(batch)
                                .flatMap(device -> send(new DeviceTarget(device), message)
                                        .onErrorResume(ex -> Mono.just(FcmSendResult.failure(ex))), concurrency)
                                .collectList()
                                .flatMap(results -> handleBatchResults(context, batch, results))
                )
                .doOnError(errorRef::set)
                .doFinally(signal -> {
                    switch (signal) {
                        case ON_COMPLETE -> observationManager.success(context);
                        case ON_ERROR -> observationManager.failure(context, errorRef.get() != null ? errorRef.get() : new FcmUnknownException("Unknown reactive error"));
                        case CANCEL -> observationManager.cancel(context);
                        default -> { }
                    }

                    observationManager.stop(context);
                })
                .then()
                .contextWrite(c -> c.put(FcmExecutionContext.KEY, context));
    }

    private Mono<Void> handleBatchResults(FcmExecutionContext context, List<FcmDevice> batch, List<FcmSendResult> results) {
        long failures = results.stream().filter(result -> !result.isSuccess()).count();
        double failureRate = (double) failures / results.size();

        context.putAttribute(FcmMetrics.Tags.BATCH_SIZE, batch.size());
        context.putAttribute(FcmMetrics.Tags.CONCURRENCY, concurrency);
        context.putAttribute(FcmMetrics.Tags.FAILURES, failures);
        context.putAttribute(FcmMetrics.Tags.SUCCESSES, results.size() - failures);
        context.putAttribute(FcmMetrics.Tags.FAILURE_RATE, failureRate);

        metricsRecorder.recordDistribution(FcmMetrics.BATCH_SIZE, context, (double) batch.size());
        metricsRecorder.recordDistribution(FcmMetrics.BATCH_FAILURE_RATE, context, failureRate);

        fcmLogger.info(FcmLogEvent.BATCH_SEND_COMPLETED, context.getAttributes());
        context.getObservation().lowCardinalityKeyValue(FcmMetrics.Tags.FAILURE_RATE, String.valueOf(failureRate));

        if (failureRate >= failureThresholdPerBatch) {
            metricsRecorder.increment(FcmMetrics.BATCH_ABORTED, context);
            return Mono.error(new FcmBatchExceededThresholdException("Threshold exceeded"));
        }

        return Mono.empty();
    }
}
