/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SetContextStoreSourceEnabledToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        SetContextStoreSourceEnabledToolCallback callback =
            new SetContextStoreSourceEnabledToolCallback(mock(ContextStoreSourceFacade.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(SetContextStoreSourceEnabledToolCallback.TOOL_NAME);
        assertThat(definition.inputSchema()).contains("enabled");
    }

    @Test
    void testCallEnablesSource() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        when(facade.setContextStoreSourceEnabled(42L, true)).thenReturn(contextStoreSource(true));

        SetContextStoreSourceEnabledToolCallback callback = new SetContextStoreSourceEnabledToolCallback(facade);

        String result = callback.call("{\"id\":42,\"enabled\":true}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("enabled")
            .asBoolean()).isTrue();

        verify(facade).setContextStoreSourceEnabled(42L, true);
    }

    @Test
    void testCallDisablesSource() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        when(facade.setContextStoreSourceEnabled(42L, false)).thenReturn(contextStoreSource(false));

        SetContextStoreSourceEnabledToolCallback callback = new SetContextStoreSourceEnabledToolCallback(facade);

        String result = callback.call("{\"id\":42,\"enabled\":false}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("enabled")
            .asBoolean()).isFalse();

        verify(facade).setContextStoreSourceEnabled(42L, false);
    }

    @Test
    void testCallSurfacesIllegalStateAsToolError() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        when(facade.setContextStoreSourceEnabled(42L, true))
            .thenThrow(new IllegalStateException("ContextStoreSource 42 has no owning workspace"));

        SetContextStoreSourceEnabledToolCallback callback = new SetContextStoreSourceEnabledToolCallback(facade);

        String result = callback.call("{\"id\":42,\"enabled\":true}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("has no owning workspace");
    }

    @Test
    void testCallRequiresEnabled() throws Exception {
        SetContextStoreSourceEnabledToolCallback callback =
            new SetContextStoreSourceEnabledToolCallback(mock(ContextStoreSourceFacade.class));

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    private static ContextStoreSource contextStoreSource(boolean enabled) {
        ContextStoreSource contextStoreSource = new ContextStoreSource();

        contextStoreSource.setId(42L);
        contextStoreSource.setEnabled(enabled);

        return contextStoreSource;
    }
}
