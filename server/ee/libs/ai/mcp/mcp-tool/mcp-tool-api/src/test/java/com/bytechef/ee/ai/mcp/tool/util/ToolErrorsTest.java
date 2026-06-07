/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.mcp.tool.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolErrorsTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolErrorReturnsJsonObjectWithErrorField() throws Exception {
        String result = ToolErrors.toolError(jsonMapper, "something went wrong");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("something went wrong");
    }

    @Test
    void testToolErrorReturnsConstantFallbackWhenSerializationFails() throws Exception {
        JsonMapper failing = mock(JsonMapper.class);

        when(failing.writeValueAsString(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new JacksonException("boom") {
                private static final long serialVersionUID = 1L;
            });

        String result = ToolErrors.toolError(failing, "any message");

        assertThat(result).isEqualTo("{\"error\":\"serialization failure\"}");
    }

    @Test
    void testRuntimeFailureReturnsTypedErrorWithToolNameAndExceptionClass() throws Exception {
        IllegalStateException exception = new IllegalStateException(
            "JDBC URL=jdbc:postgresql://prod-db/secret near table ai_hub_memory");

        String result = ToolErrors.runtimeFailure(
            jsonMapper, ToolErrorsTest.class, "doSomething", exception);

        JsonNode node = jsonMapper.readTree(result);
        String message = node.get("error")
            .asText();

        assertThat(message).contains("doSomething");
        assertThat(message).contains("IllegalStateException");
        // Pinning the hardening contract: the LLM/chat-transcript-visible payload must NEVER include the
        // exception's getMessage(). Underlying messages from JDBC, Spring AI, and HttpClient routinely contain
        // JDBC URLs, table/column names, and stack-trace fragments; surfacing them to the chat would leak
        // internals to end users. A regression that re-adds exception.getMessage() flips this assertion.
        assertThat(message)
            .as("runtime-failure payload must not leak the exception's getMessage() text")
            .doesNotContain("jdbc:postgresql")
            .doesNotContain("ai_hub_memory")
            .doesNotContain("secret");
    }

    @Test
    void testRuntimeFailureFollowsExpectedShape() throws Exception {
        RuntimeException exception = new RuntimeException("oops");

        String result = ToolErrors.runtimeFailure(jsonMapper, ToolErrorsTest.class, "myTool", exception);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error"))
            .as("response must always have an 'error' field for the LLM to parse")
            .isTrue();
        assertThat(node.size())
            .as("response must contain only the 'error' field")
            .isEqualTo(1);
    }
}
