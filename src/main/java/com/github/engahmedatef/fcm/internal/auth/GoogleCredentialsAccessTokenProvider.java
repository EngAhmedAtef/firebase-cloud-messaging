package com.github.engahmedatef.fcm.internal.auth;

import com.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import com.github.engahmedatef.fcm.exception.FcmTransportException;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.spi.FcmAccessTokenProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleCredentialsAccessTokenProvider implements FcmAccessTokenProvider {

    private final GoogleCredentials credentials;
    private final FcmMetricsRecorder metricsRecorder;
    private final FcmProperties.JWT jwtConfig;

    public GoogleCredentialsAccessTokenProvider(GoogleCredentials credentials, String scope, FcmMetricsRecorder metricsRecorder, FcmProperties.JWT jwtConfig) {
        this.credentials = credentials.createScoped(scope);
        this.metricsRecorder = metricsRecorder;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public Mono<String> getAccessToken(FcmExecutionContext context) {

        AtomicBoolean refreshed = new AtomicBoolean(false);
        AtomicReference<Instant> refreshStart = new AtomicReference<>();

        return Mono.fromCallable(() -> {

                    AccessToken token = credentials.getAccessToken();

                    Instant now = Instant.now();
                    Instant expiry = token != null && token.getExpirationTime() != null
                            ? token.getExpirationTime().toInstant()
                            : null;

                    boolean shouldRefresh = (token == null || expiry == null)
                            || now.plusSeconds(jwtConfig.getRefreshBufferSeconds()).isAfter(expiry);

                    if (shouldRefresh) {
                        refreshed.set(true);
                        refreshStart.set(now);

                        credentials.refreshIfExpired();
                    }

                    AccessToken newToken = credentials.getAccessToken();

                    if (newToken == null || newToken.getTokenValue() == null) {
                        throw new FcmTransportException("Failed to obtain Google access token");
                    }

                    if (refreshed.get()) {
                        metricsRecorder.increment(
                                FcmMetrics.TOKEN_REFRESH,
                                context,
                                FcmMetrics.Tags.OUTCOME,
                                "success"
                        );

                        metricsRecorder.recordTimer(
                                FcmMetrics.TOKEN_REFRESH_DURATION,
                                context,
                                Duration.between(refreshStart.get(), Instant.now())
                        );
                    }

                    return newToken.getTokenValue();
                })
                .doOnError(ex -> {
                    if (refreshed.get()) {
                        metricsRecorder.increment(
                                FcmMetrics.TOKEN_REFRESH,
                                context,
                                FcmMetrics.Tags.OUTCOME,
                                "failure"
                        );
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
