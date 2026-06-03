package com.github.engahmedatef.fcm.spi;

import lombok.Getter;

/**
 * Structured log event constants emitted by the SDK to {@link FcmLogger}.
 * Each constant has a {@link #getValue()} that returns the event name as a dot-separated string.
 */
@Getter
public enum FcmLogEvent {
    SEND_STARTED("fcm.send.started"),
    SEND_SUCCEEDED("fcm.send.succeeded"),
    SEND_FAILED("fcm.send.failed"),
    TOKEN_CLEANUP_STARTED("fcm.token.cleanup.started"),
    TOKEN_CLEANUP_SUCCEEDED("fcm.token.cleanup.succeeded"),
    TOKEN_CLEANUP_FAILED("fcm.token.cleanup.failed"),
    RETRY_ATTEMPT("fcm.retry.attempt"),
    BATCH_SEND_STARTED("fcm.batch.started"),
    BATCH_SEND_COMPLETED("fcm.batch.completed");

    private final String value;

    FcmLogEvent(String value) {
        this.value = value;
    }
}
