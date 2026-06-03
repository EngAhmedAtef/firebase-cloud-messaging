package com.github.engahmedatef.fcm.integration;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.engahmedatef.fcm.internal.metrics.DefaultFcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import com.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
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
import com.github.engahmedatef.fcm.model.FcmSendResult;
import com.github.engahmedatef.fcm.support.FakeAccessTokenProvider;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

class RetryIT {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String SUCCESS_BODY = """
            {"name":"projects/test-project/messages/msg-001"}
            """;

    private static final String SERVER_ERROR_BODY = """
            {
              "error": {
                "code": 500,
                "message": "Internal error",
                "details": [{"errorCode": "INTERNAL"}]
              }
            }
            """;

    private static final String INVALID_ARG_BODY = """
            {
              "error": {
                "code": 400,
                "message": "Invalid argument",
                "details": [{"errorCode": "INVALID_ARGUMENT"}]
              }
            }
            """;

    private DefaultReactiveFcmClient buildClient(int maxRetries, SimpleMeterRegistry registry) {
        String sendUrl = wm.baseUrl() + "/v1/projects/test-project/messages:send";
        WebClient webClient = WebClient.builder().baseUrl(wm.baseUrl()).build();
        var tokenProvider = new FakeAccessTokenProvider();
        var metricsRecorder = registry != null ? new DefaultFcmMetricsRecorder(registry) : new NoOpFcmMetricsRecorder();
        var sender = new WebClientFcmMessageSender(tokenProvider, webClient, sendUrl,
                ObservationRegistry.NOOP, "test-project");
        var logger = new DefaultFcmLogger();
        var observationManager = new DefaultFcmObservationManager();
        var retryPolicy = new DefaultFcmRetryPolicy(maxRetries, Duration.ofMillis(5), Duration.ofMillis(50),
                metricsRecorder, logger, observationManager);
        var errorHandler = new DefaultFcmResponseErrorHandler(null, logger);
        var payloadBuilder = new DefaultFcmPayloadBuilder();
        var contextFactory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());

        return new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                null, 4, 10, 0.5, "test-project", logger,
                new ValidatorChain(List.of(), false),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(), false),
                contextFactory, observationManager, metricsRecorder
        );
    }

    @Test
    void transientServerError_retriesAndSucceeds() {
        wm.stubFor(post(anyUrl())
                .inScenario("retry-then-success")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SERVER_ERROR_BODY))
                .willSetStateTo("first-fail"));

        wm.stubFor(post(anyUrl())
                .inScenario("retry-then-success")
                .whenScenarioStateIs("first-fail")
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SERVER_ERROR_BODY))
                .willSetStateTo("second-fail"));

        wm.stubFor(post(anyUrl())
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-fail")
                .willReturn(okJson(SUCCESS_BODY)));

        DefaultReactiveFcmClient client = buildClient(2, null);

        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectNextMatches(FcmSendResult::isSuccess)
                .verifyComplete();

        wm.verify(3, postRequestedFor(anyUrl()));
    }

    @Test
    void networkFault_wrapsAsFcmTransportException_notRetried() {
        // WebClientRequestException is mapped to FcmTransportException inside the sender.
        // FcmTransportException is not FcmRetryableException, so the retry policy does NOT retry it.
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        DefaultReactiveFcmClient client = buildClient(2, null);

        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectError(com.github.engahmedatef.fcm.exception.FcmTransportException.class)
                .verify(Duration.ofSeconds(5));

        // No retry — exactly 1 request despite maxRetries=2
        wm.verify(1, postRequestedFor(anyUrl()));
    }

    @Test
    void allRetriesExhausted_propagatesLastError() {
        // 500/INTERNAL → FcmInternalException (extends FcmRetryableException) → retried.
        // maxRetries=2: 1 initial + 2 retries = 3 total requests, then error propagates.
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SERVER_ERROR_BODY)));

        DefaultReactiveFcmClient client = buildClient(2, null);

        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectError(com.github.engahmedatef.fcm.exception.FcmInternalException.class)
                .verify(Duration.ofSeconds(10));

        wm.verify(3, postRequestedFor(anyUrl()));
    }

    @Test
    void nonRetryableError_failsImmediately_noRetries() {
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(INVALID_ARG_BODY)));

        DefaultReactiveFcmClient client = buildClient(3, null);

        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectError(com.github.engahmedatef.fcm.exception.FcmInvalidRequestException.class)
                .verify(Duration.ofSeconds(5));

        // Non-retryable: exactly 1 request, no retries
        wm.verify(1, postRequestedFor(anyUrl()));
    }

    @Test
    void retryMetric_incrementedPerRetry() {
        // FcmInternalException (from 500/INTERNAL) extends FcmRetryableException → is retried.
        // With maxRetries=2 and all attempts failing, expect 2 retry counter increments.
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SERVER_ERROR_BODY)));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DefaultReactiveFcmClient client = buildClient(2, registry);

        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectError()
                .verify(Duration.ofSeconds(10));

        double retryCount = registry.find(FcmMetrics.MESSAGES_RETRIED).counter().count();
        assertThat(retryCount).isEqualTo(2.0);
    }
}
