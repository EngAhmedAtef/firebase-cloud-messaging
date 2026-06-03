package com.github.engahmedatef.fcm.unit;

import com.github.engahmedatef.fcm.internal.metrics.DefaultFcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFcmMetricsRecorderTest {

    private SimpleMeterRegistry meterRegistry;
    private DefaultFcmMetricsRecorder recorder;
    private FcmExecutionContext context;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        recorder = new DefaultFcmMetricsRecorder(meterRegistry);
        var factory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());
        context = factory.create("test-project", null);
    }

    @Test
    void increment_registersCounterWithProjectIdAndTargetTags() {
        recorder.increment(FcmMetrics.MESSAGES_SENT, context);

        Counter counter = meterRegistry.find(FcmMetrics.MESSAGES_SENT)
                .tag(FcmMetrics.Tags.PROJECT_ID, "test-project")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementWithExtraTag_addsTagToCounter() {
        recorder.increment(FcmMetrics.MESSAGES_SENT, context, FcmMetrics.Tags.OUTCOME, "success");

        Counter counter = meterRegistry.find(FcmMetrics.MESSAGES_SENT)
                .tag(FcmMetrics.Tags.OUTCOME, "success")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordTimer_registersTimerWithCorrectDuration() {
        recorder.recordTimer(FcmMetrics.MESSAGES_DURATION, context, Duration.ofMillis(150));

        Timer timer = meterRegistry.find(FcmMetrics.MESSAGES_DURATION)
                .tag(FcmMetrics.Tags.PROJECT_ID, "test-project")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isCloseTo(150.0, org.assertj.core.data.Offset.offset(10.0));
    }

    @Test
    void recordDistribution_registersSummaryWithCountAndTotal() {
        recorder.recordDistribution(FcmMetrics.BATCH_SIZE, context, 5.0);

        DistributionSummary summary = meterRegistry.find(FcmMetrics.BATCH_SIZE)
                .tag(FcmMetrics.Tags.PROJECT_ID, "test-project")
                .summary();

        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.totalAmount()).isEqualTo(5.0);
    }

    @Test
    void multipleIncrements_accumulateCounts() {
        recorder.increment(FcmMetrics.MESSAGES_SENT, context);
        recorder.increment(FcmMetrics.MESSAGES_SENT, context);
        recorder.increment(FcmMetrics.MESSAGES_SENT, context);

        Counter counter = meterRegistry.find(FcmMetrics.MESSAGES_SENT)
                .tag(FcmMetrics.Tags.PROJECT_ID, "test-project")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }
}
