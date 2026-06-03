package io.github.engahmedatef.fcm.internal.transport.android;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AndroidNotification {
    private String title;
    private String body;
    private String icon;
    private String color;
    private String sound;
    private String channelId;
    private String image;
    private String clickAction;

    public static AndroidNotification copyOf(AndroidNotification source) {
        return AndroidNotification.builder()
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
