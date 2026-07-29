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

import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import java.util.Optional;
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
        SetContextStoreSourceEnabledToolCallback callback = new SetContextStoreSourceEnabledToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(SetContextStoreSourceEnabledToolCallback.TOOL_NAME);
        assertThat(definition.inputSchema()).contains("enabled");
    }

    @Test
    void testCallEnablesSource() throws Exception {
        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);
        WorkspaceContextStoreSourceService service = mock(WorkspaceContextStoreSourceService.class);

        when(service.fetchWorkspaceIdByContextStoreSourceId(42L)).thenReturn(Optional.of(7L));

        SetContextStoreSourceEnabledToolCallback callback =
            new SetContextStoreSourceEnabledToolCallback(facade, service);

        String result = callback.call("{\"id\":42,\"enabled\":true}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("enabled")
            .asBoolean()).isTrue();

        verify(facade).setEnabled(7L, 42L, true);
    }

    @Test
    void testCallDisablesSource() throws Exception {
        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);
        WorkspaceContextStoreSourceService service = mock(WorkspaceContextStoreSourceService.class);

        when(service.fetchWorkspaceIdByContextStoreSourceId(42L)).thenReturn(Optional.of(7L));

        SetContextStoreSourceEnabledToolCallback callback =
            new SetContextStoreSourceEnabledToolCallback(facade, service);

        String result = callback.call("{\"id\":42,\"enabled\":false}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("enabled")
            .asBoolean()).isFalse();

        verify(facade).setEnabled(7L, 42L, false);
    }

    @Test
    void testCallRequiresEnabled() throws Exception {
        SetContextStoreSourceEnabledToolCallback callback = new SetContextStoreSourceEnabledToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
