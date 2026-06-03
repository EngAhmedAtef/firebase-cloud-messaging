package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.internal.transport.MessagePayload;
import io.github.engahmedatef.fcm.spi.FcmInterceptorContext;
import io.github.engahmedatef.fcm.spi.FcmPayloadInterceptor;
import reactor.core.publisher.Mono;

import java.util.List;

public class InterceptorChain {
    private final List<FcmPayloadInterceptor> interceptors;
    private final boolean enabled;

    public InterceptorChain(List<FcmPayloadInterceptor> interceptors, boolean enabled) {
        this.interceptors = interceptors;
        this.enabled = enabled;
    }

    public Mono<MessagePayload> apply(FcmInterceptorContext context, MessagePayload payload) {
        if (!enabled || interceptors.isEmpty())
            return Mono.just(payload);

        Mono<MessagePayload> chain = Mono.just(MessagePayload.copyOf(payload));
        for (FcmPayloadInterceptor interceptor : interceptors)
            chain = chain.flatMap(p -> interceptor.intercept(context, p));

        return chain;
    }
}
