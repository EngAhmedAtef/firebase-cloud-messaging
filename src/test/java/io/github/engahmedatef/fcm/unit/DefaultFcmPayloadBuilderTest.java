package io.github.engahmedatef.fcm.unit;

import io.github.engahmedatef.fcm.internal.service.DefaultFcmPayloadBuilder;
import io.github.engahmedatef.fcm.internal.transport.MessagePayload;
import io.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import io.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import io.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import io.github.engahmedatef.fcm.support.FcmTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFcmPayloadBuilderTest {

    private DefaultFcmPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DefaultFcmPayloadBuilder();
    }

    @Test
    void buildsDevicePayloadWithTokenPopulated() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        MessagePayload payload = builder.build(target, FcmTestFixtures.aDeviceMessage());
        assertThat(payload.getMessage().getToken()).isEqualTo(FcmTestFixtures.DEVICE_TOKEN);
        assertThat(payload.getMessage().getTopic()).isNull();
        assertThat(payload.getMessage().getCondition()).isNull();
    }

    @Test
    void buildsTopicPayloadWithTopicPopulated() {
        TopicTarget target = FcmTestFixtures.aTopicTarget();
        MessagePayload payload = builder.build(target, FcmTestFixtures.aTopicMessage());
        assertThat(payload.getMessage().getTopic()).isEqualTo(FcmTestFixtures.TOPIC_NAME);
        assertThat(payload.getMessage().getToken()).isNull();
        assertThat(payload.getMessage().getCondition()).isNull();
    }

    @Test
    void buildsConditionPayloadWithConditionPopulated() {
        ConditionTarget target = FcmTestFixtures.aConditionTarget();
        MessagePayload payload = builder.build(target, FcmTestFixtures.aConditionMessage());
        assertThat(payload.getMessage().getCondition()).isEqualTo(FcmTestFixtures.CONDITION_EXPR);
        assertThat(payload.getMessage().getToken()).isNull();
        assertThat(payload.getMessage().getTopic()).isNull();
    }

    @Test
    void throwsForUnsupportedTargetType() {
        FcmTarget unknownTarget = new FcmTarget() {};
        assertThatThrownBy(() -> builder.build(unknownTarget, FcmTestFixtures.aDeviceMessage()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported target type");
    }

    @Test
    void notificationIsIncludedInPayload() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        MessagePayload payload = builder.build(target, FcmTestFixtures.aDeviceMessage());
        assertThat(payload.getMessage().getNotification()).isNotNull();
        assertThat(payload.getMessage().getNotification().getTitle()).isEqualTo("Test Title");
        assertThat(payload.getMessage().getNotification().getBody()).isEqualTo("Test Body");
    }

    @Test
    void dataIsIncludedWhenProvided() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        MessagePayload payload = builder.build(target, FcmTestFixtures.aDeviceMessageWithData());
        assertThat(payload.getMessage().getData()).containsEntry("key1", "value1");
    }

    @Test
    void androidConfigIsIncludedWhenProvided() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        MessagePayload payload = builder.build(target, FcmTestFixtures.aMessageWithAndroid());
        assertThat(payload.getMessage().getAndroid()).isNotNull();
        assertThat(payload.getMessage().getAndroid().getPriority().toValue()).isEqualTo("high");
    }

    @Test
    void apnsConfigIsIncludedWhenProvided() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        MessagePayload payload = builder.build(target, FcmTestFixtures.aMessageWithApns(true, true));
        assertThat(payload.getMessage().getApns()).isNotNull();
        assertThat(payload.getMessage().getApns().getPayload().getAps().getContentAvailable()).isEqualTo(1);
    }
}
