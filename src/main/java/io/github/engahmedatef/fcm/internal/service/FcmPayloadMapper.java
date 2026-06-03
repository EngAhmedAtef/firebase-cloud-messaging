package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.internal.transport.FcmWireNotification;
import io.github.engahmedatef.fcm.internal.transport.MessagePayload;
import io.github.engahmedatef.fcm.internal.transport.android.AndroidConfig;
import io.github.engahmedatef.fcm.internal.transport.android.AndroidMessagePriority;
import io.github.engahmedatef.fcm.internal.transport.android.AndroidNotification;
import io.github.engahmedatef.fcm.internal.transport.apns.Alert;
import io.github.engahmedatef.fcm.internal.transport.apns.ApnsConfig;
import io.github.engahmedatef.fcm.internal.transport.apns.ApnsPayload;
import io.github.engahmedatef.fcm.internal.transport.apns.Aps;
import io.github.engahmedatef.fcm.model.FcmMessage;
import io.github.engahmedatef.fcm.model.FcmNotification;
import io.github.engahmedatef.fcm.model.android.FcmAndroidConfig;
import io.github.engahmedatef.fcm.model.android.FcmAndroidMessagePriority;
import io.github.engahmedatef.fcm.model.apns.FcmApnsConfig;
import io.github.engahmedatef.fcm.model.apns.FcmAps;

public final class FcmPayloadMapper {

    private FcmPayloadMapper() {}

    public static AndroidConfig buildAndroidConfig(FcmAndroidConfig fcmAndroidConfig) {
        AndroidConfig.AndroidConfigBuilder configBuilder = AndroidConfig.builder()
                .collapseKey(fcmAndroidConfig.getCollapseKey())
                .priority(mapPriority(fcmAndroidConfig.getPriority()))
                .ttl(fcmAndroidConfig.getTtl() != null ? fcmAndroidConfig.getTtl().toString() : null)
                .restrictedPackageName(fcmAndroidConfig.getRestrictedPackageName())
                .data(fcmAndroidConfig.getData());

        if (fcmAndroidConfig.getNotification() != null)
            configBuilder.notification(AndroidNotification.builder()
                    .title(fcmAndroidConfig.getNotification().getTitle())
                    .body(fcmAndroidConfig.getNotification().getBody())
                    .icon(fcmAndroidConfig.getNotification().getIcon())
                    .color(fcmAndroidConfig.getNotification().getColor())
                    .sound(fcmAndroidConfig.getNotification().getSound())
                    .channelId(fcmAndroidConfig.getNotification().getChannelId())
                    .image(fcmAndroidConfig.getNotification().getImage())
                    .clickAction(fcmAndroidConfig.getNotification().getClickAction())
                    .build());

        return configBuilder.build();
    }

    public static FcmWireNotification buildWireNotification(FcmNotification fcmNotification) {
        return FcmWireNotification.builder()
                .title(fcmNotification.getTitle())
                .body(fcmNotification.getBody())
                .image(fcmNotification.getImage())
                .build();
    }

    public static ApnsConfig buildApnsConfig(FcmApnsConfig fcmApnsConfig) {
        ApnsConfig.ApnsConfigBuilder configBuilder = ApnsConfig.builder()
                .headers(fcmApnsConfig.getHeaders());

        if (fcmApnsConfig.getAps() != null) {
            FcmAps fcmAps = fcmApnsConfig.getAps();
            Aps.ApsBuilder apsBuilder = Aps.builder()
                    .badge(fcmAps.getBadge())
                    .sound(fcmAps.getSound())
                    .mutableContent(fcmAps.isMutableContent() ? 1 : null)
                    .contentAvailable(fcmAps.isContentAvailable() ? 1 : null);

            if (fcmAps.getAlert() != null)
                apsBuilder.alert(Alert.builder()
                        .title(fcmAps.getAlert().getTitle())
                        .body(fcmAps.getAlert().getBody())
                        .build());

            Aps aps = apsBuilder.build();
            ApnsPayload apnsPayload = ApnsPayload.builder().aps(aps).build();
            configBuilder.payload(apnsPayload);
        }

        return configBuilder.build();
    }

    public static MessagePayload toDeviceMessagePayload(FcmMessage message, String token) {
        MessagePayload payload = createBasePayload(message);
        payload.getMessage().setToken(token);
        return payload;
    }

    public static MessagePayload toTopicMessagePayload(FcmMessage message, String topic) {
        MessagePayload payload = createBasePayload(message);
        payload.getMessage().setTopic(topic);
        return payload;
    }

    public static MessagePayload toConditionMessagePayload(FcmMessage message, String condition) {
        MessagePayload payload = createBasePayload(message);
        payload.getMessage().setCondition(condition);
        return payload;
    }

    public static MessagePayload createBasePayload(FcmMessage fcmMessage) {
        MessagePayload payload = new MessagePayload();
        MessagePayload.Message message = new MessagePayload.Message();
        payload.setMessage(message);

        if (fcmMessage.getData() != null)
            message.setData(fcmMessage.getData());

        if (fcmMessage.getNotification() != null)
            message.setNotification(buildWireNotification(fcmMessage.getNotification()));

        if (fcmMessage.getAndroidConfig() != null)
            message.setAndroid(buildAndroidConfig(fcmMessage.getAndroidConfig()));

        if (fcmMessage.getApnsConfig() != null)
            message.setApns(buildApnsConfig(fcmMessage.getApnsConfig()));

        return payload;
    }

    private static AndroidMessagePriority mapPriority(FcmAndroidMessagePriority priority) {
        if (priority == null)
            return null;

        return switch (priority) {
            case HIGH -> AndroidMessagePriority.HIGH;
            case NORMAL -> AndroidMessagePriority.NORMAL;
        };
    }
}
