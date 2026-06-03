package com.github.engahmedatef.fcm.internal.transport.apns;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApnsPayload {
    private Aps aps;

    public static ApnsPayload copyOf(ApnsPayload source) {
        return ApnsPayload.builder()
                .aps(source.getAps() == null ? null : Aps.copyOf(source.getAps()))
                .build();
    }
}
