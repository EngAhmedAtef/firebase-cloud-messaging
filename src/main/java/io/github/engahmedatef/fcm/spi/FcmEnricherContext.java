package io.github.engahmedatef.fcm.spi;

import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;

import java.util.Map;

/**
 * Contextual metadata supplied to each {@link FcmMessageEnricher} invocation.
 *
 * @param target     the FCM send target (device, topic, or condition)
 * @param projectId  the configured Firebase project ID
 * @param attributes mutable map for passing data between enrichers in the same chain
 */
public record FcmEnricherContext(
        FcmTarget target,
        String projectId,
        Map<String, Object> attributes
) {}
