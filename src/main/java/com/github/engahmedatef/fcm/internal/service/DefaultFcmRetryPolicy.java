package com.github.engahmedatef.fcm.internal.service;

import com.github.engahmedatef.fcm.exception.FcmRetryableException;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.internal.observability.ObservationManager;
import com.github.engahmedatef.fcm.spi.FcmLogEvent;
import com.github.engahmedatef.fcm.spi.FcmLogger;
import com.github.engahmedatef.fcm.spi.FcmRetryPolicy;
import lombok.AllArgsConstructor;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@AllArgsConstructor
public class DefaultFcmRetryPolicy implements FcmRetryPolicy {
    private final int maxRetries;
    private final Duration baseBackoff;
    private final Duration maxBackoff;
    private final FcmMetricsRecorder metricsRecorder;
    private final FcmLogger fcmLogger;
    private final ObservationManager observationManager;

    @Override
    public Retry create(FcmExecutionContext executionContext) {
        return Retry.backoff(maxRetries, baseBackoff)
                .maxBackoff(maxBackoff)
                .jitter(0.3)
                .filter(this::isRetryable)
                .doBeforeRetry(retrySignal -> recordRetry(retrySignal, executionContext))
                .onRetryExhaustedThrow((spec, signal) -> {
                    String exceptionName = signal.failure().getClass().getSimpleName();
                    metricsRecorder.increment(FcmMetrics.RETRIES_EXHAUSTED, executionContext,
                            FcmMetrics.Tags.EXCEPTION, exceptionName);
                    return signal.failure();
                });
    }

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientRequestException ||
               throwable instanceof FcmRetryableException;
    }

    private void recordRetry(Retry.RetrySignal signal, FcmExecutionContext executionContext) {
        String exceptionName = signal.failure().getClass().getSimpleName();

        metricsRecorder.increment(FcmMetrics.MESSAGES_RETRIED,
                executionContext, FcmMetrics.Tags.EXCEPTION, exceptionName);

        fcmLogger.warn(FcmLogEvent.RETRY_ATTEMPT, Map.of(
                FcmMetrics.Tags.EXCEPTION, exceptionName,
                FcmMetrics.Tags.RETRY_COUNT, signal.totalRetries() + 1,
                FcmMetrics.Tags.CORRELATION_ID, executionContext.getCorrelationId()
        ));

        observationManager.retry(executionContext, signal.failure());
    }
}
