package io.github.engahmedatef.fcm.autoconfigure;

import io.github.engahmedatef.fcm.exception.FcmConfigurationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Objects;

public class FcmEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String projectId = environment.getProperty("fcm.project-id");
        if (projectId == null || projectId.isBlank())
            throw new FcmConfigurationException("fcm.project-id is required");

        String type = environment.getProperty("fcm.auth.credentials.type");
        String path = environment.getProperty("fcm.auth.credentials.json-path");
        if (Objects.equals(type, FcmCredentialType.SERVICE_ACCOUNT_JSON.name())) {
            if (path == null || path.isBlank())
                throw new FcmConfigurationException("fcm.auth.credentials.path is required when using SERVICE_ACCOUNT_JSON");
        }
    }
}
