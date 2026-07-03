/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link OpenWorkflowChatTabToolCallback}. The tool is signaling-only — its server-side behaviour is to
 * echo the {@code threadId} back so the client subscriber can switch the active task. Tests pin the echo contract and
 * the input validation, both of which the client subscriber depends on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenWorkflowChatTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        OpenWorkflowChatTabToolCallback callback = new OpenWorkflowChatTabToolCallback();

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openWorkflowChatTab");
        assertThat(definition.inputSchema()).contains("threadId");
    }

    @Test
    void testRejectsMissingThreadId() throws Exception {
        OpenWorkflowChatTabToolCallback callback = new OpenWorkflowChatTabToolCallback();

        JsonNode node = jsonMapper.readTree(callback.call("{}"));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("threadId");
    }

    @Test
    void testRejectsBlankThreadId() throws Exception {
        OpenWorkflowChatTabToolCallback callback = new OpenWorkflowChatTabToolCallback();

        JsonNode node = jsonMapper.readTree(callback.call("{\"threadId\":\"   \"}"));

        // Blank threadId would route the client to no task at all — fail closed rather than silently
        // open the empty AI Hub root.
        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testEchoesThreadIdAndTitle() throws Exception {
        OpenWorkflowChatTabToolCallback callback = new OpenWorkflowChatTabToolCallback();

        JsonNode node = jsonMapper.readTree(
            callback.call("{\"threadId\":\"00000000-0000-0000-0000-00000000abc3\",\"title\":\"Support bot\"}"));

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("threadId")
            .asText()).isEqualTo("00000000-0000-0000-0000-00000000abc3");
        assertThat(node.get("title")
            .asText()).isEqualTo("Support bot");
    }

    @Test
    void testTitleOptional() throws Exception {
        OpenWorkflowChatTabToolCallback callback = new OpenWorkflowChatTabToolCallback();

        JsonNode node = jsonMapper.readTree(callback.call("{\"threadId\":\"00000000-0000-0000-0000-00000000abc3\"}"));

        // Title is optional — a freshly-created chat with no rename yet still routes correctly.
        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("threadId")
            .asText()).isEqualTo("00000000-0000-0000-0000-00000000abc3");
    }
}
