/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

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
class ListAvailableSourceComponentsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        ListAvailableSourceComponentsToolCallback callback = new ListAvailableSourceComponentsToolCallback();

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(ListAvailableSourceComponentsToolCallback.TOOL_NAME);
        assertThat(definition.description()).isNotBlank();
    }

    @Test
    void testCallReturnsNonEmptyCatalog() throws Exception {
        ListAvailableSourceComponentsToolCallback callback = new ListAvailableSourceComponentsToolCallback();

        String result = callback.call("{}");

        JsonNode array = jsonMapper.readTree(result);

        assertThat(array.isArray()).isTrue();
        assertThat(array)
            .as("MVP catalog should include at least the seed components (airtable, csvFile, jsonFile)")
            .isNotEmpty();
    }

    @Test
    void testCallExposesReadersForEachComponent() throws Exception {
        ListAvailableSourceComponentsToolCallback callback = new ListAvailableSourceComponentsToolCallback();

        String result = callback.call("{}");

        JsonNode array = jsonMapper.readTree(result);

        for (JsonNode component : array) {
            assertThat(component.has("componentName")).isTrue();
            assertThat(component.has("componentVersion")).isTrue();
            assertThat(component.has("availableReaders")).isTrue();
            assertThat(component.get("availableReaders")
                .isArray()).isTrue();
            assertThat(component.get("availableReaders")).isNotEmpty();
        }
    }
}
