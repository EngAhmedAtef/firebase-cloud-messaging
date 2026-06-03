package com.github.engahmedatef.fcm.unit.auth;

import com.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import com.github.engahmedatef.fcm.exception.FcmTransportException;
import com.github.engahmedatef.fcm.internal.auth.GoogleCredentialsAccessTokenProvider;
import com.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCredentialsAccessTokenProviderTest {

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String TEST_TOKEN = "ya29.test-access-token";

    private static FcmExecutionContext testContext() {
        return new FcmExecutionContext("test-correlation", "test-project", "device", Observation.NOOP);
    }

    private static FcmProperties.JWT defaultJwtConfig() {
        return new FcmProperties.JWT();
    }

    @Test
    void returnsAccessTokenFromCredentials() throws IOException {
        GoogleCredentials credentials = buildFreshMockCredentials(TEST_TOKEN);

        GoogleCredentialsAccessTokenProvider provider =
                new GoogleCredentialsAccessTokenProvider(credentials, SCOPE, new NoOpFcmMetricsRecorder(), defaultJwtConfig());

        StepVerifier.create(provider.getAccessToken(testContext()))
                .expectNext(TEST_TOKEN)
                .verifyComplete();
    }

    @Test
    void callsRefreshIfExpiredWhenTokenIsAboutToExpire() throws IOException {
        GoogleCredentials credentials = buildExpiredMockCredentials(TEST_TOKEN);

        GoogleCredentialsAccessTokenProvider provider =
                new GoogleCredentialsAccessTokenProvider(credentials, SCOPE, new NoOpFcmMetricsRecorder(), defaultJwtConfig());

        StepVerifier.create(provider.getAccessToken(testContext()))
                .expectNext(TEST_TOKEN)
                .verifyComplete();

        verify(credentials).refreshIfExpired();
    }

    @Test
    void usesCreateScopedCredentials() throws IOException {
        GoogleCredentials base = mock(GoogleCredentials.class);
        GoogleCredentials scoped = mock(GoogleCredentials.class);
        AccessToken token = new AccessToken(TEST_TOKEN, Date.from(Instant.now().plusSeconds(3600)));

        when(base.createScoped(SCOPE)).thenReturn(scoped);
        when(scoped.getAccessToken()).thenReturn(token);

        GoogleCredentialsAccessTokenProvider provider =
                new GoogleCredentialsAccessTokenProvider(base, SCOPE, new NoOpFcmMetricsRecorder(), defaultJwtConfig());

        StepVerifier.create(provider.getAccessToken(testContext()))
                .expectNext(TEST_TOKEN)
                .verifyComplete();

        verify(base).createScoped(SCOPE);
        verify(scoped, never()).refreshIfExpired();
    }

    @Test
    void propagatesIoExceptionFromRefreshAsError() throws IOException {
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        when(credentials.createScoped(any(String.class))).thenReturn(credentials);
        doThrow(new IOException("Network error")).when(credentials).refreshIfExpired();

        GoogleCredentialsAccessTokenProvider provider =
                new GoogleCredentialsAccessTokenProvider(credentials, SCOPE, new NoOpFcmMetricsRecorder(), defaultJwtConfig());

        StepVerifier.create(provider.getAccessToken(testContext()))
                .expectError(IOException.class)
                .verify();
    }

    @Test
    void propagatesTransportExceptionWhenAccessTokenIsNull() throws IOException {
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        when(credentials.createScoped(any(String.class))).thenReturn(credentials);
        when(credentials.getAccessToken()).thenReturn(null);

        GoogleCredentialsAccessTokenProvider provider =
                new GoogleCredentialsAccessTokenProvider(credentials, SCOPE, new NoOpFcmMetricsRecorder(), defaultJwtConfig());

        StepVerifier.create(provider.getAccessToken(testContext()))
                .expectError(FcmTransportException.class)
                .verify();
    }

    // --- Helpers ---

    /** Token valid for 3600 s — no proactive refresh expected. */
    private GoogleCredentials buildFreshMockCredentials(String tokenValue) throws IOException {
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        AccessToken token = new AccessToken(tokenValue, Date.from(Instant.now().plusSeconds(3600)));
        when(credentials.createScoped(any(String.class))).thenReturn(credentials);
        when(credentials.getAccessToken()).thenReturn(token);
        return credentials;
    }

    /** Token already expired — shouldRefresh will be true and refreshIfExpired() will be called. */
    private GoogleCredentials buildExpiredMockCredentials(String tokenValue) throws IOException {
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        AccessToken token = new AccessToken(tokenValue, Date.from(Instant.now().minusSeconds(1)));
        when(credentials.createScoped(any(String.class))).thenReturn(credentials);
        when(credentials.getAccessToken()).thenReturn(token);
        return credentials;
    }
}
