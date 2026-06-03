package io.github.engahmedatef.fcm.model;

import io.github.engahmedatef.fcm.model.android.FcmAndroidConfig;
import io.github.engahmedatef.fcm.model.apns.FcmApnsConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Top-level domain model representing a Firebase Cloud Messaging push notification.
 *
 * <p>Build instances using the provided Lombok builder. At least one of {@link #notification}
 * or {@link #data} must be populated for the message to be meaningful.
 * Platform-specific overrides can be applied via {@link #androidConfig} and {@link #apnsConfig}.
 *
 * <pre>{@code
 * FcmMessage message = FcmMessage.builder()
 *         .notification(FcmNotification.builder()
 *                 .title("Order shipped")
 *                 .body("Your order #12345 is on its way.")
 *                 .build())
 *         .data(Map.of("orderId", "12345", "screen", "order_tracking"))
 *         .build();
 * }</pre>
 *
 * <p>The SDK creates a defensive copy of this object before passing it through the enricher
 * chain, so mutations inside enrichers do not affect the caller's original instance.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmMessage {
    private FcmNotification notification;
    @Builder.Default
    private Map<String, String> data = new HashMap<>();
    private FcmAndroidConfig androidConfig;
    private FcmApnsConfig apnsConfig;

    public static FcmMessage copyOf(FcmMessage source) {
        if (source == null)
            return null;

        return FcmMessage.builder()
                .data(source.getData() == null ? new HashMap<>() : new HashMap<>(source.getData()))
                .notification(source.getNotification() == null ? null : FcmNotification.copyOf(source.getNotification()))
                .androidConfig(source.getAndroidConfig() == null ? null : FcmAndroidConfig.copyOf(source.getAndroidConfig()))
                .apnsConfig(source.getApnsConfig() == null ? null : FcmApnsConfig.copyOf(source.getApnsConfig()))
                .build();
    }
}
