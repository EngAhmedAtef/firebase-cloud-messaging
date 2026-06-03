package io.github.engahmedatef.fcm.internal.transport.android;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** FCM message delivery priority for Android: {@code HIGH} or {@code NORMAL}. */
@Getter
public enum AndroidMessagePriority {
    HIGH("high"),
    NORMAL("normal");

    private final String value;

    AndroidMessagePriority(String value) {
        this.value = value;
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
