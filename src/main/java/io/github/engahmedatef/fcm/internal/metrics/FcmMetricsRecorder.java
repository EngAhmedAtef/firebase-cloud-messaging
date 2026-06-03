package io.github.engahmedatef.fcm.internal.metrics;

import io.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;

import java.time.Duration;

/** Internal SPI for recording FCM metrics via Micrometer or a no-op fallback. */
public interface FcmMetricsRecorder {

    void increment(String metric, FcmExecutionContext context);

    void increment(String metric, FcmExecutionContext context, String key, String value);

    void recordTimer(String metric, FcmExecutionContext context, Duration duration);

    void recordDistribution(String metric, FcmExecutionContext context, Double value);
}
