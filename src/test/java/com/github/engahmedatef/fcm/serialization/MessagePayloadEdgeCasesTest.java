package com.github.engahmedatef.fcm.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.engahmedatef.fcm.internal.service.FcmPayloadMapper;
import com.github.engahmedatef.fcm.internal.transport.MessagePayload;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.model.FcmNotification;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class MessagePayloadEdgeCasesTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void explicitNullDataProducesNoDataField() throws Exception {
        // @Builder.Default initializes data to HashMap; pass null explicitly to override
        FcmMessage message = FcmMessage.builder()
                .notification(FcmTestFixtures.aNotification())
                .data(null)
                .build();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.data").isAbsent();
    }

    @Test
    void defaultEmptyDataMapSerializesAsEmptyObject() throws Exception {
        // FcmMessage.data defaults to new HashMap<>() via @Builder.Default
        FcmMessage message = FcmMessage.builder()
                .notification(FcmTestFixtures.aNotification())
                .build();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.data").isEqualTo("{}");
    }

    @Test
    void nullNotificationWithDataOnlyProducesNoNotificationField() throws Exception {
        FcmMessage message = FcmMessage.builder()
                .data(Map.of("key", "value"))
                .build();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.notification").isAbsent();
        assertThatJson(json).node("message.data.key").isEqualTo("value");
    }

    @Test
    void unicodeCharactersInTitleAndBodyRoundtripCleanly() throws Exception {
        FcmMessage message = FcmMessage.builder()
                .notification(FcmNotification.builder()
                        .title("日本語タイトル 🎉")
                        .body("مرحبا بالعالم — Héllo Wörld")
                        .build())
                .build();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.notification.title").isEqualTo("日本語タイトル 🎉");
        assertThatJson(json).node("message.notification.body").isEqualTo("مرحبا بالعالم — Héllo Wörld");
    }

    @Test
    void androidAndApnsAbsentFromDeviceOnlyMessage() throws Exception {
        FcmMessage message = FcmMessage.builder()
                .notification(FcmTestFixtures.aNotification())
                .build();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.android").isAbsent();
        assertThatJson(json).node("message.apns").isAbsent();
    }

    @Test
    void conditionTargetSerializesWithConditionField() throws Exception {
        FcmMessage message = FcmMessage.builder()
                .notification(FcmTestFixtures.aNotification())
                .build();

        MessagePayload payload = FcmPayloadMapper.toConditionMessagePayload(message, FcmTestFixtures.CONDITION_EXPR);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.condition").isEqualTo(FcmTestFixtures.CONDITION_EXPR);
        assertThatJson(json).node("message.token").isAbsent();
        assertThatJson(json).node("message.topic").isAbsent();
    }

    @Test
    void topicTargetSerializesWithTopicField() throws Exception {
        FcmMessage message = FcmMessage.builder()
                .notification(FcmTestFixtures.aNotification())
                .build();

        MessagePayload payload = FcmPayloadMapper.toTopicMessagePayload(message, FcmTestFixtures.TOPIC_NAME);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.topic").isEqualTo(FcmTestFixtures.TOPIC_NAME);
        assertThatJson(json).node("message.token").isAbsent();
        assertThatJson(json).node("message.condition").isAbsent();
    }

    @Test
    void largeDataValueIsSerializedWithoutTruncation() throws Exception {
        String largeValue = "x".repeat(1024);
        FcmMessage message = FcmMessage.builder()
                .data(Map.of("big-key", largeValue))
                .build();

        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.data.big-key").isEqualTo(largeValue);
    }
}
