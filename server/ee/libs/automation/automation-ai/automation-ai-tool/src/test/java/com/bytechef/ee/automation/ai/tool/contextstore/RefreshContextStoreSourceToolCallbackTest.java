/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
class RefreshContextStoreSourceToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        RefreshContextStoreSourceToolCallback callback =
            new RefreshContextStoreSourceToolCallback(mock(ContextStoreSourceFacade.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(RefreshContextStoreSourceToolCallback.TOOL_NAME);
    }

    @Test
    void testCallReturnsJobId() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        when(facade.refreshContextStoreSource(42L)).thenReturn(12345L);

        RefreshContextStoreSourceToolCallback callback = new RefreshContextStoreSourceToolCallback(facade);

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("jobId")
            .asLong()).isEqualTo(12345L);
    }

    @Test
    void testCallRequiresId() throws Exception {
        RefreshContextStoreSourceToolCallback callback =
            new RefreshContextStoreSourceToolCallback(mock(ContextStoreSourceFacade.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallSurfacesIllegalStateAsToolError() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        when(facade.refreshContextStoreSource(42L)).thenThrow(new IllegalStateException("source has no workflow"));

        RefreshContextStoreSourceToolCallback callback = new RefreshContextStoreSourceToolCallback(facade);

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("source has no workflow");
    }
}
