package io.github.engahmedatef.fcm.internal.transport;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class FcmSendErrorResponse {
    private FcmSendError error;

    public enum FcmErrorCodes {
        UNSPECIFIED_ERROR,
        INVALID_ARGUMENT,
        UNREGISTERED,
        SENDER_ID_MISMATCH,
        QUOTA_EXCEEDED,
        UNAVAILABLE,
        INTERNAL,
        THIRD_PARTY_AUTH_ERROR
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FcmSendError {
        private Integer code;
        private String message;
        private String status;
        private JsonNode details;

        public Optional<FcmErrorCodes> getFcmErrorCode() {
            if (details == null || !details.isArray())
                return Optional.empty();

            for (JsonNode node : details) {
                JsonNode errorCodeNode = node.get("errorCode");
                if (errorCodeNode != null) {
                    try {
                        return Optional.of(FcmErrorCodes.valueOf(errorCodeNode.asText()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            return Optional.empty();
        }
    }
}
