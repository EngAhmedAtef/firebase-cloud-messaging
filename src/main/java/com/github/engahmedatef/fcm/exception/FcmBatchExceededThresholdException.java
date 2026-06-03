package com.github.engahmedatef.fcm.exception;

/**
 * Thrown when a {@code sendToDevices} batch exceeds the configured failure rate threshold
 * ({@code send.failure-threshold-per-batch}). The batch is aborted; devices processed before
 * the threshold was hit may have already received the message.
 */
public class FcmBatchExceededThresholdException extends FcmException {

    public FcmBatchExceededThresholdException(String message) {
        super(message);
    }

    public FcmBatchExceededThresholdException(String message, Throwable cause) {
        super(message, cause);
    }
}
