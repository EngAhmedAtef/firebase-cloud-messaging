package io.github.engahmedatef.fcm.exception;

/**
 * Thrown when the FCM project quota is exhausted (HTTP 429 / QUOTA_EXCEEDED).
 * Apply exponential back-off and contact Firebase support if the issue persists.
 */
public class FcmQuotaExceededException extends FcmException {

    public FcmQuotaExceededException(String message) {
        super(message);
    }

    public FcmQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
