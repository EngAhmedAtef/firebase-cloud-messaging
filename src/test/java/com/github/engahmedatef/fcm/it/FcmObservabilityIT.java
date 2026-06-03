package com.github.engahmedatef.fcm.it;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.engahmedatef.fcm.api.ReactiveFcmClient;
import com.github.engahmedatef.fcm.internal.metrics.DefaultFcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmObservationManager;
import com.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmLogger;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmPayloadBuilder;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmResponseErrorHandler;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmRetryPolicy;
import com.github.engahmedatef.fcm.internal.service.DefaultReactiveFcmClient;
import com.github.engahmedatef.fcm.internal.service.EnricherChain;
import com.github.engahmedatef.fcm.internal.service.InterceptorChain;
import com.github.engahmedatef.fcm.internal.service.ValidatorChain;
import com.github.engahmedatef.fcm.internal.service.sender.WebClientFcmMessageSender;
import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmDeviceType;
import com.github.engahmedatef.fcm.support.FakeAccessTokenProvider;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class FcmObservabilityIT {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ReactiveFcmClient client;
    private SimpleMeterRegistry meterRegistry;
    private TestObservationRegistry observationRegistry;

    @BeforeEach
    void setUp() {
        String sendUrl = wm.baseUrl() + "/v1/projects/test-project/messages:send";
        observationRegistry = TestObservationRegistry.create();
        meterRegistry = new SimpleMeterRegistry();
        var metricsRecorder = new DefaultFcmMetricsRecorder(meterRegistry);

        WebClient webClient = WebClient.builder().baseUrl(wm.baseUrl()).build();
        var tokenProvider = new FakeAccessTokenProvider();
        var sender = new WebClientFcmMessageSender(tokenProvider, webClient, sendUrl,
                observationRegistry, "test-project");
        var logger = new DefaultFcmLogger();
        var observationManager = new DefaultFcmObservationManager();
        var retryPolicy = new DefaultFcmRetryPolicy(0, Duration.ofMillis(1), Duration.ofMillis(10), metricsRecorder, logger, observationManager);
        var errorHandler = new DefaultFcmResponseErrorHandler(null, logger);
        var payloadBuilder = new DefaultFcmPayloadBuilder();
        var contextFactory = new DefaultFcmExecutionContextFactory(observationRegistry, new FcmSendObservationConvention());

        client = new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                null, 4, 10, 0.5,
                "test-project", logger,
                new ValidatorChain(List.of(), false),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(), false),
                contextFactory, observationManager, metricsRecorder
        );
    }

    @Test
    void successfulSendCreatesObservationWithCorrectName() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectNextMatches(r -> r.isSuccess())
                .verifyComplete();

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.http.send");
    }

    @Test
    void sendToDeviceObservationCreatedWithProjectIdTag() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectNextMatches(r -> r.isSuccess())
                .verifyComplete();

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.send")
                .that()
                .hasLowCardinalityKeyValue("projectId", "test-project");
    }

    @Test
    void batchSendCreatesObservationWithBatchTargetType() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        List<FcmDevice> devices = IntStream.range(0, 3)
                .mapToObj(i -> FcmDevice.builder()
                        .type(FcmDeviceType.ANDROID)
                        .token("token-" + i + "-padded12345678")
                        .build())
                .toList();

        StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.send")
                .that()
                .hasLowCardinalityKeyValue("targetType", "batch");
    }

    @Test
    void successfulSendRecordsBatchSizeDistributionSummary() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        List<FcmDevice> devices = IntStream.range(0, 5)
                .mapToObj(i -> FcmDevice.builder()
                        .type(FcmDeviceType.ANDROID)
                        .token("token-" + i + "-padded12345678")
                        .build())
                .toList();

        StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        var batchSizeSummary = meterRegistry.find("fcm.batch.size").summary();
        assertThat(batchSizeSummary).isNotNull();
        assertThat(batchSizeSummary.count()).isGreaterThan(0);
        assertThat(batchSizeSummary.totalAmount()).isEqualTo(5.0);
    }
}
