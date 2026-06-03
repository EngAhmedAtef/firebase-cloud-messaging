package io.github.engahmedatef.fcm.spi;

import io.github.engahmedatef.fcm.internal.transport.MessagePayload;
import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import io.github.engahmedatef.fcm.model.FcmMessage;

/**
 * Converts the public domain model ({@link FcmMessage} + {@link FcmTarget}) into the internal
 * wire payload ({@link MessagePayload}) that is serialized and sent to the FCM HTTP v1 API.
 * Override the default bean to take full control of payload construction.
 *
 * <p>Called once per send attempt, after the enricher chain and before the interceptor chain.
 */
public interface FcmPayloadBuilder {

    /**
     * Builds the wire payload for the given target and message.
     *
     * @param target  the FCM send target (device token, topic, or condition)
     * @param message the domain-level message to convert
     * @return the wire payload ready for HTTP serialization; must not be {@code null}
     */
    MessagePayload build(FcmTarget target, FcmMessage message);
}
