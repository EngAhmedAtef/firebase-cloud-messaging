package com.github.engahmedatef.fcm.spi;

import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import reactor.util.retry.Retry;

/**
 * Strategy for retrying failed FCM send operations.
 * Override the default bean to apply custom back-off, jitter, or circuit-breaker logic.
 *
 * <p>The {@link Retry} spec returned by {@link #create()} is applied to every individual
 * send attempt. The default implementation retries on {@link com.github.engahmedatef.fcm.exception.FcmRetryableException}
 * subtypes using exponential back-off bounded by {@code send.max-retries},
 * {@code send.base-backoff-millis}, and {@code send.max-backoff-millis}.
 */
public interface FcmRetryPolicy {

    /**
     * Creates a new {@link Retry} specification.
     *
     * @return a non-null {@link Retry} instance
     */
    Retry create(FcmExecutionContext executionContext);
}
