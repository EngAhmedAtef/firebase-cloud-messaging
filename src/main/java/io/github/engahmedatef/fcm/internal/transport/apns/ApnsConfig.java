package io.github.engahmedatef.fcm.internal.transport.apns;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
/** Internal transport model for the FCM HTTP v1 {@code apns} object. */
public class ApnsConfig {
    private Map<String, String> headers;
    private ApnsPayload payload;

    public static ApnsConfig copyOf(ApnsConfig source) {
        return ApnsConfig.builder()
                .headers(source.getHeaders() == null ? new HashMap<>() : new HashMap<>(source.getHeaders()))
                .payload(source.getPayload() == null ? null : ApnsPayload.copyOf(source.getPayload()))
                .build();
    }
}
