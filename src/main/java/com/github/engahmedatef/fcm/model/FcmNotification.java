package com.github.engahmedatef.fcm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Platform-independent notification payload. The SDK maps these fields directly to the FCM
 * HTTP v1 {@code notification} object; FCM then adapts them for each platform.
 *
 * <p>For platform-specific presentation control (channel ID, badge, sound, etc.) use
 * {@link com.github.engahmedatef.fcm.model.android.FcmAndroidConfig} or
 * {@link com.github.engahmedatef.fcm.model.apns.FcmApnsConfig} on the parent {@link FcmMessage}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmNotification {
    private String title;
    private String body;
    private String image;

    public static FcmNotification copyOf(FcmNotification source) {
        return FcmNotification.builder()
                .title(source.getTitle())
                .body(source.getBody())
                .image(source.getImage())
                .build();
    }
}
