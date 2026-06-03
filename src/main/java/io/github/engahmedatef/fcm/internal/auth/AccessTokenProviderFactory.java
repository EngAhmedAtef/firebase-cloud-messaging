package io.github.engahmedatef.fcm.internal.auth;

import io.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import io.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import io.github.engahmedatef.fcm.spi.FcmAccessTokenProvider;
import com.google.auth.oauth2.GoogleCredentials;

/** Factory that creates {@link io.github.engahmedatef.fcm.spi.FcmAccessTokenProvider} instances. */
public class AccessTokenProviderFactory {

    public FcmAccessTokenProvider create(GoogleCredentials credentials, String scope, FcmMetricsRecorder metricsRecorder, FcmProperties.JWT jwtConfig) {
        return new GoogleCredentialsAccessTokenProvider(credentials, scope, metricsRecorder, jwtConfig);
    }
}
