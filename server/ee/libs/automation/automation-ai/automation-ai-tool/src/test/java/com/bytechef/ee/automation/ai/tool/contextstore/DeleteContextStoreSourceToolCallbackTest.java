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
class DeleteContextStoreSourceToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        DeleteContextStoreSourceToolCallback callback = new DeleteContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(DeleteContextStoreSourceToolCallback.TOOL_NAME);
        assertThat(definition.inputSchema()).contains("id");
    }

    @Test
    void testCallDelegatesToFacade() throws Exception {
        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);
        WorkspaceContextStoreSourceService service = mock(WorkspaceContextStoreSourceService.class);

        when(service.fetchWorkspaceIdByContextStoreSourceId(42L)).thenReturn(Optional.of(7L));

        DeleteContextStoreSourceToolCallback callback = new DeleteContextStoreSourceToolCallback(facade, service);

        String result = callback.call("{\"id\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("deleted")
            .asBoolean()).isTrue();
        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);

        verify(facade).delete(7L, 42L);
    }

    @Test
    void testCallRequiresId() throws Exception {
        DeleteContextStoreSourceToolCallback callback = new DeleteContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
