package com.github.engahmedatef.fcm.autoconfigure;

import com.github.engahmedatef.fcm.api.FcmClient;
import com.github.engahmedatef.fcm.api.ReactiveFcmClient;
import com.github.engahmedatef.fcm.internal.service.sender.FcmMessageSender;
import com.github.engahmedatef.fcm.spi.*;
import com.github.engahmedatef.fcm.support.FakeAccessTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FcmAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FcmAutoConfiguration.class);

    @Test
    void doesNotActivateWithoutProjectId() {
        contextRunner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ReactiveFcmClient.class);
            assertThat(ctx).doesNotHaveBean(FcmClient.class);
        });
    }

    @Test
    void wiresFullBeanGraphWhenProjectIdSet() {
        contextRunner
                .withPropertyValues("com.github.engahmedatef.fcm.project-id=test-project")
                .withBean(FcmAccessTokenProvider.class, FakeAccessTokenProvider::new)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ReactiveFcmClient.class);
                    assertThat(ctx).hasSingleBean(FcmClient.class);
                    assertThat(ctx).hasSingleBean(FcmMessageSender.class);
                    assertThat(ctx).hasSingleBean(FcmRetryPolicy.class);
                    assertThat(ctx).hasSingleBean(FcmPayloadBuilder.class);
                    assertThat(ctx).hasSingleBean(FcmResponseErrorHandler.class);
                    assertThat(ctx).hasSingleBean(FcmLogger.class);
                    assertThat(ctx).hasSingleBean(FcmConfigurationValidator.class);
                });
    }

    @Test
    void customMaxRetriesBindsToRetryPolicy() {
        contextRunner
                .withPropertyValues(
                        "com.github.engahmedatef.fcm.project-id=test-project",
                        "com.github.engahmedatef.fcm.send.max-retries=7"
                )
                .withBean(FcmAccessTokenProvider.class, FakeAccessTokenProvider::new)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(FcmRetryPolicy.class);
                });
    }

    // --- Validator tests: tested directly since ApplicationContextRunner doesn't run ApplicationRunner beans ---

    @Test
    void validatorFailsWhenServiceAccountJsonPathMissing() {
        FcmProperties properties = buildProperties();
        properties.getAuth().getCredentials().setType(FcmCredentialType.SERVICE_ACCOUNT_JSON);
        properties.getAuth().getCredentials().setJsonPath(null);

        FcmConfigurationValidator validator = new FcmConfigurationValidator();
        assertThatThrownBy(() -> validator.validate(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("json-path");
    }

    @Test
    void validatorFailsWhenMaxInMemorySizeExceeds100() {
        FcmProperties properties = buildProperties();
        properties.getHttp().setMaxInMemorySizeMb(101);

        FcmConfigurationValidator validator = new FcmConfigurationValidator();
        assertThatThrownBy(() -> validator.validate(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-in-memory-size-mb");
    }

    @Test
    void validatorPassesWithValidProperties() {
        FcmProperties properties = buildProperties();
        FcmConfigurationValidator validator = new FcmConfigurationValidator();
        // Should not throw
        validator.validate(properties);
    }

    private FcmProperties buildProperties() {
        FcmProperties properties = new FcmProperties();
        properties.setProjectId("test-project");
        properties.getAuth().getCredentials().setType(FcmCredentialType.APPLICATION_DEFAULT);
        properties.getAuth().getJwt().setRefreshBufferSeconds(60);
        properties.getHttp().setMaxInMemorySizeMb(10);
        return properties;
    }
}
