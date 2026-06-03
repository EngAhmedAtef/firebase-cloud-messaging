package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import io.github.engahmedatef.fcm.model.FcmMessage;
import io.github.engahmedatef.fcm.spi.FcmRequestValidator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class ValidatorChain {
    private final List<FcmRequestValidator> validators;
    private final boolean enabled;

    public ValidatorChain(List<FcmRequestValidator> validators, boolean enabled) {
        this.validators = validators;
        this.enabled = enabled;
    }

    public Mono<Void> apply(FcmTarget target, FcmMessage message) {
        if (!enabled || validators.isEmpty())
            return Mono.empty();

        return Flux.fromIterable(validators)
                .concatMap(validator -> validator.validate(target, message))
                .then();
    }
}
