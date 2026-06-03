package io.github.engahmedatef.fcm.model.apns;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * APNs-specific message configuration passed through FCM to Apple Push Notification service.
 *
 * <p>Maps to the {@code apns} object in the FCM HTTP v1 API request body.
 * Use this to set APNs headers (e.g. {@code apns-priority}, {@code apns-expiration})
 * and the {@code aps} payload for iOS-specific notification presentation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmApnsConfig {
    /**
     * APNs HTTP/2 headers forwarded to APNs verbatim (e.g.
     * {@code apns-priority: 10}, {@code apns-topic: com.example.app}).
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    /** APNs payload dictionary containing alert, badge, and sound overrides. */
    private FcmAps aps;

    public static FcmApnsConfig copyOf(FcmApnsConfig source) {
        return FcmApnsConfig.builder()
                .headers(source.getHeaders() == null ? new HashMap<>() : new HashMap<>(source.getHeaders()))
                .aps(source.getAps() == null ? null : FcmAps.copyOf(source.getAps()))
                .build();
    }
}
