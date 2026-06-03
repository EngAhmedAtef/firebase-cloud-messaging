package com.github.engahmedatef.fcm.internal.metrics;

import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;

import java.time.Duration;

public class NoOpFcmMetricsRecorder implements FcmMetricsRecorder {

    @Override
    public void increment(String metric, FcmExecutionContext context) {}

    @Override
    public void increment(String metric, FcmExecutionContext context, String key, String value) {}

    @Override
    public void recordTimer(String metric, FcmExecutionContext context, Duration duration) {}

    @Override
    public void recordDistribution(String metric, FcmExecutionContext context, Double value) {}
}
