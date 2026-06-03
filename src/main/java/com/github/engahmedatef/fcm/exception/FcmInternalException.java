package com.github.engahmedatef.fcm.exception;

/**
 * Thrown when the SDK encounters an unexpected internal error unrelated to the FCM API response
 * (e.g. payload serialisation failure, unexpected null from a required SPI).
 */
public class FcmInternalException extends FcmRetryableException {

    public FcmInternalException(String message) {
        super(message);
    }

    public FcmInternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
