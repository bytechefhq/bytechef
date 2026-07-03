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
class OpenDataTableTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenDataTableTabName() {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback();

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openDataTableTab");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("dataTableId");
        assertThat(definition.inputSchema()).contains("name");
    }

    @Test
    void testCallEchoesArgumentsAsOpenedPayload() throws Exception {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback();

        String result = callback.call("{\"dataTableId\":\"42\",\"name\":\"Customer Records\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("dataTableId")
            .asText()).isEqualTo("42");
        assertThat(node.get("name")
            .asText()).isEqualTo("Customer Records");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback();

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenDataTableIdMissing() throws Exception {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback();

        String result = callback.call("{\"name\":\"Customer Records\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
