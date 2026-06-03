package com.github.engahmedatef.fcm.unit;

import com.github.engahmedatef.fcm.exception.FcmInvalidRequestException;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmRequestValidator;
import com.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import com.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import com.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.model.apns.FcmApnsConfig;
import com.github.engahmedatef.fcm.model.apns.FcmAps;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class DefaultFcmRequestValidatorTest {

    private DefaultFcmRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultFcmRequestValidator();
    }

    // --- Topic name validation ---

    @Test
    void validTopicNamePassesValidation() {
        StepVerifier.create(validator.validate(new TopicTarget("news-updates_2024"), FcmTestFixtures.aTopicMessage()))
                .verifyComplete();
    }

    @Test
    void topicNameWithSpecialCharactersFailsValidation() {
        StepVerifier.create(validator.validate(new TopicTarget("invalid topic!"), FcmTestFixtures.aTopicMessage()))
                .expectError(FcmInvalidRequestException.class)
                .verify();
    }

    @Test
    void emptyTopicNameFailsValidation() {
        StepVerifier.create(validator.validate(new TopicTarget(""), FcmTestFixtures.aTopicMessage()))
                .expectError(FcmInvalidRequestException.class)
                .verify();
    }

    @Test
    void topicNameWithAllowedUrlEncodingPasses() {
        StepVerifier.create(validator.validate(new TopicTarget("topic%20name"), FcmTestFixtures.aTopicMessage()))
                .verifyComplete();
    }

    // --- Condition validation ---

    @Test
    void validConditionWithTwoTopicsPasses() {
        StepVerifier.create(validator.validate(
                new ConditionTarget("'TopicA' in topics && 'TopicB' in topics"),
                FcmTestFixtures.aConditionMessage()))
                .verifyComplete();
    }

    @Test
    void conditionWithUnbalancedParenthesesFailsValidation() {
        StepVerifier.create(validator.validate(
                new ConditionTarget("('TopicA' in topics && 'TopicB' in topics"),
                FcmTestFixtures.aConditionMessage()))
                .expectError(FcmInvalidRequestException.class)
                .verify();
    }

    @Test
    void blankConditionFailsValidation() {
        StepVerifier.create(validator.validate(
                new ConditionTarget("   "),
                FcmTestFixtures.aConditionMessage()))
                .expectError(FcmInvalidRequestException.class)
                .verify();
    }

    // --- APNs validation ---

    @Test
    void apnsWithZeroBadgePassesValidation() {
        FcmMessage message = messageWithApns(0, "default");
        StepVerifier.create(validator.validate(FcmTestFixtures.aDeviceTarget(), message))
                .verifyComplete();
    }

    @Test
    void apnsWithNegativeBadgeFailsValidation() {
        FcmMessage message = messageWithApns(-1, "default");
        StepVerifier.create(validator.validate(FcmTestFixtures.aDeviceTarget(), message))
                .expectError(FcmInvalidRequestException.class)
                .verify();
    }

    @Test
    void apnsWithSoundExactly255CharsPassesValidation() {
        String maxSound = "a".repeat(255);
        FcmMessage message = messageWithApns(0, maxSound);
        StepVerifier.create(validator.validate(FcmTestFixtures.aDeviceTarget(), message))
                .verifyComplete();
    }

    // --- Device target skips topic/condition validation ---

    @Test
    void deviceTargetSkipsTopicAndConditionValidation() {
        StepVerifier.create(validator.validate(
                new DeviceTarget(FcmTestFixtures.aDevice()),
                FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();
    }

    // --- Message without APNs ---

    @Test
    void messageWithoutApnsSkipsApnsValidation() {
        StepVerifier.create(validator.validate(
                FcmTestFixtures.aDeviceTarget(),
                FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();
    }

    // --- Helpers ---

    private FcmMessage messageWithApns(int badge, String sound) {
        return FcmMessage.builder()
                .apnsConfig(FcmApnsConfig.builder()
                        .aps(FcmAps.builder()
                                .badge(badge)
                                .sound(sound)
                                .build())
                        .build())
                .build();
    }
}
