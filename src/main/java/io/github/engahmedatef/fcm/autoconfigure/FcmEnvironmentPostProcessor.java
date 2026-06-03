package io.github.engahmedatef.fcm.autoconfigure;

import io.github.engahmedatef.fcm.exception.FcmConfigurationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Objects;

public class FcmEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String projectId = environment.getProperty("io.github.engahmedatef.fcm.project-id");
        if (projectId == null || projectId.isBlank())
            throw new FcmConfigurationException("io.github.engahmedatef.fcm.project-id is required");

        String type = environment.getProperty("io.github.engahmedatef.fcm.auth.credentials.type");
        String path = environment.getProperty("io.github.engahmedatef.fcm.auth.credentials.json-path");
        if (Objects.equals(type, FcmCredentialType.SERVICE_ACCOUNT_JSON.name())) {
            if (path == null || path.isBlank())
                throw new FcmConfigurationException("io.github.engahmedatef.fcm.auth.credentials.path is required when using SERVICE_ACCOUNT_JSON");
        }
    }
}
