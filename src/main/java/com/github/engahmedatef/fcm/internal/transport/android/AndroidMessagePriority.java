package com.github.engahmedatef.fcm.internal.transport.android;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

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
