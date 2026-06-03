package io.github.engahmedatef.fcm.spi;

import io.github.engahmedatef.fcm.internal.transport.MessagePayload;
import reactor.core.publisher.Mono;

/**
 * Intercepts and optionally mutates the wire {@link MessagePayload} immediately before it is
 * sent to the FCM HTTP v1 API. Multiple interceptors are applied in {@link org.springframework.core.annotation.Order} sequence.
 *
 * <p>Use this SPI for cross-cutting concerns that operate at the transport level (e.g. audit
 * logging, payload encryption, header injection). For domain-level enrichment, prefer
 * {@link FcmMessageEnricher} instead.
 *
 * <p>Implementations must be non-blocking. Register beans via {@code @Component} or {@code @Bean};
 * they are picked up automatically by the SDK via {@code ObjectProvider}.
 */
public interface FcmPayloadInterceptor {

    /**
     * Intercepts the outgoing payload.
     *
     * @param context contextual metadata (target, original message, project ID, attributes)
     * @param payload the wire payload about to be sent; may be mutated or replaced
     * @return a {@link Mono} emitting the (possibly modified) payload to forward; must not be empty
     */
    Mono<MessagePayload> intercept(FcmInterceptorContext context, MessagePayload payload);
}
