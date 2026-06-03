package com.github.engahmedatef.fcm.autoconfigure;

public class FcmConfigurationValidator {

    public void validate(FcmProperties properties) {
        validateAuth(properties);
        validateHttp(properties);
    }

    private void validateAuth(FcmProperties properties) {
        FcmProperties.Credentials credentials = properties.getAuth().getCredentials();
        if (credentials.getType() == FcmCredentialType.SERVICE_ACCOUNT_JSON && credentials.getJsonPath() == null)
            throw new IllegalArgumentException("auth.credentials.json-path must be set when credential type is SERVICE_ACCOUNT_JSON");
    }

    private void validateHttp(FcmProperties properties) {
        FcmProperties.Http http = properties.getHttp();
        if (http.getMaxInMemorySizeMb() > 100)
            throw new IllegalArgumentException("http.max-in-memory-size-mb cannot exceed 100 MB to prevent excessive memory usage");
    }
}
