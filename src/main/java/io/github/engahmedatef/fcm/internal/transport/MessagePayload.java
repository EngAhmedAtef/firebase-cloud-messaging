package io.github.engahmedatef.fcm.internal.transport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.engahmedatef.fcm.internal.transport.android.AndroidConfig;
import io.github.engahmedatef.fcm.internal.transport.apns.ApnsConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagePayload {
    private Message message;

    public static MessagePayload copyOf(MessagePayload source) {
        return MessagePayload.builder()
                .message(source.getMessage() == null ? null : Message.copyOf(source.getMessage()))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String token;
        private String topic;
        private String condition;
        private FcmWireNotification notification;
        private Map<String, String> data;
        private AndroidConfig android;
        private ApnsConfig apns;

        public static Message copyOf(Message source) {
            return Message.builder()
                    .token(source.getToken())
                    .topic(source.getTopic())
                    .condition(source.getCondition())
                    .notification(source.getNotification() == null ? null : FcmWireNotification.copyOf(source.getNotification()))
                    .data(source.getData() == null ? new HashMap<>() : new HashMap<>(source.getData()))
                    .android(source.getAndroid() == null ? null : AndroidConfig.copyOf(source.getAndroid()))
                    .apns(source.getApns() == null ? null : ApnsConfig.copyOf(source.getApns()))
                    .build();
        }
    }
}
