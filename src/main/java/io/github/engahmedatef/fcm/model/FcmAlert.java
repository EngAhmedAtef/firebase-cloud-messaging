package io.github.engahmedatef.fcm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * APNs alert payload embedded inside {@link io.github.engahmedatef.fcm.model.apns.FcmAps}.
 * Maps to the {@code alert} key in the APNs {@code aps} dictionary.
 *
 * <p>Use this for iOS-specific alert text that differs from the base {@link FcmNotification}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmAlert {
    /** The alert title displayed in the iOS notification banner. */
    private String title;
    /** The alert body text displayed below the title. */
    private String body;

    public static FcmAlert copyOf(FcmAlert source) {
        return FcmAlert.builder()
                .title(source.getTitle())
                .body(source.getBody())
                .build();
    }
}
