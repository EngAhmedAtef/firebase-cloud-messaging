package com.github.engahmedatef.fcm.support;

import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.spi.FcmAccessTokenProvider;
import reactor.core.publisher.Mono;

public class FakeAccessTokenProvider implements FcmAccessTokenProvider {

    public static final String TEST_TOKEN = "test-bearer-token";

    @Override
    public Mono<String> getAccessToken(FcmExecutionContext context) {
        return Mono.just(TEST_TOKEN);
    }
}
