package com.github.engahmedatef.fcm.internal.service.sender;

import com.github.engahmedatef.fcm.internal.transport.MessagePayload;
import reactor.core.publisher.Mono;

public interface FcmMessageSender {
    Mono<FcmSenderResponse> send(MessagePayload payload);
}
