package io.github.engahmedatef.fcm.exception;

/**
 * Thrown when a low-level HTTP transport error prevents the SDK from reaching the FCM API
 * (e.g. connection timeout, TLS handshake failure, or a network reset mid-stream).
 */
public class FcmTransportException extends FcmException {

    public FcmTransportException(String message) {
        super(message);
    }

    public FcmTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
