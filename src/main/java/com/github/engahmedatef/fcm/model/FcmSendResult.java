package com.github.engahmedatef.fcm.model;

import com.github.engahmedatef.fcm.exception.FcmException;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable result of a single-device or single-topic send operation.
 *
 * <p>On success, {@link #getMessageId()} returns the FCM-assigned message name in the format
 * {@code projects/{project_id}/messages/{message_id}}.
 *
 * <p>On failure (non-exception path), {@link #getError()} contains the typed
 * {@link FcmException} subtype. In batch operations the SDK catches per-device failures and
 * wraps them in a {@code FcmSendResult} rather than terminating the reactive stream, so callers
 * can inspect individual outcomes after the batch completes.
 *
 * <p>Use the static factory methods {@link #success(String)} and {@link #failure(Throwable)}
 * to construct instances.
 */
@Getter
@ToString
public final class FcmSendResult {

    private final boolean success;
    private final String messageId;
    private final Throwable error;

    private FcmSendResult(boolean success, String messageId, Throwable error) {
        this.success = success;
        this.messageId = messageId;
        this.error = error;
    }

    public static FcmSendResult success(String messageId) {
        return new FcmSendResult(true, messageId, null);
    }

    public static FcmSendResult failure(Throwable error) {
        return new FcmSendResult(false, null, error);
    }
}
