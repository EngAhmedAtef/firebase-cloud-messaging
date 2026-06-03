package io.github.engahmedatef.fcm.unit.auth;

import io.github.engahmedatef.fcm.autoconfigure.FcmCredentialType;
import io.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import io.github.engahmedatef.fcm.exception.FcmResourceNotFoundException;
import io.github.engahmedatef.fcm.internal.auth.CredentialResolver;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialResolverTest {

    @TempDir
    Path tempDir;

    private static String fakeSaJson;

    @BeforeAll
    static void generateFakeSaJson() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        String base64Key = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(pair.getPrivate().getEncoded());
        String pemKey = "-----BEGIN PRIVATE KEY-----\n" + base64Key + "\n-----END PRIVATE KEY-----\n";
        String pemInJson = pemKey.replace("\n", "\\n");

        fakeSaJson = """
                {
                  "type": "service_account",
                  "project_id": "test-project",
                  "private_key_id": "key-001",
                  "private_key": "%s",
                  "client_email": "test@test-project.iam.gserviceaccount.com",
                  "client_id": "123456789",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(pemInJson);
    }

    private FcmProperties.Credentials credentials(FcmCredentialType type, String path) {
        FcmProperties.Credentials creds = new FcmProperties.Credentials();
        creds.setType(type);
        creds.setJsonPath(path);
        return creds;
    }

    @Test
    void nullPathThrowsIllegalArgument() {
        CredentialResolver resolver = new CredentialResolver(new DefaultResourceLoader());
        assertThatThrownBy(() -> resolver.resolve(credentials(FcmCredentialType.SERVICE_ACCOUNT_JSON, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("json path");
    }

    @Test
    void blankPathThrowsIllegalArgument() {
        CredentialResolver resolver = new CredentialResolver(new DefaultResourceLoader());
        assertThatThrownBy(() -> resolver.resolve(credentials(FcmCredentialType.SERVICE_ACCOUNT_JSON, "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("json path");
    }

    @Test
    void classpathResourceNotFoundThrowsResourceNotFoundException() {
        CredentialResolver resolver = new CredentialResolver(new DefaultResourceLoader());
        assertThatThrownBy(() -> resolver.resolve(credentials(FcmCredentialType.SERVICE_ACCOUNT_JSON, "classpath:/nonexistent-sa.json")))
                .isInstanceOf(FcmResourceNotFoundException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void validFilesystemPathLoadsServiceAccountCredentials() throws Exception {
        Path saFile = tempDir.resolve("sa.json");
        Files.writeString(saFile, fakeSaJson);

        CredentialResolver resolver = new CredentialResolver(new DefaultResourceLoader());
        var result = resolver.resolve(credentials(FcmCredentialType.SERVICE_ACCOUNT_JSON, saFile.toAbsolutePath().toString()));

        assertThat(result).isInstanceOf(ServiceAccountCredentials.class);
        assertThat(((ServiceAccountCredentials) result).getClientEmail())
                .isEqualTo("test@test-project.iam.gserviceaccount.com");
    }

    @Test
    void nonExistentFilesystemPathThrowsResourceNotFoundException() {
        CredentialResolver resolver = new CredentialResolver(new DefaultResourceLoader());
        assertThatThrownBy(() -> resolver.resolve(credentials(FcmCredentialType.SERVICE_ACCOUNT_JSON, "/nonexistent/path/sa.json")))
                .isInstanceOf(FcmResourceNotFoundException.class);
    }
}
