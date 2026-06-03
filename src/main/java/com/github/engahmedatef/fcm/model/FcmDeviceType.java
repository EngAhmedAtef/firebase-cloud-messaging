package com.github.engahmedatef.fcm.model;

/**
 * Identifies the platform of a target device.
 * Used by {@link FcmDevice} and carried through the pipeline so enrichers, interceptors,
 * and metrics can branch on platform.
 */
public enum FcmDeviceType {
    /** Android device targeted via the FCM HTTP v1 API. */
    ANDROID,
    /** Apple iOS/iPadOS device targeted via the FCM HTTP v1 API (uses APNs under the hood). */
    IOS
}
