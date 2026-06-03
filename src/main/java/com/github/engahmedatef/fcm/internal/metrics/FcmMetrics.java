package com.github.engahmedatef.fcm.internal.metrics;

/**
 * Central registry of Micrometer metric names and tag key constants used throughout the SDK.
 *
 * <p>All metric names follow the dot-separated convention {@code fcm.*}.
 * Instrument names are stable across releases; tag keys are listed in the nested {@link Tags} class.
 *
 * <h3>Counters</h3>
 * <ul>
 *   <li>{@link #MESSAGES_SENT} — total send attempts, tagged by {@code outcome} (success/failure)</li>
 *   <li>{@link #MESSAGES_RETRIED} — individual retry attempts</li>
 *   <li>{@link #RETRIES_EXHAUSTED} — sends that used all retry budget</li>
 *   <li>{@link #ERRORS_CLASSIFIED} — error events tagged by {@code errorCategory}</li>
 *   <li>{@link #BATCH_ABORTED} — batch operations aborted due to threshold breach</li>
 *   <li>{@link #TOKEN_REFRESH} — OAuth token refresh events</li>
 * </ul>
 *
 * <h3>Timers</h3>
 * <ul>
 *   <li>{@link #MESSAGES_DURATION} — end-to-end latency per send (including retries)</li>
 *   <li>{@link #TOKEN_REFRESH_DURATION} — time to obtain a fresh OAuth token</li>
 * </ul>
 *
 * <h3>Distribution Summaries</h3>
 * <ul>
 *   <li>{@link #BATCH_SIZE} — number of devices per batch window</li>
 *   <li>{@link #BATCH_FAILURE_RATE} — fraction of failures within a batch window [0.0 – 1.0]</li>
 * </ul>
 */
public final class FcmMetrics {

    private FcmMetrics() {}

    public static final String MESSAGES_SENT = "fcm.messages.sent";
    public static final String MESSAGES_DURATION = "fcm.messages.duration";
    public static final String MESSAGES_RETRIED = "fcm.messages.retried";
    public static final String BATCH_SIZE = "fcm.batch.size";
    public static final String BATCH_FAILURE_RATE = "fcm.batch.failure.rate";
    public static final String BATCH_ABORTED = "fcm.batch.aborted";
    public static final String TOKEN_REFRESH = "fcm.token.refresh";
    public static final String TOKEN_REFRESH_DURATION = "fcm.token.refresh.duration";
    public static final String RETRIES_EXHAUSTED = "fcm.messages.retries.exhausted";
    public static final String ERRORS_CLASSIFIED = "fcm.errors.classified";

    public static class Tags {
        public static final String OUTCOME = "outcome";
        public static final String REASON = "reason";
        public static final String EXCEPTION = "exception";
        public static final String DEVICE_TYPE = "deviceType";
        public static final String PROJECT_ID = "projectId";
        public static final String TOPIC = "topic";
        public static final String CONDITION = "condition";
        public static final String ERROR_CATEGORY = "errorCategory";
        public static final String TARGET = "target";
        public static final String HTTP_STATUS_CLASS = "httpStatusClass";
        public static final String CORRELATION_ID = "correlationId";
        public static final String RETRY_COUNT = "retryCount";
        public static final String DEVICE_TOKEN_PREFIX = "deviceTokenPrefix";
        public static final String MESSAGE_ID = "messageId";
        public static final String BATCH_SIZE = "batchSize";
        public static final String CONCURRENCY = "concurrency";
        public static final String FAILURES = "failures";
        public static final String SUCCESSES = "successes";
        public static final String FAILURE_RATE = "failureRate";
    }
}
