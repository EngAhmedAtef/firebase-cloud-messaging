package com.github.engahmedatef.fcm.autoconfigure;

/**
 * Selects the credential strategy used by the SDK to obtain an OAuth 2.0 access token
 * for the FCM HTTP v1 API.
 *
 * <p>Set via {@code com.github.engahmedatef.fcm.auth.credentials.type}.
 */
public enum FcmCredentialType {
    /**
     * Reads a Google service-account JSON key file from the path configured by
     * {@code com.github.engahmedatef.fcm.auth.credentials.json-path}.
     * The SDK self-signs a JWT and exchanges it for a short-lived access token,
     * refreshing proactively based on {@code auth.jwt.refresh-buffer-seconds}.
     */
    SERVICE_ACCOUNT_JSON,
    /**
     * Uses the Google Application Default Credentials (ADC) chain:
     * {@code GOOGLE_APPLICATION_CREDENTIALS} environment variable →
     * GCE/GKE metadata server → gcloud CLI credentials.
     * Recommended for deployments on Google Cloud infrastructure.
     */
    APPLICATION_DEFAULT
}
