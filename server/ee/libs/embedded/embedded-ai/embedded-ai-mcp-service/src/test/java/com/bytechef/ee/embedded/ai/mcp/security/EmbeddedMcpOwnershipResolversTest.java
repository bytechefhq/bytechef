/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpOwnershipResolversTest {

    private final McpComponentRepository mcpComponentRepository = mock(McpComponentRepository.class);
    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);
    private final McpToolRepository mcpToolRepository = mock(McpToolRepository.class);
    private final EmbeddedMcpServerOwnershipResolver serverResolver =
        new EmbeddedMcpServerOwnershipResolver(mcpServerRepository);
    private final EmbeddedMcpComponentOwnershipResolver componentResolver =
        new EmbeddedMcpComponentOwnershipResolver(mcpComponentRepository, mcpServerRepository);
    private final EmbeddedMcpToolOwnershipResolver toolResolver =
        new EmbeddedMcpToolOwnershipResolver(mcpComponentRepository, mcpServerRepository, mcpToolRepository);

    @Test
    void testResourceTypesMatchTheGateTokens() {
        assertThat(serverResolver.resourceType()).isEqualTo("McpServer");
        assertThat(componentResolver.resourceType()).isEqualTo("McpComponent");
        assertThat(toolResolver.resourceType()).isEqualTo("McpTool");
    }

    @Test
    void testResolvesRowsOfAnEmbeddedServer() {
        givenChain(PlatformType.EMBEDDED);

        assertThat(serverResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(serverResolver.resolveOwner(Long.valueOf(1L))).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(componentResolver.resolveOwner(2L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(componentResolver.resolveOwner(Long.valueOf(2L))).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(toolResolver.resolveOwner(3L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(toolResolver.resolveOwner(Long.valueOf(3L))).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testRowsOfAnAutomationServerAreUnknown() {
        givenChain(PlatformType.AUTOMATION);

        assertThat(serverResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.unknown());
        assertThat(componentResolver.resolveOwner(2L)).isEqualTo(ResourceOwner.unknown());
        assertThat(toolResolver.resolveOwner(3L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testMissingRowsAreUnknown() {
        when(mcpServerRepository.findById(1L)).thenReturn(Optional.empty());
        when(mcpComponentRepository.findById(2L)).thenReturn(Optional.empty());
        when(mcpToolRepository.findById(3L)).thenReturn(Optional.empty());

        assertThat(serverResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.unknown());
        assertThat(componentResolver.resolveOwner(2L)).isEqualTo(ResourceOwner.unknown());
        assertThat(toolResolver.resolveOwner(3L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testANonNumericIdIsUnknown() {
        assertThat(serverResolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
        assertThat(componentResolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
        assertThat(toolResolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }

    private void givenChain(PlatformType type) {
        McpServer mcpServer = new McpServer("server", type, Environment.PRODUCTION);
        McpComponent mcpComponent = mock(McpComponent.class);
        McpTool mcpTool = mock(McpTool.class);

        when(mcpComponent.getMcpServerId()).thenReturn(1L);
        when(mcpTool.getMcpComponentId()).thenReturn(2L);
        when(mcpServerRepository.findById(1L)).thenReturn(Optional.of(mcpServer));
        when(mcpComponentRepository.findById(2L)).thenReturn(Optional.of(mcpComponent));
        when(mcpToolRepository.findById(3L)).thenReturn(Optional.of(mcpTool));
    }
}
