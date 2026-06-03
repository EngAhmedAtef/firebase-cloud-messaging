package io.github.engahmedatef.fcm.internal.transport;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FcmWireNotification {
    private String title;
    private String body;
    private String image;

    public static FcmWireNotification copyOf(FcmWireNotification source) {
        return FcmWireNotification.builder()
                .title(source.getTitle())
                .body(source.getBody())
                .image(source.getImage())
                .build();
    }
}
