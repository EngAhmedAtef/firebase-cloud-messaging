package io.github.engahmedatef.fcm.unit.auth;

import io.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import io.github.engahmedatef.fcm.internal.auth.AccessTokenProviderFactory;
import io.github.engahmedatef.fcm.internal.auth.GoogleCredentialsAccessTokenProvider;
import io.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessTokenProviderFactoryTest {

    @Test
    void createReturnsGoogleCredentialsAccessTokenProvider() {
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        when(credentials.createScoped(any(String.class))).thenReturn(credentials);

        var provider = new AccessTokenProviderFactory().create(
                credentials,
                "https://www.googleapis.com/auth/firebase.messaging",
                new NoOpFcmMetricsRecorder(),
                new FcmProperties.JWT()
        );

        assertThat(provider).isInstanceOf(GoogleCredentialsAccessTokenProvider.class);
    }
}
