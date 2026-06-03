package com.github.engahmedatef.fcm.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.engahmedatef.fcm.internal.transport.FcmSendSuccessResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmSendSuccessResponseDeserializationTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void parsesSuccessResponseWithMessageName() throws Exception {
        String json = """
                {
                  "name": "projects/my-project/messages/0:1620000000000000%abc123"
                }
                """;

        FcmSendSuccessResponse response = objectMapper.readValue(json, FcmSendSuccessResponse.class);
        assertThat(response.name()).isEqualTo("projects/my-project/messages/0:1620000000000000%abc123");
    }
}
