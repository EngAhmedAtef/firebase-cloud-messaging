package com.github.engahmedatef.fcm.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.engahmedatef.fcm.exception.*;
import com.github.engahmedatef.fcm.internal.http.ReactorNettyWebClientFactory;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmRetryPolicy;
import com.github.engahmedatef.fcm.internal.service.sender.FcmSenderResponse;
import com.github.engahmedatef.fcm.internal.service.sender.WebClientFcmMessageSender;
import com.github.engahmedatef.fcm.internal.transport.MessagePayload;
import com.github.engahmedatef.fcm.spi.FcmRetryPolicy;
import com.github.engahmedatef.fcm.support.FakeAccessTokenProvider;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

class WebClientFcmMessageSenderIT {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private WebClientFcmMessageSender sender;
    private String sendUrl;
    private MessagePayload payload;

    @BeforeEach
    void setUp() {
        sendUrl = wm.baseUrl() + "/v1/projects/test-project/messages:send";
        WebClient webClient = WebClient.builder()
                .baseUrl(wm.baseUrl())
                .build();
        sender = new WebClientFcmMessageSender(new FakeAccessTokenProvider(), webClient, sendUrl,
                ObservationRegistry.NOOP, "test-project");
        payload = FcmTestFixtures.aDeviceMessagePayload();
    }

    private static reactor.core.publisher.Mono<FcmSenderResponse> sendWithContext(WebClientFcmMessageSender sender, MessagePayload payload) {
        FcmExecutionContext ctx = new FcmExecutionContext("test-correlation", "test-project", "device", Observation.NOOP);
        return sender.send(payload).contextWrite(c -> c.put(FcmExecutionContext.KEY, ctx));
    }

    @Test
    void successfulSendReturnsMessageId() {
        wm.stubFor(post(urlEqualTo("/v1/projects/test-project/messages:send"))
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        StepVerifier.create(sendWithContext(sender, payload))
                .assertNext(response -> {
                    assertThat(response.success()).isTrue();
                    assertThat(response.successResponse().name()).isEqualTo("projects/test-project/messages/msg-001");
                })
                .verifyComplete();

        wm.verify(postRequestedFor(urlEqualTo("/v1/projects/test-project/messages:send"))
                .withHeader("Authorization", equalTo("Bearer " + FakeAccessTokenProvider.TEST_TOKEN))
                .withHeader("Content-Type", containing("application/json")));
    }

    @Test
    void returns400InvalidArgument() {
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorBody("INVALID_ARGUMENT", "Invalid registration token"))));

        StepVerifier.create(sendWithContext(sender, payload))
                .assertNext(response -> {
                    assertThat(response.success()).isFalse();
                    assertThat(response.errorResponse().getError().getMessage()).contains("Invalid registration token");
                })
                .verifyComplete();
    }

    @Test
    void returns404UnregisteredToken() {
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorBody("UNREGISTERED", "Token is unregistered"))));

        StepVerifier.create(sendWithContext(sender, payload))
                .assertNext(response -> assertThat(response.success()).isFalse())
                .verifyComplete();
    }

    @Test
    void networkFaultResultsInTransportException() {
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        StepVerifier.create(sendWithContext(sender, payload))
                .expectError(FcmTransportException.class)
                .verify(Duration.ofSeconds(5));
    }

    private String errorBody(String errorCode, String message) {
        return String.format("""
                {
                  "error": {
                    "code": 400,
                    "message": "%s",
                    "status": "FAILED_PRECONDITION",
                    "details": [
                      { "errorCode": "%s" }
                    ]
                  }
                }
                """, message, errorCode);
    }
}
