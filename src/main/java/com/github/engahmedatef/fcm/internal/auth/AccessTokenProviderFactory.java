package com.github.engahmedatef.fcm.internal.auth;

import com.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import com.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import com.github.engahmedatef.fcm.spi.FcmAccessTokenProvider;
import com.google.auth.oauth2.GoogleCredentials;

public class AccessTokenProviderFactory {

    public FcmAccessTokenProvider create(GoogleCredentials credentials, String scope, FcmMetricsRecorder metricsRecorder, FcmProperties.JWT jwtConfig) {
        return new GoogleCredentialsAccessTokenProvider(credentials, scope, metricsRecorder, jwtConfig);
    }
}
