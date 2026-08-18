/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DeleteContextStoreSourceToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        DeleteContextStoreSourceToolCallback callback =
            new DeleteContextStoreSourceToolCallback(mock(ContextStoreSourceFacade.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(DeleteContextStoreSourceToolCallback.TOOL_NAME);
        assertThat(definition.inputSchema()).contains("id");
    }

    @Test
    void testCallDelegatesToFacade() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        DeleteContextStoreSourceToolCallback callback = new DeleteContextStoreSourceToolCallback(facade);

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("deleted")
            .asBoolean()).isTrue();
        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);

        verify(facade).deleteContextStoreSource(42L);
    }

    @Test
    void testCallSurfacesIllegalStateAsToolError() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        doThrow(new IllegalStateException("ContextStoreSource 42 has no owning workspace")).when(facade)
            .deleteContextStoreSource(42L);

        DeleteContextStoreSourceToolCallback callback = new DeleteContextStoreSourceToolCallback(facade);

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("has no owning workspace");
    }

    @Test
    void testCallRequiresId() throws Exception {
        DeleteContextStoreSourceToolCallback callback =
            new DeleteContextStoreSourceToolCallback(mock(ContextStoreSourceFacade.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
