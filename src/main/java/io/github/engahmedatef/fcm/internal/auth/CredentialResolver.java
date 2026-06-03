package io.github.engahmedatef.fcm.internal.auth;

import io.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import io.github.engahmedatef.fcm.exception.FcmResourceNotFoundException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

public class CredentialResolver {

    private final ResourceLoader resourceLoader;

    public CredentialResolver(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public GoogleCredentials resolve(FcmProperties.Credentials credentials) throws IOException {
        return switch (credentials.getType()) {
            case APPLICATION_DEFAULT -> GoogleCredentials.getApplicationDefault();
            case SERVICE_ACCOUNT_JSON -> loadServiceAccount(credentials.getJsonPath());
        };
    }

    private GoogleCredentials loadServiceAccount(String path) throws IOException {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("FCM json path must not be null or blank");

        String lower = path.toLowerCase();
        boolean useResourceLoader = lower.contains("classpath:") || lower.startsWith("file:");
        Resource resource = useResourceLoader ? resourceLoader.getResource(path) : new FileSystemResource(path);
        if (!resource.exists())
            throw new FcmResourceNotFoundException("Resource not found: " + path);

        try (InputStream inputStream = resource.getInputStream()) {
            return ServiceAccountCredentials.fromStream(inputStream);
        }
    }
}
