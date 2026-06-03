package com.github.engahmedatef.fcm.internal.service.sender;

import com.github.engahmedatef.fcm.exception.FcmTransportException;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import com.github.engahmedatef.fcm.internal.observability.FcmCorrelationId;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import com.github.engahmedatef.fcm.internal.transport.FcmSendSuccessResponse;
import com.github.engahmedatef.fcm.internal.transport.MessagePayload;
import com.github.engahmedatef.fcm.spi.FcmAccessTokenProvider;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

public class WebClientFcmMessageSender implements FcmMessageSender {
    private final FcmAccessTokenProvider tokenProvider;
    private final WebClient webClient;
    private final String sendUrl;
    private final ObservationRegistry observationRegistry;
    private final String projectId;

    public WebClientFcmMessageSender(FcmAccessTokenProvider tokenProvider,
                                     WebClient webClient,
                                     String sendUrl,
                                     ObservationRegistry observationRegistry,
                                     String projectId) {
        this.tokenProvider = tokenProvider;
        this.webClient = webClient;
        this.sendUrl = sendUrl;
        this.observationRegistry = observationRegistry;
        this.projectId = projectId;
    }

    @Override
    public Mono<FcmSenderResponse> send(MessagePayload payload) {
        return Mono.deferContextual(contextView -> {
            FcmExecutionContext parentContext = contextView.getOrDefault(FcmExecutionContext.KEY, null);
            if (parentContext == null)
                throw new FcmTransportException("FcmExecutionContext not found in the context");

            String correlationId = parentContext.getCorrelationId();
            Observation obs = Observation.createNotStarted("fcm.http.send", observationRegistry)
                    .lowCardinalityKeyValue(FcmMetrics.Tags.PROJECT_ID, projectId)
                    .parentObservation(parentContext.getObservation())
                    .start();

            return tokenProvider.getAccessToken(parentContext)
                    .flatMap(token -> webClient.post()
                            .uri(sendUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(h -> {
                                h.setBearerAuth(token);
                                if (correlationId != null) {
                                    h.set(FcmCorrelationId.HEADER, correlationId);
                                }
                            })
                            .bodyValue(payload)
                            .exchangeToMono(response -> {
                                String statusClass = response.statusCode().is2xxSuccessful() ? "2xx" : "5xx";
                                obs.lowCardinalityKeyValue(FcmMetrics.Tags.HTTP_STATUS_CLASS, statusClass);
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(FcmSendSuccessResponse.class)
                                            .map(FcmSenderResponse::success);
                                } else {
                                    return response.bodyToMono(FcmSendErrorResponse.class)
                                            .map(FcmSenderResponse::error);
                                }
                            })
                            .onErrorMap(WebClientRequestException.class, ex -> new FcmTransportException("Transport error while calling FCM", ex))
                    )
                    .doOnSuccess(r -> obs.lowCardinalityKeyValue(FcmMetrics.Tags.OUTCOME, r.success() ? "success" : "failure"))
                    .doOnError(obs::error)
                    .doFinally(signalType -> obs.stop());
        });
    }
}
