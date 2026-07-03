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
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenKnowledgeBaseTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenKnowledgeBaseTabName() {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback();

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openKnowledgeBaseTab");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("knowledgeBaseId");
        assertThat(definition.inputSchema()).contains("name");
    }

    @Test
    void testCallEchoesArgumentsAsOpenedPayload() throws Exception {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback();

        String result = callback.call("{\"knowledgeBaseId\":\"42\",\"name\":\"Product Docs\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("knowledgeBaseId")
            .asText()).isEqualTo("42");
        assertThat(node.get("name")
            .asText()).isEqualTo("Product Docs");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback();

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenKnowledgeBaseIdMissing() throws Exception {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback();

        String result = callback.call("{\"name\":\"Product Docs\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
