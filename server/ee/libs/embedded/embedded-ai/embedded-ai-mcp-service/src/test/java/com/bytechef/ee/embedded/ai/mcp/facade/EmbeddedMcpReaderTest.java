/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpReaderTest {

    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);

    private EmbeddedMcpReader embeddedMcpReader;

    @BeforeEach
    void beforeEach() {
        embeddedMcpReader = new EmbeddedMcpReader(mcpComponentService, mcpServerService, mcpToolService);

        stubChain(1L, 2L, 3L, PlatformType.EMBEDDED);
        stubChain(11L, 12L, 13L, PlatformType.AUTOMATION);
    }

    @Test
    void testAnEmbeddedServerAndToolAreReturned() {
        assertThat(embeddedMcpReader.getMcpServer(1L)).isNotNull();
        assertThat(embeddedMcpReader.fetchMcpTool(3L)).isPresent();
    }

    @Test
    void testAnAutomationServerAndToolAreNotReturned() {
        assertThatThrownBy(() -> embeddedMcpReader.getMcpServer(11L)).isInstanceOf(IllegalArgumentException.class);

        assertThat(embeddedMcpReader.fetchMcpTool(13L)).isEmpty();
    }

    private void stubChain(long serverId, long componentId, long toolId, PlatformType type) {
        McpServer mcpServer = new McpServer("server", type, Environment.DEVELOPMENT);

        mcpServer.setId(serverId);

        McpTool mcpTool = new McpTool("tool", Map.of(), componentId);

        mcpTool.setId(toolId);

        when(mcpServerService.fetchMcpServer(serverId)).thenReturn(Optional.of(mcpServer));
        when(mcpComponentService.fetchMcpComponentMcpServerId(componentId)).thenReturn(Optional.of(serverId));
        when(mcpToolService.fetchMcpToolMcpComponentId(toolId)).thenReturn(Optional.of(componentId));
        when(mcpToolService.getMcpComponentMcpTools(componentId)).thenReturn(List.of(mcpTool));
    }
}
