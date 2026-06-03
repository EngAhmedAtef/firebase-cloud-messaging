package com.github.engahmedatef.fcm.internal.transport.target;

import com.github.engahmedatef.fcm.model.FcmDevice;

public record DeviceTarget(FcmDevice device) implements FcmTarget {
}
