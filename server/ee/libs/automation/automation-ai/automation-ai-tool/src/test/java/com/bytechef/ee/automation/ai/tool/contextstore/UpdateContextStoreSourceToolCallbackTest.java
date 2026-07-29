/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.contextstore.dto.UpdateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class UpdateContextStoreSourceToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        UpdateContextStoreSourceToolCallback callback = new UpdateContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(UpdateContextStoreSourceToolCallback.TOOL_NAME);
        assertThat(definition.inputSchema()).contains("cadence");
    }

    @Test
    void testCallDelegatesToFacade() throws Exception {
        ContextStoreSource updated = new ContextStoreSource();

        updated.setId(42L);
        updated.setName("renamed");
        updated.setCadence("@daily");
        updated.setEnabled(true);
        updated.setStatus(ContextStoreSourceStatus.READY);

        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);
        WorkspaceContextStoreSourceService service = mock(WorkspaceContextStoreSourceService.class);

        when(service.fetchWorkspaceIdByContextStoreSourceId(42L)).thenReturn(Optional.of(7L));
        when(facade.update(eq(7L), eq(42L), any(UpdateContextStoreSourceInput.class))).thenReturn(updated);

        UpdateContextStoreSourceToolCallback callback = new UpdateContextStoreSourceToolCallback(facade, service);

        String result = callback.call("{\"id\":42,\"name\":\"renamed\",\"cadence\":\"@daily\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("cadence")
            .asText()).isEqualTo("@daily");

        ArgumentCaptor<UpdateContextStoreSourceInput> captor =
            ArgumentCaptor.forClass(UpdateContextStoreSourceInput.class);

        verify(facade).update(eq(7L), eq(42L), captor.capture());

        UpdateContextStoreSourceInput captured = captor.getValue();

        assertThat(captured.name()).isEqualTo("renamed");
        assertThat(captured.cadence()).isEqualTo("@daily");
        assertThat(captured.enabled()).isNull();
    }

    @Test
    void testCallRequiresId() throws Exception {
        UpdateContextStoreSourceToolCallback callback = new UpdateContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class), mock(WorkspaceContextStoreSourceService.class));

        String result = callback.call("{\"name\":\"x\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("id");
    }
}
