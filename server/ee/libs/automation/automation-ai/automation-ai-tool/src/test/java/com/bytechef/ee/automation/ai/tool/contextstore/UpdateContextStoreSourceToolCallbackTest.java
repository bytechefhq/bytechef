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
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
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
        UpdateContextStoreSourceToolCallback callback =
            new UpdateContextStoreSourceToolCallback(mock(ContextStoreSourceFacade.class));

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

        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        when(facade.updateContextStoreSource(eq(42L), any(UpdateContextStoreSourceInput.class))).thenReturn(updated);

        UpdateContextStoreSourceToolCallback callback = new UpdateContextStoreSourceToolCallback(facade);

        String result = callback.call("{\"id\":42,\"name\":\"renamed\",\"cadence\":\"@daily\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("cadence")
            .asText()).isEqualTo("@daily");

        ArgumentCaptor<UpdateContextStoreSourceInput> captor =
            ArgumentCaptor.forClass(UpdateContextStoreSourceInput.class);

        verify(facade).updateContextStoreSource(eq(42L), captor.capture());

        UpdateContextStoreSourceInput captured = captor.getValue();

        assertThat(captured.name()).isEqualTo("renamed");
        assertThat(captured.cadence()).isEqualTo("@daily");
        assertThat(captured.enabled()).isNull();
    }

    @Test
    void testCallSurfacesIllegalStateAsToolError() throws Exception {
        ContextStoreSourceFacade facade = mock(ContextStoreSourceFacade.class);

        when(facade.updateContextStoreSource(eq(42L), any(UpdateContextStoreSourceInput.class)))
            .thenThrow(new IllegalStateException("ContextStoreSource 42 has no owning workspace"));

        UpdateContextStoreSourceToolCallback callback = new UpdateContextStoreSourceToolCallback(facade);

        String result = callback.call("{\"id\":42,\"name\":\"renamed\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("has no owning workspace");
    }

    @Test
    void testCallRequiresId() throws Exception {
        UpdateContextStoreSourceToolCallback callback =
            new UpdateContextStoreSourceToolCallback(mock(ContextStoreSourceFacade.class));

        String result = callback.call("{\"name\":\"x\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("id");
    }
}
