package io.github.engahmedatef.fcm.model.android;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Android-specific notification display properties that override values in the base
 * {@link io.github.engahmedatef.fcm.model.FcmNotification}.
 *
 * <p>Maps to the {@code android.notification} object in the FCM HTTP v1 API.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmAndroidNotification {
    /** Notification title; overrides {@code FcmNotification.title} on Android. */
    private String title;
    /** Notification body text; overrides {@code FcmNotification.body} on Android. */
    private String body;
    /** Drawable resource name for the notification icon (e.g. {@code "ic_notification"}). */
    private String icon;
    /** Accent color for the icon, in {@code #RRGGBB} hex format. */
    private String color;
    /** Sound file name (without extension) to play; use {@code "default"} for the device default. */
    private String sound;
    /** Android notification channel ID. Required on Android 8.0 (API 26) and above. */
    private String channelId;
    /** URL of an image to display in the expanded notification. */
    private String image;
    /** Activity class name to start when the notification is tapped. */
    private String clickAction;

    public static FcmAndroidNotification copyOf(FcmAndroidNotification source) {
        return FcmAndroidNotification.builder()
                .title(source.getTitle())
                .body(source.getBody())
                .icon(source.getIcon())
                .color(source.getColor())
                .sound(source.getSound())
                .channelId(source.getChannelId())
                .image(source.getImage())
                .clickAction(source.getClickAction())
                .build();
    }
}
