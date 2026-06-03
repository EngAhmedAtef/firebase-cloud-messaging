package io.github.engahmedatef.fcm.internal.transport.target;

import io.github.engahmedatef.fcm.model.FcmDevice;

public record DeviceTarget(FcmDevice device) implements FcmTarget {
}
