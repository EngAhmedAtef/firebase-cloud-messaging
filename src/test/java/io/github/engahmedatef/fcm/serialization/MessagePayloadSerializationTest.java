package io.github.engahmedatef.fcm.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.engahmedatef.fcm.internal.service.FcmPayloadMapper;
import io.github.engahmedatef.fcm.internal.transport.MessagePayload;
import io.github.engahmedatef.fcm.model.FcmMessage;
import io.github.engahmedatef.fcm.model.FcmNotification;
import io.github.engahmedatef.fcm.support.FcmTestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

class MessagePayloadSerializationTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private String loadFixture(String name) throws Exception {
        var url = getClass().getClassLoader().getResource("fixtures/payloads/" + name);
        return Files.readString(Paths.get(url.toURI()));
    }

    @Test
    void deviceNotificationMatchesGoldenFile() throws Exception {
        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(
                FcmTestFixtures.aDeviceMessage(), FcmTestFixtures.DEVICE_TOKEN);

        String json = objectMapper.writeValueAsString(payload);
        assertThatJson(json).when(IGNORING_ARRAY_ORDER).isEqualTo(loadFixture("device-notification.json"));
    }

    @Test
    void deviceNotificationWithDataMatchesGoldenFile() throws Exception {
        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(
                FcmTestFixtures.aDeviceMessageWithData(), FcmTestFixtures.DEVICE_TOKEN);

        String json = objectMapper.writeValueAsString(payload);
        assertThatJson(json).when(IGNORING_ARRAY_ORDER).isEqualTo(loadFixture("device-notification-data.json"));
    }

    @Test
    void topicNotificationMatchesGoldenFile() throws Exception {
        MessagePayload payload = FcmPayloadMapper.toTopicMessagePayload(
                FcmTestFixtures.aTopicMessage(), FcmTestFixtures.TOPIC_NAME);

        String json = objectMapper.writeValueAsString(payload);
        assertThatJson(json).when(IGNORING_ARRAY_ORDER).isEqualTo(loadFixture("topic-notification.json"));
    }

    @Test
    void conditionNotificationMatchesGoldenFile() throws Exception {
        MessagePayload payload = FcmPayloadMapper.toConditionMessagePayload(
                FcmTestFixtures.aConditionMessage(), FcmTestFixtures.CONDITION_EXPR);

        String json = objectMapper.writeValueAsString(payload);
        assertThatJson(json).when(IGNORING_ARRAY_ORDER).isEqualTo(loadFixture("condition-notification.json"));
    }

    @Test
    void androidFullConfigMatchesGoldenFile() throws Exception {
        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(
                FcmTestFixtures.aMessageWithAndroid(), FcmTestFixtures.DEVICE_TOKEN);

        String json = objectMapper.writeValueAsString(payload);
        assertThatJson(json).when(IGNORING_ARRAY_ORDER).isEqualTo(loadFixture("android-full.json"));
    }

    @Test
    void apnsFullConfigMatchesGoldenFile() throws Exception {
        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(
                FcmTestFixtures.aMessageWithApns(true, true), FcmTestFixtures.DEVICE_TOKEN);

        String json = objectMapper.writeValueAsString(payload);
        assertThatJson(json).when(IGNORING_ARRAY_ORDER).isEqualTo(loadFixture("apns-full.json"));
    }

    @Test
    void apnsContentAvailableFalseFieldIsOmitted() throws Exception {
        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(
                FcmTestFixtures.aMessageWithApnsNoNotification(false, false),
                FcmTestFixtures.DEVICE_TOKEN);

        String json = objectMapper.writeValueAsString(payload);
        assertThatJson(json).node("message.apns.payload.aps.content-available").isAbsent();
        assertThatJson(json).node("message.apns.payload.aps.mutable-content").isAbsent();
    }

    @Test
    void nullsAreOmittedFromOutput() throws Exception {
        FcmMessage message = FcmMessage.builder()
                .notification(FcmTestFixtures.aNotification())
                .build();
        MessagePayload payload = FcmPayloadMapper.toDeviceMessagePayload(message, FcmTestFixtures.DEVICE_TOKEN);
        String json = objectMapper.writeValueAsString(payload);

        assertThatJson(json).node("message.android").isAbsent();
        assertThatJson(json).node("message.apns").isAbsent();
        assertThatJson(json).node("message.topic").isAbsent();
        assertThatJson(json).node("message.condition").isAbsent();
    }
}
