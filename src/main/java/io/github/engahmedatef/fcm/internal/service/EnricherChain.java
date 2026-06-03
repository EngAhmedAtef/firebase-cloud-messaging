package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.model.FcmMessage;
import io.github.engahmedatef.fcm.spi.FcmEnricherContext;
import io.github.engahmedatef.fcm.spi.FcmMessageEnricher;
import reactor.core.publisher.Mono;

import java.util.List;

/** Applies registered {@link io.github.engahmedatef.fcm.spi.FcmMessageEnricher} beans in order to a message before it is sent. */
public class EnricherChain {
    private final List<FcmMessageEnricher> enrichers;
    private final boolean enabled;

    public EnricherChain(List<FcmMessageEnricher> enrichers, boolean enabled) {
        this.enrichers = enrichers;
        this.enabled = enabled;
    }

    public Mono<FcmMessage> apply(FcmEnricherContext context, FcmMessage message) {
        if (!enabled || enrichers.isEmpty())
            return Mono.just(message);

        Mono<FcmMessage> chain = Mono.just(FcmMessage.copyOf(message));
        for (FcmMessageEnricher enricher : enrichers)
            chain = chain.flatMap(m -> enricher.enrich(context, m));

        return chain;
    }
}
