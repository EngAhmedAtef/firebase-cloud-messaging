package io.github.engahmedatef.fcm.spi;

import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import io.github.engahmedatef.fcm.model.FcmMessage;
import reactor.core.publisher.Mono;

/**
 * Validates a send request before the enricher chain runs. Multiple validators are applied in
 * {@link org.springframework.core.annotation.Order} sequence alongside
 * {@code DefaultFcmRequestValidator} (which enforces built-in rules such as topic name format,
 * condition syntax, and APNs badge/sound constraints).
 *
 * <p>Return {@link reactor.core.publisher.Mono#empty()} to indicate the message is valid.
 * Signal a {@link io.github.engahmedatef.fcm.exception.FcmInvalidRequestException} (or any
 * {@link io.github.engahmedatef.fcm.exception.FcmException} subtype) to reject it.
 *
 * <p>Validation can be globally disabled via {@code io.github.engahmedatef.fcm.validation.enabled=false}.
 */
public interface FcmRequestValidator {

    /**
     * Validates the request.
     *
     * @param target  the FCM target (device, topic, or condition) the message will be sent to
     * @param message the domain message to validate
     * @return a {@link reactor.core.publisher.Mono} that completes empty if valid, or errors if invalid
     */
    Mono<Void> validate(FcmTarget target, FcmMessage message);
}
