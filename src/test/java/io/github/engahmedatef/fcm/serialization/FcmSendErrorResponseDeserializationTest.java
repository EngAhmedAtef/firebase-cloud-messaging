package io.github.engahmedatef.fcm.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import io.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse.FcmErrorCodes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmSendErrorResponseDeserializationTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void parsesStandardFcmErrorWithErrorCode() throws Exception {
        String json = """
                {
                  "error": {
                    "code": 404,
                    "message": "Requested entity was not found.",
                    "status": "NOT_FOUND",
                    "details": [
                      {
                        "@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError",
                        "errorCode": "UNREGISTERED"
                      }
                    ]
                  }
                }
                """;

        FcmSendErrorResponse response = objectMapper.readValue(json, FcmSendErrorResponse.class);

        assertThat(response.getError().getCode()).isEqualTo(404);
        assertThat(response.getError().getMessage()).isEqualTo("Requested entity was not found.");
        assertThat(response.getError().getFcmErrorCode()).contains(FcmErrorCodes.UNREGISTERED);
    }

    @Test
    void parsesErrorWithMissingDetails() throws Exception {
        String json = """
                {
                  "error": {
                    "code": 500,
                    "message": "Internal error",
                    "status": "INTERNAL"
                  }
                }
                """;

        FcmSendErrorResponse response = objectMapper.readValue(json, FcmSendErrorResponse.class);
        assertThat(response.getError().getFcmErrorCode()).isEmpty();
    }

    @Test
    void parsesErrorWithEmptyDetails() throws Exception {
        String json = """
                {
                  "error": {
                    "code": 400,
                    "message": "Bad request",
                    "status": "INVALID_ARGUMENT",
                    "details": []
                  }
                }
                """;

        FcmSendErrorResponse response = objectMapper.readValue(json, FcmSendErrorResponse.class);
        assertThat(response.getError().getFcmErrorCode()).isEmpty();
    }

    @Test
    void parsesErrorWithUnrecognizedErrorCode() throws Exception {
        String json = """
                {
                  "error": {
                    "code": 400,
                    "message": "Unknown",
                    "details": [
                      { "errorCode": "FUTURE_UNKNOWN_CODE" }
                    ]
                  }
                }
                """;

        FcmSendErrorResponse response = objectMapper.readValue(json, FcmSendErrorResponse.class);
        assertThat(response.getError().getFcmErrorCode()).isEmpty();
    }

    @Test
    void parsesAllSupportedErrorCodes() throws Exception {
        for (FcmErrorCodes code : FcmErrorCodes.values()) {
            String json = String.format("""
                    {
                      "error": {
                        "code": 400,
                        "message": "Error",
                        "details": [
                          { "errorCode": "%s" }
                        ]
                      }
                    }
                    """, code.name());

            FcmSendErrorResponse response = objectMapper.readValue(json, FcmSendErrorResponse.class);
            assertThat(response.getError().getFcmErrorCode())
                    .as("Expected code %s to be parsed", code)
                    .contains(code);
        }
    }
}
