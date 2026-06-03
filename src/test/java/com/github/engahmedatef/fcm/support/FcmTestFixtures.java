package com.github.engahmedatef.fcm.support;

import com.github.engahmedatef.fcm.internal.service.FcmPayloadMapper;
import com.github.engahmedatef.fcm.internal.transport.MessagePayload;
import com.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import com.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import com.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import com.github.engahmedatef.fcm.model.*;
import com.github.engahmedatef.fcm.model.android.FcmAndroidConfig;
import com.github.engahmedatef.fcm.model.android.FcmAndroidMessagePriority;
import com.github.engahmedatef.fcm.model.android.FcmAndroidNotification;
import com.github.engahmedatef.fcm.model.apns.FcmApnsConfig;
import com.github.engahmedatef.fcm.model.apns.FcmAps;

import java.time.Duration;
import java.util.Map;

public final class FcmTestFixtures {

    public static final String DEVICE_TOKEN = "device-token-abc123";
    public static final String TOPIC_NAME = "news";
    public static final String CONDITION_EXPR = "'TopicA' in topics && 'TopicB' in topics";

    private FcmTestFixtures() {}

    public static FcmDevice aDevice() {
        return FcmDevice.builder()
                .type(FcmDeviceType.ANDROID)
                .token(DEVICE_TOKEN)
                .build();
    }

    public static FcmNotification aNotification() {
        return FcmNotification.builder()
                .title("Test Title")
                .body("Test Body")
                .build();
    }

    public static FcmMessage aDeviceMessage() {
        return FcmMessage.builder()
                .notification(aNotification())
                .build();
    }

    public static FcmMessage aDeviceMessageWithData() {
        return FcmMessage.builder()
                .notification(aNotification())
                .data(Map.of("key1", "value1", "key2", "value2"))
                .build();
    }

    public static FcmMessage aTopicMessage() {
        return FcmMessage.builder()
                .notification(aNotification())
                .build();
    }

    public static FcmMessage aConditionMessage() {
        return FcmMessage.builder()
                .notification(aNotification())
                .build();
    }

    public static FcmAndroidConfig anAndroidConfig() {
        return FcmAndroidConfig.builder()
                .priority(FcmAndroidMessagePriority.HIGH)
                .ttl(Duration.ofHours(1))
                .collapseKey("collapse-key")
                .restrictedPackageName("com.example.app")
                .notification(FcmAndroidNotification.builder()
                        .title("Android Title")
                        .body("Android Body")
                        .icon("ic_notification")
                        .color("#FF0000")
                        .sound("default")
                        .channelId("default-channel")
                        .image("https://example.com/image.png")
                        .clickAction("OPEN_ACTIVITY")
                        .build())
                .build();
    }

    public static FcmApnsConfig anApnsConfig(boolean contentAvailable, boolean mutableContent) {
        return FcmApnsConfig.builder()
                .aps(FcmAps.builder()
                        .alert(FcmAlert.builder()
                                .title("APNs Title")
                                .body("APNs Body")
                                .build())
                        .badge(5)
                        .sound("default")
                        .contentAvailable(contentAvailable)
                        .mutableContent(mutableContent)
                        .build())
                .build();
    }

    public static FcmMessage aMessageWithAndroid() {
        return FcmMessage.builder()
                .notification(aNotification())
                .androidConfig(anAndroidConfig())
                .build();
    }

    public static FcmMessage aMessageWithApns(boolean contentAvailable, boolean mutableContent) {
        return FcmMessage.builder()
                .notification(aNotification())
                .apnsConfig(anApnsConfig(contentAvailable, mutableContent))
                .build();
    }

    public static FcmMessage aMessageWithApnsNoNotification(boolean contentAvailable, boolean mutableContent) {
        return FcmMessage.builder()
                .apnsConfig(anApnsConfig(contentAvailable, mutableContent))
                .build();
    }

    public static MessagePayload aDeviceMessagePayload() {
        return FcmPayloadMapper.toDeviceMessagePayload(aDeviceMessage(), DEVICE_TOKEN);
    }

    public static DeviceTarget aDeviceTarget() {
        return new DeviceTarget(aDevice());
    }

    public static TopicTarget aTopicTarget() {
        return new TopicTarget(TOPIC_NAME);
    }

    public static ConditionTarget aConditionTarget() {
        return new ConditionTarget(CONDITION_EXPR);
    }
}
