package io.github.engahmedatef.fcm.model.android;

/**
 * FCM message delivery priority for Android devices.
 *
 * <p>Maps to the {@code android.priority} field in the FCM HTTP v1 API.
 * See the
 * <a href="https://firebase.google.com/docs/cloud-messaging/android/message-priority">FCM priority documentation</a>
 * for battery and delivery trade-offs.
 */
public enum FcmAndroidMessagePriority {
    /**
     * Delivers the message immediately and wakes the device if it is in doze mode.
     * Use for time-sensitive, user-visible alerts (e.g. chat messages, alarms).
     */
    HIGH,
    /**
     * Delivers the message opportunistically without waking the device.
     * Use for non-urgent background data syncs.
     */
    NORMAL
}
