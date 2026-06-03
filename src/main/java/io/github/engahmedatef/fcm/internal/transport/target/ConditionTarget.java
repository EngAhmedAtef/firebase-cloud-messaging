package io.github.engahmedatef.fcm.internal.transport.target;

/** FCM send target representing a topic-condition expression (e.g. {@code "'TopicA' in topics && 'TopicB' in topics"}). */
public record ConditionTarget(String condition) implements FcmTarget {
}
