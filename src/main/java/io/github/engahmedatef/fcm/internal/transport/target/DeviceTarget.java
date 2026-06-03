package io.github.engahmedatef.fcm.internal.transport.target;

import io.github.engahmedatef.fcm.model.FcmDevice;

/** FCM send target for a single device identified by a registration token. */
public record DeviceTarget(FcmDevice device) implements FcmTarget {
}
