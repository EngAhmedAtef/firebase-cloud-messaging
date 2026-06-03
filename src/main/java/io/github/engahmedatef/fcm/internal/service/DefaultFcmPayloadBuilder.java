package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.internal.transport.MessagePayload;
import io.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import io.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import io.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import io.github.engahmedatef.fcm.model.FcmDevice;
import io.github.engahmedatef.fcm.model.FcmMessage;
import io.github.engahmedatef.fcm.spi.FcmPayloadBuilder;

/** Builds a {@link io.github.engahmedatef.fcm.internal.transport.MessagePayload} from a target and {@link io.github.engahmedatef.fcm.model.FcmMessage}. */
public class DefaultFcmPayloadBuilder implements FcmPayloadBuilder {

    @Override
    public MessagePayload build(FcmTarget target, FcmMessage message) {
        if (target instanceof DeviceTarget deviceTarget) {
            FcmDevice device = deviceTarget.device();
            return FcmPayloadMapper.toDeviceMessagePayload(message, device.token());
        } else if (target instanceof TopicTarget topicTarget) {
            return FcmPayloadMapper.toTopicMessagePayload(message, topicTarget.topic());
        } else if (target instanceof ConditionTarget conditionTarget) {
            return FcmPayloadMapper.toConditionMessagePayload(message, conditionTarget.condition());
        } else {
            throw new IllegalArgumentException("Unsupported target type: " + target.getClass().getName());
        }
    }
}
