package io.github.engahmedatef.fcm.model.apns;

import io.github.engahmedatef.fcm.model.FcmAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * APNs {@code aps} dictionary payload. Controls the iOS system notification presentation
 * and background-fetch behaviour.
 *
 * <p>Maps to the {@code apns.payload.aps} object in the FCM HTTP v1 API.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmAps {
    /** iOS alert content; overrides the base {@link io.github.engahmedatef.fcm.model.FcmNotification} on iOS. */
    private FcmAlert alert;
    /** App badge count to display. Set to {@code 0} to clear the badge. */
    private Integer badge;
    /**
     * Sound file name (without extension) bundled with the app, or {@code "default"}
     * for the system default sound. Set to {@code ""} (empty string) for silent push.
     */
    private String sound;
    /**
     * When {@code true}, the system wakes the app in the background to process the notification.
     * Set {@code apns-priority} header to {@code 5} when using background push.
     */
    private boolean contentAvailable;
    /**
     * When {@code true}, the Notification Service Extension can modify the notification content
     * before it is displayed (e.g. to decrypt or download media).
     */
    private boolean mutableContent;

    public static FcmAps copyOf(FcmAps source) {
        return FcmAps.builder()
                .alert(source.getAlert() == null ? null : FcmAlert.copyOf(source.getAlert()))
                .badge(source.getBadge())
                .sound(source.getSound())
                .contentAvailable(source.isContentAvailable())
                .mutableContent(source.isMutableContent())
                .build();
    }
}
