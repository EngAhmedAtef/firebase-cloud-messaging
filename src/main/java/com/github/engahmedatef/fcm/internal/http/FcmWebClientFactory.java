package com.github.engahmedatef.fcm.internal.http;

import org.springframework.web.reactive.function.client.WebClient;

public interface FcmWebClientFactory {
    WebClient create();
}
