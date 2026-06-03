package com.github.engahmedatef.fcm.internal.metrics;

import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;

import java.time.Duration;

@AllArgsConstructor
public class DefaultFcmMetricsRecorder implements FcmMetricsRecorder {

    private final MeterRegistry meterRegistry;

    @Override
    public void increment(String metric, FcmExecutionContext context) {
        Counter.builder(metric)
                .tags(context.metricTags())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void increment(String metric, FcmExecutionContext context, String key, String value) {
        Counter.builder(metric)
                .tags(context.metricTags())
                .tag(key, value)
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordTimer(String metric, FcmExecutionContext context, Duration duration) {
        Timer.builder(metric)
                .tags(context.metricTags())
                .register(meterRegistry)
                .record(duration);
    }

    @Override
    public void recordDistribution(String metric, FcmExecutionContext context, Double value) {
        DistributionSummary.builder(metric)
                .tags(context.metricTags())
                .register(meterRegistry)
                .record(value);
    }
}
