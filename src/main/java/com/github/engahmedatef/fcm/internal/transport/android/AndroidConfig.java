package com.github.engahmedatef.fcm.internal.transport.android;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AndroidConfig {
    private String collapseKey;
    private AndroidMessagePriority priority;
    private String ttl;
    private String restrictedPackageName;
    private AndroidNotification notification;
    private Map<String, String> data;

    public static AndroidConfig copyOf(AndroidConfig source) {
        return AndroidConfig.builder()
                .collapseKey(source.getCollapseKey())
                .priority(source.getPriority())
                .ttl(source.getTtl())
                .restrictedPackageName(source.getRestrictedPackageName())
                .notification(source.getNotification() == null ? null : AndroidNotification.copyOf(source.getNotification()))
                .data(source.getData() == null ? new HashMap<>() : new HashMap<>(source.getData()))
                .build();
    }
}
