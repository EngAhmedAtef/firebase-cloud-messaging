package com.github.engahmedatef.fcm.model.android;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Android-specific message configuration. Fields here override or supplement the
 * platform-independent values in {@link com.github.engahmedatef.fcm.model.FcmMessage}.
 *
 * <p>Maps to the {@code android} object in the FCM HTTP v1 API request body.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmAndroidConfig {
    /**
     * Collapse key: if multiple messages share the same key, only the latest is delivered
     * when the device reconnects. Limited to four active collapse keys per app.
     */
    private String collapseKey;
    /** Message delivery priority. Use {@link FcmAndroidMessagePriority#HIGH} for user-facing alerts. */
    private FcmAndroidMessagePriority priority;
    /**
     * Maximum duration the message is stored in FCM storage when the device is offline.
     * Defaults to four weeks; maximum is four weeks.
     */
    private Duration ttl;
    /** Restricts delivery to the specified package name; ignored if {@code null}. */
    private String restrictedPackageName;
    /** Android-specific notification display overrides. */
    private FcmAndroidNotification notification;
    /** Android-specific data payload that supplements or overrides {@code FcmMessage.data}. */
    @Builder.Default
    private Map<String, String> data = new HashMap<>();

    public static FcmAndroidConfig copyOf(FcmAndroidConfig source) {
        return FcmAndroidConfig.builder()
                .collapseKey(source.getCollapseKey())
                .priority(source.getPriority())
                .ttl(source.getTtl())
                .restrictedPackageName(source.getRestrictedPackageName())
                .notification(source.getNotification() == null ? null : FcmAndroidNotification.copyOf(source.getNotification()))
                .data(source.getData() == null ? new HashMap<>() : new HashMap<>(source.getData()))
                .build();
    }
}
