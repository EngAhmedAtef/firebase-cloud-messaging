package io.github.engahmedatef.fcm.unit;

import io.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import io.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import io.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import io.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import io.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import io.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.core.instrument.Tags;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmExecutionContextTest {

    private DefaultFcmExecutionContextFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());
    }

    @Test
    void metricTags_containsProjectIdAndTargetType() {
        FcmExecutionContext ctx = factory.create("my-project", null);

        Tags tags = ctx.metricTags();

        assertThat(tags.stream().map(io.micrometer.core.instrument.Tag::getKey))
                .contains(FcmMetrics.Tags.PROJECT_ID, FcmMetrics.Tags.TARGET);
        assertThat(tags.stream()
                .filter(t -> t.getKey().equals(FcmMetrics.Tags.PROJECT_ID))
                .map(io.micrometer.core.instrument.Tag::getValue)
                .findFirst()).hasValue("my-project");
    }

    @Test
    void metricTags_includesDeviceTypeWhenAttributePresent() {
        FcmExecutionContext ctx = factory.create("test-project", new DeviceTarget(FcmTestFixtures.aDevice()));
        ctx.putAttribute(FcmMetrics.Tags.DEVICE_TYPE, "ANDROID");

        Tags tags = ctx.metricTags();

        assertThat(tags.stream()
                .filter(t -> t.getKey().equals(FcmMetrics.Tags.DEVICE_TYPE))
                .map(io.micrometer.core.instrument.Tag::getValue)
                .findFirst()).hasValue("ANDROID");
    }

    @Test
    void metricTags_omitsDeviceTypeWhenAbsent() {
        FcmExecutionContext ctx = factory.create("test-project", null);

        Tags tags = ctx.metricTags();

        assertThat(tags.stream().map(io.micrometer.core.instrument.Tag::getKey))
                .doesNotContain(FcmMetrics.Tags.DEVICE_TYPE);
    }

    @Test
    void incrementRetryCount_returnsSequentialValues() {
        FcmExecutionContext ctx = factory.create("test-project", null);

        assertThat(ctx.incrementRetryCount()).isEqualTo(1);
        assertThat(ctx.incrementRetryCount()).isEqualTo(2);
        assertThat(ctx.incrementRetryCount()).isEqualTo(3);
    }

    @Test
    void duration_isNonNegative() {
        FcmExecutionContext ctx = factory.create("test-project", null);

        assertThat(ctx.duration().toNanos()).isGreaterThanOrEqualTo(0);
    }
}
