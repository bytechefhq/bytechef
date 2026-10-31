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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
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

    private final McpComponentRepository mcpComponentRepository = mock(McpComponentRepository.class);
    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);
    private final McpToolRepository mcpToolRepository = mock(McpToolRepository.class);

    private EmbeddedMcpReader embeddedMcpReader;

    @BeforeEach
    void beforeEach() {
        embeddedMcpReader = new EmbeddedMcpReader(mcpComponentRepository, mcpServerRepository, mcpToolRepository);

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

        McpComponent mcpComponent = new McpComponent("component", 1, serverId, null);

        mcpComponent.setId(componentId);

        McpTool mcpTool = new McpTool("tool", Map.of(), componentId);

        mcpTool.setId(toolId);

        when(mcpServerRepository.findById(serverId)).thenReturn(Optional.of(mcpServer));
        when(mcpComponentRepository.findById(componentId)).thenReturn(Optional.of(mcpComponent));
        when(mcpToolRepository.findById(toolId)).thenReturn(Optional.of(mcpTool));
    }
}
