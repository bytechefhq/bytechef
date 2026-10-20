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
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpOwnershipResolversTest {

    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);
    private final EmbeddedMcpServerOwnershipResolver serverResolver =
        new EmbeddedMcpServerOwnershipResolver(mcpServerService);
    private final EmbeddedMcpComponentOwnershipResolver componentResolver =
        new EmbeddedMcpComponentOwnershipResolver(mcpComponentService, mcpServerService);
    private final EmbeddedMcpToolOwnershipResolver toolResolver =
        new EmbeddedMcpToolOwnershipResolver(mcpComponentService, mcpServerService, mcpToolService);

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
        when(mcpServerService.fetchMcpServer(1L)).thenReturn(Optional.empty());
        when(mcpComponentService.fetchMcpComponentMcpServerId(2L)).thenReturn(Optional.empty());
        when(mcpToolService.fetchMcpToolMcpComponentId(3L)).thenReturn(Optional.empty());

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

        when(mcpServerService.fetchMcpServer(1L)).thenReturn(Optional.of(mcpServer));
        when(mcpComponentService.fetchMcpComponentMcpServerId(2L)).thenReturn(Optional.of(1L));
        when(mcpToolService.fetchMcpToolMcpComponentId(3L)).thenReturn(Optional.of(2L));
    }
}
