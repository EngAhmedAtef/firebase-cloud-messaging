package io.github.engahmedatef.fcm.internal.observability;

import io.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.observation.Observation;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-request execution context propagated through the reactive pipeline via Reactor's
 * {@link reactor.util.context.Context}.
 *
 * <p>Each send operation creates one {@code FcmExecutionContext}. Batch operations create a
 * parent context plus one child context per device. The context carries:
 * <ul>
 *   <li>a {@link #correlationId} (UUID) for end-to-end traceability</li>
 *   <li>a Micrometer {@link io.micrometer.observation.Observation} for distributed tracing and metrics</li>
 *   <li>a monotonic {@link #startTime} used to compute send latency</li>
 *   <li>a thread-safe {@link #retryCount} incremented on each retry attempt</li>
 *   <li>a {@link #attributes} map for ad-hoc data sharing between SDK components</li>
 * </ul>
 *
 * <p>Retrieve the current context inside an enricher or interceptor via:
 * <pre>{@code
 * Mono.deferContextual(ctx -> {
 *     FcmExecutionContext execCtx = ctx.getOrDefault(FcmExecutionContext.KEY, null);
 *     ...
 * })
 * }</pre>
 *
 * <p>This class is part of the {@code internal} package and is not part of the public API;
 * it is exposed to SPI implementations through the {@link io.github.engahmedatef.fcm.spi} context records.
 */
@Getter
public final class FcmExecutionContext {

    public static final Class<FcmExecutionContext> KEY = FcmExecutionContext.class;

    private final String correlationId;
    private final String projectId;
    private final String targetType;
    private final Instant startTime;
    private final AtomicInteger retryCount;
    private final Observation observation;
    private final Map<String, Object> attributes;

    public FcmExecutionContext(String correlationId, String projectId, String targetType, Observation observation) {
        this.correlationId = correlationId;
        this.projectId = projectId;
        this.targetType = targetType;
        this.startTime = Instant.now();
        this.observation = observation;
        this.retryCount = new AtomicInteger(0);
        this.attributes = new ConcurrentHashMap<>();
    }

    public Duration duration() {
        return Duration.between(startTime, Instant.now());
    }

    public int incrementRetryCount() {
        return retryCount.incrementAndGet();
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Tags metricTags() {
        Tags tags = Tags.of(
                FcmMetrics.Tags.PROJECT_ID, projectId,
                FcmMetrics.Tags.TARGET, targetType
        );

        Object deviceType = attributes.get(FcmMetrics.Tags.DEVICE_TYPE);
        if (deviceType != null) {
            tags = tags.and(FcmMetrics.Tags.DEVICE_TYPE, deviceType.toString());
        }

        return tags;
    }
}
