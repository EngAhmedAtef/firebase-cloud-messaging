package com.github.engahmedatef.fcm.internal.transport.apns;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Alert {
    private String title;
    private String body;

    public static Alert copyOf(Alert source) {
        return Alert.builder()
                .title(source.getTitle())
                .body(source.getBody())
                .build();
    }
}
