package io.github.engahmedatef.fcm.exception;

/**
 * Thrown when FCM returns a 503 Service Unavailable response.
 * Extends {@link FcmRetryableException} semantics — the default retry policy will
 * back off and retry automatically.
 */
public class FcmUnavailableException extends FcmException {

    public FcmUnavailableException(String message) {
        super(message);
    }

    public FcmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
