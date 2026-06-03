package io.github.engahmedatef.fcm.model;

import lombok.Builder;

/**
 * Identifies a single target device for an FCM send operation.
 *
 * <p>Construct instances using the provided builder:
 * <pre>{@code
 * FcmDevice device = FcmDevice.builder()
 *         .type(FcmDeviceType.ANDROID)
 *         .token("eFcm_device_registration_token")
 *         .build();
 * }</pre>
 *
 * @param type  the platform type of the device ({@link FcmDeviceType#ANDROID} or {@link FcmDeviceType#IOS})
 * @param token the FCM registration token issued to this device; must not be blank
 */
@Builder
public record FcmDevice(FcmDeviceType type, String token) {
}
