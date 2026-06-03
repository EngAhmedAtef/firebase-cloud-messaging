package com.github.engahmedatef.fcm.internal.service;

import com.github.engahmedatef.fcm.internal.transport.MessagePayload;
import com.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import com.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import com.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import com.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.spi.FcmPayloadBuilder;

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
