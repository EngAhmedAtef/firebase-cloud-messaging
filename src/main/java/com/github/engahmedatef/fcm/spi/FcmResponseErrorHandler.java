package com.github.engahmedatef.fcm.spi;

import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import com.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import com.github.engahmedatef.fcm.model.FcmSendResult;
import reactor.core.publisher.Mono;

/**
 * Translates an FCM HTTP error response into either a typed exception or a failure
 * {@link FcmSendResult}. Override the default bean to customise error handling or stale token
 * cleanup behaviour.
 */
public interface FcmResponseErrorHandler {

    /**
     * Handles an error response from the FCM HTTP v1 API.
     *
     * @param target the FCM target (device, topic, or condition) that was being sent to
     * @param error  the parsed FCM error body
     * @return a {@link Mono} that either emits a failure result or terminates with a typed
     *         {@link com.github.engahmedatef.fcm.exception.FcmException}
     */
    Mono<FcmSendResult> handle(FcmTarget target, FcmSendErrorResponse error);
}
