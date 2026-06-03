package com.github.engahmedatef.fcm.internal.metrics;

import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;

import java.time.Duration;

public interface FcmMetricsRecorder {

    void increment(String metric, FcmExecutionContext context);

    void increment(String metric, FcmExecutionContext context, String key, String value);

    void recordTimer(String metric, FcmExecutionContext context, Duration duration);

    void recordDistribution(String metric, FcmExecutionContext context, Double value);
}
