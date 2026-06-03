package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.exception.FcmRetryableException;
import io.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import io.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import io.github.engahmedatef.fcm.internal.observability.ObservationManager;
import io.github.engahmedatef.fcm.spi.FcmLogEvent;
import io.github.engahmedatef.fcm.spi.FcmLogger;
import io.github.engahmedatef.fcm.spi.FcmRetryPolicy;
import lombok.AllArgsConstructor;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/** Exponential back-off retry policy with jitter; retries on {@link io.github.engahmedatef.fcm.exception.FcmRetryableException} and network errors. */
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
