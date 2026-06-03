package com.github.engahmedatef.fcm.exception;

/**
 * Thrown when the SDK or a registered {@link com.github.engahmedatef.fcm.spi.FcmRequestValidator} rejects
 * the message before it is sent. Inspect the message for the specific constraint that was violated.
 */
public class FcmInvalidRequestException extends FcmException {

    public FcmInvalidRequestException(String message) {
        super(message);
    }

    public FcmInvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
