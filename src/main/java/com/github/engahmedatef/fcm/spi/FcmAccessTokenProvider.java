package com.github.engahmedatef.fcm.spi;

import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import reactor.core.publisher.Mono;

/**
 * Provides an abstraction for retrieving an OAuth 2.0 access token asynchronously.
 * Override the default bean to supply custom authentication (e.g., token caching, non-Google credentials).
 */
public interface FcmAccessTokenProvider {

    /**
     * Retrieves a valid access token, refreshing it if necessary.
     *
     * @param context the current execution context for metrics and observability
     * @return a {@link Mono} emitting the access token string
     */
    Mono<String> getAccessToken(FcmExecutionContext context);
}
