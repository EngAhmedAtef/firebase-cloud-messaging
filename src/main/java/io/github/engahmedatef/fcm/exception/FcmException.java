package io.github.engahmedatef.fcm.exception;

/**
 * Base class for all FCM SDK exceptions. All subtypes are unchecked.
 * Catch specific subtypes rather than this base class to distinguish recoverable
 * from non-recoverable errors.
 */
public abstract class FcmException extends RuntimeException {

    protected FcmException(String message) {
        super(message);
    }

    protected FcmException(String message, Throwable cause) {
        super(message, cause);
    }
}
