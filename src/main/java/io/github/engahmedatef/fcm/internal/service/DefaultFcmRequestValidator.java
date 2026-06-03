package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.exception.FcmInvalidRequestException;
import io.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import io.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import io.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import io.github.engahmedatef.fcm.model.FcmMessage;
import io.github.engahmedatef.fcm.model.apns.FcmApnsConfig;
import io.github.engahmedatef.fcm.model.apns.FcmAps;
import io.github.engahmedatef.fcm.spi.FcmRequestValidator;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

public class DefaultFcmRequestValidator implements FcmRequestValidator {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_.~%]{1,900}$");

    @Override
    public Mono<Void> validate(FcmTarget target, FcmMessage message) {
        return Mono.defer(() -> {
            validateTarget(target);
            validateMessage(message);
            return Mono.empty();
        });
    }

    private void validateMessage(FcmMessage message) {
        if (message == null)
            throw new FcmInvalidRequestException("FCM message must not be null");

        if (message.getApnsConfig() != null)
            validateApns(message.getApnsConfig());
    }

    private void validateTarget(FcmTarget target) {
        if (target == null)
            throw new FcmInvalidRequestException("FCM target must not be null");

        if (target instanceof DeviceTarget deviceTarget)
            validateToken(deviceTarget.device().token());
        else if (target instanceof TopicTarget topicTarget) {
            validateTopicName(topicTarget.topic());
        } else if (target instanceof ConditionTarget conditionTarget) {
            validateCondition(conditionTarget.condition());
        } else
            throw new FcmInvalidRequestException("Unknown FCM target type: " + target.getClass().getName());
    }

    private void validateTopicName(String topic) {
        if (topic == null || topic.isBlank())
            throw new FcmInvalidRequestException("Topic name must not be blank");

        if (!TOPIC_PATTERN.matcher(topic).matches())
            throw new FcmInvalidRequestException("Invalid FCM topic name: '" + topic + "'. Must match [a-zA-Z0-9-_.~%]{1,900}");
    }

    private void validateToken(String token) {
        if (token == null || token.isBlank())
            throw new FcmInvalidRequestException("Device token must not be blank");
    }

    private void validateCondition(String condition) {
        if (condition == null || condition.isBlank())
            throw new FcmInvalidRequestException("Condition expression must not be blank");

        int depth = 0;
        for (char c : condition.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth < 0)
                throw new FcmInvalidRequestException("Condition has unbalanced parentheses");
        }
        if (depth != 0)
            throw new FcmInvalidRequestException("Condition has unbalanced parentheses");
    }

    private void validateApns(FcmApnsConfig apnsConfig) {
        FcmAps aps = apnsConfig.getAps();
        if (aps == null)
            return;

        if (aps.getBadge() != null && aps.getBadge() < 0)
            throw new FcmInvalidRequestException("APNs badge must be >= 0");
    }
}
