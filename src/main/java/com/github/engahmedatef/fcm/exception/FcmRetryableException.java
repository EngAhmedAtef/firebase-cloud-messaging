package com.github.engahmedatef.fcm.exception;

/**
 * Marker base class for transient FCM errors that are eligible for automatic retry
 * by the configured {@link com.github.engahmedatef.fcm.spi.FcmRetryPolicy}.
 */
public abstract class FcmRetryableException extends FcmException {

    protected FcmRetryableException(String message) {
        super(message);
    }

    protected FcmRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
