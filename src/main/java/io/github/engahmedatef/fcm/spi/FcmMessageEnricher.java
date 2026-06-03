package io.github.engahmedatef.fcm.spi;

import io.github.engahmedatef.fcm.model.FcmMessage;
import reactor.core.publisher.Mono;

/**
 * Enriches or mutates a domain {@link FcmMessage} before it is converted to the wire payload.
 * Multiple enrichers are applied in {@link org.springframework.core.annotation.Order} sequence.
 *
 * <p>Use this SPI for domain-level augmentation (e.g. stamping a locale, appending default data
 * keys). For wire-level mutations after payload build, use {@link FcmPayloadInterceptor} instead.
 *
 * <p>Implementations must be non-blocking. Register beans via {@code @Component} or {@code @Bean};
 * they are picked up automatically by the SDK via {@code ObjectProvider}.
 */
public interface FcmMessageEnricher {

    /**
     * Enriches the domain message.
     *
     * @param context contextual metadata (target, project ID, attributes)
     * @param message the domain message to enrich; may be mutated in place or replaced
     * @return a {@link Mono} emitting the enriched message; must not be empty
     */
    Mono<FcmMessage> enrich(FcmEnricherContext context, FcmMessage message);
}
