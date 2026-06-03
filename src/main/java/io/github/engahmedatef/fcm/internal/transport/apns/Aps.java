package io.github.engahmedatef.fcm.internal.transport.apns;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Aps {
    private Alert alert;
    private Integer badge;
    private String sound;
    @JsonProperty("content-available")
    private Integer contentAvailable;
    @JsonProperty("mutable-content")
    private Integer mutableContent;

    public static Aps copyOf(Aps source) {
        return Aps.builder()
                .alert(source.getAlert() == null ? null : Alert.copyOf(source.getAlert()))
                .badge(source.getBadge())
                .sound(source.getSound())
                .contentAvailable(source.getContentAvailable())
                .mutableContent(source.getMutableContent())
                .build();
    }
}
