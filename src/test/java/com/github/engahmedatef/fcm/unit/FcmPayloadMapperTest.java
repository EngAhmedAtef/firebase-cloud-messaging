package com.github.engahmedatef.fcm.unit;

import com.github.engahmedatef.fcm.internal.service.FcmPayloadMapper;
import com.github.engahmedatef.fcm.internal.transport.MessagePayload;
import com.github.engahmedatef.fcm.internal.transport.android.AndroidConfig;
import com.github.engahmedatef.fcm.internal.transport.apns.ApnsConfig;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.model.FcmNotification;
import com.github.engahmedatef.fcm.model.android.FcmAndroidConfig;
import com.github.engahmedatef.fcm.model.android.FcmAndroidMessagePriority;
import com.github.engahmedatef.fcm.model.apns.FcmApnsConfig;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FcmPayloadMapperTest {

    @Test
    void mapsDeviceTokenToPayload() {
        FcmMessage message = FcmTestFixtures.aDeviceMessage();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);

        assertEquals(FcmTestFixtures.DEVICE_TOKEN, payload.getMessage().getToken());
        assertNull(payload.getMessage().getTopic());
        assertNull(payload.getMessage().getCondition());
    }

    @Test
    void mapsTopicToPayload() {
        FcmMessage message = FcmTestFixtures.aTopicMessage();

        MessagePayload payload = FcmPayloadMapper.toTopicMessagePayload(message, FcmTestFixtures.TOPIC_NAME);

        assertEquals(FcmTestFixtures.TOPIC_NAME, payload.getMessage().getTopic());
        assertNull(payload.getMessage().getToken());
        assertNull(payload.getMessage().getCondition());
    }

    @Test
    void mapsConditionToPayload() {
        FcmMessage message = FcmTestFixtures.aConditionMessage();

        MessagePayload payload = FcmPayloadMapper.toConditionMessagePayload(message, FcmTestFixtures.CONDITION_EXPR);

        assertEquals(FcmTestFixtures.CONDITION_EXPR, payload.getMessage().getCondition());
        assertNull(payload.getMessage().getTopic());
        assertNull(payload.getMessage().getToken());
    }

    @Test
    void androidPriorityHighMapsToLowercaseHigh() {
        FcmAndroidConfig fcmAndroidConfig = FcmAndroidConfig.builder()
                .priority(FcmAndroidMessagePriority.HIGH)
                .build();

        AndroidConfig androidConfig = FcmPayloadMapper.buildAndroidConfig(fcmAndroidConfig);

        assertEquals("high", androidConfig.getPriority().toValue());
    }

    @Test
    void androidPriorityNormalMapsToLowercaseNormal() {
        FcmAndroidConfig fcmAndroidConfig = FcmAndroidConfig.builder()
                .priority(FcmAndroidMessagePriority.NORMAL)
                .build();

        AndroidConfig androidConfig = FcmPayloadMapper.buildAndroidConfig(fcmAndroidConfig);

        assertEquals("normal", androidConfig.getPriority().toValue());
    }

    @Test
    void androidTtlMappedAsDurationString() {
        FcmAndroidConfig fcmAndroidConfig = FcmAndroidConfig.builder()
                .ttl(Duration.ofHours(1))
                .build();

        AndroidConfig androidConfig = FcmPayloadMapper.buildAndroidConfig(fcmAndroidConfig);

        assertEquals("PT1H", androidConfig.getTtl());
    }

    @Test
    void androidNullTtlProducesNullField() {
        FcmAndroidConfig fcmAndroidConfig = FcmAndroidConfig.builder().build();

        AndroidConfig androidConfig = FcmPayloadMapper.buildAndroidConfig(fcmAndroidConfig);

        assertNull(androidConfig.getTtl());
    }

    @Test
    void contentAvailableTrueProducesInteger1() {
        FcmApnsConfig fcmApnsConfig = FcmTestFixtures.anApnsConfig(true, false);

        ApnsConfig apnsConfig = FcmPayloadMapper.buildApnsConfig(fcmApnsConfig);

        assertEquals(1, apnsConfig.getPayload().getAps().getContentAvailable());
    }

    @Test
    void contentAvailableFalseProducesNullField() {
        FcmApnsConfig fcmApnsConfig = FcmTestFixtures.anApnsConfig(false, false);

        ApnsConfig apnsConfig = FcmPayloadMapper.buildApnsConfig(fcmApnsConfig);

        assertNull(apnsConfig.getPayload().getAps().getContentAvailable());
    }

    @Test
    void mutableContentTrueProducesInteger1() {
        FcmApnsConfig fcmApnsConfig = FcmTestFixtures.anApnsConfig(false, true);

        ApnsConfig apnsConfig = FcmPayloadMapper.buildApnsConfig(fcmApnsConfig);

        assertEquals(1, apnsConfig.getPayload().getAps().getMutableContent());
    }

    @Test
    void mutableContentFalseProducesNullField() {
        FcmApnsConfig fcmApnsConfig = FcmTestFixtures.anApnsConfig(false, false);

        ApnsConfig apnsConfig = FcmPayloadMapper.buildApnsConfig(fcmApnsConfig);

        assertNull(apnsConfig.getPayload().getAps().getMutableContent());
    }

    @Test
    void nullNotificationNotIncludedInPayload() {
        FcmMessage message = FcmMessage.builder().build();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, "token");

        assertNull(payload.getMessage().getNotification());
    }

    @Test
    void explicitNullDataNotIncludedInPayload() {
        FcmNotification fcmNotification = FcmTestFixtures.aNotification();
        FcmMessage message = FcmMessage.builder()
                .notification(fcmNotification)
                .build();
        message.setData(null);

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, "token");

        assertNull(payload.getMessage().getData());
    }

    @Test
    void emptyDataFromBuilderIsPassedThrough() {
        FcmNotification fcmNotification = FcmTestFixtures.aNotification();
        FcmMessage message = FcmMessage.builder()
                .notification(fcmNotification)
                .build();

        MessagePayload payload = FcmPayloadMapper.createBasePayload(message);

        assertNotNull(payload.getMessage().getData());
        assertThat(payload.getMessage().getData()).isEmpty();
    }
}
