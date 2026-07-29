/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ClusterElementToolCallbackLazySchemaTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);

    @Test
    void testInputSchemaIsGeneratedLazilyAndCached() {
        ClusterElementDefinition definition = mock(ClusterElementDefinition.class);

        when(definition.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage"))
            .thenReturn(definition);

        ClusterElementToolCallback callback = new ClusterElementToolCallback(
            "slack_sendMessage", "Send a Slack message", "slack", 1, "sendMessage",
            clusterElementDefinitionService, connectionService);

        // Construction must not resolve the component (no schema generation yet).
        verifyNoInteractions(clusterElementDefinitionService);

        // getToolDefinition() itself forces the memoized schema load at the callback level — it is the
        // map-keyed dispatch in MapToolCallbackResolver / seedCatalogToolCallbacks that avoids that call in
        // production by keying off the tool name without ever invoking getToolDefinition().
        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("slack_sendMessage");

        // Accessing the schema twice resolves the component exactly once (memoized).
        assertThat(callback.getToolDefinition()
            .inputSchema()).isNotNull();
        assertThat(callback.getToolDefinition()
            .inputSchema()).isNotNull();

        verify(clusterElementDefinitionService, times(1)).getClusterElementDefinition("slack", 1, "sendMessage");
    }

    @Test
    void testInputSchemaDegradesToEmptyObjectOnMalformedSchema() {
        when(clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage"))
            .thenThrow(new RuntimeException("malformed property tree"));

        ClusterElementToolCallback callback = new ClusterElementToolCallback(
            "slack_sendMessage", "Send a Slack message", "slack", 1, "sendMessage",
            clusterElementDefinitionService, connectionService);

        String expectedSchema = "{\"type\":\"object\",\"properties\":{}}";

        assertThat(callback.getToolDefinition()
            .inputSchema()).isEqualTo(expectedSchema);

        // The failure is memoized too — the callback stays usable and doesn't retry the failing lookup.
        assertThat(callback.getToolDefinition()
            .inputSchema()).isEqualTo(expectedSchema);

        verify(clusterElementDefinitionService, times(1)).getClusterElementDefinition("slack", 1, "sendMessage");
    }
}
