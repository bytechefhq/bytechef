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
class RefreshContextStoreSourceToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        RefreshContextStoreSourceToolCallback callback = new RefreshContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(RefreshContextStoreSourceToolCallback.TOOL_NAME);
    }

    @Test
    void testCallReturnsJobId() throws Exception {
        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);
        WorkspaceContextStoreSourceService service = mock(WorkspaceContextStoreSourceService.class);

        when(service.fetchWorkspaceIdByContextStoreSourceId(42L)).thenReturn(Optional.of(7L));
        when(facade.refreshNow(7L, 42L)).thenReturn(12345L);

        RefreshContextStoreSourceToolCallback callback = new RefreshContextStoreSourceToolCallback(facade, service);

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("jobId")
            .asLong()).isEqualTo(12345L);
    }

    @Test
    void testCallRequiresId() throws Exception {
        RefreshContextStoreSourceToolCallback callback = new RefreshContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallSurfacesIllegalStateAsToolError() throws Exception {
        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);
        WorkspaceContextStoreSourceService service = mock(WorkspaceContextStoreSourceService.class);

        when(service.fetchWorkspaceIdByContextStoreSourceId(42L)).thenReturn(Optional.of(7L));
        when(facade.refreshNow(7L, 42L)).thenThrow(new IllegalStateException("source has no workflow"));

        RefreshContextStoreSourceToolCallback callback = new RefreshContextStoreSourceToolCallback(facade, service);

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("source has no workflow");
    }
}
