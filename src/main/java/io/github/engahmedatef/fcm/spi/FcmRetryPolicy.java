package io.github.engahmedatef.fcm.spi;

import io.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import reactor.util.retry.Retry;

/**
 * Strategy for retrying failed FCM send operations.
 * Override the default bean to apply custom back-off, jitter, or circuit-breaker logic.
 *
 * <p>The {@link Retry} spec returned by {@link #create(FcmExecutionContext)} is applied to every individual
 * send attempt. The default implementation retries on {@link io.github.engahmedatef.fcm.exception.FcmRetryableException}
 * subtypes using exponential back-off bounded by {@code send.max-retries},
 * {@code send.base-backoff-millis}, and {@code send.max-backoff-millis}.
 */
public interface FcmRetryPolicy {

    /**
     * Creates a new {@link Retry} specification for the given execution context.
     *
     * @param executionContext the context for the current send attempt
     * @return a non-null {@link Retry} instance
     */
    Retry create(FcmExecutionContext executionContext);
}
