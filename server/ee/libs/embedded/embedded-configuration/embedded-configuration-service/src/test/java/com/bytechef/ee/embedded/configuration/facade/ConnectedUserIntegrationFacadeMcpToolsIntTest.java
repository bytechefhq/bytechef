/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ConnectedUserIntegrationFacadeMcpToolsIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ConnectedUserIntegrationFacadeMcpToolsIntTest {

    private static final String COMPONENT_NAME =
        ConnectedUserIntegrationFacadeMcpToolsIntTestConfiguration.COMPONENT_NAME;

    @Autowired
    private ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @AfterEach
    void afterEach() {
        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testGetMcpToolsUsesClusterElementTitleAsLabel() {
        McpTool mcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");

        List<ConnectedUserIntegrationDTO.McpToolInfo> mcpToolInfos =
            connectedUserIntegrationFacade.getMcpTools(COMPONENT_NAME);

        assertThat(mcpToolInfos).containsExactly(
            new ConnectedUserIntegrationDTO.McpToolInfo(mcpTool.getId(), "sendEmail", "Send Email", "Send an email"));
    }

    @Test
    void testGetMcpToolsKeepsNullLabelWhenClusterElementHasNoTitle() {
        McpTool mcpTool = createMcpTool(PlatformType.EMBEDDED, true, "getEmail");

        List<ConnectedUserIntegrationDTO.McpToolInfo> mcpToolInfos =
            connectedUserIntegrationFacade.getMcpTools(COMPONENT_NAME);

        assertThat(mcpToolInfos).containsExactly(
            new ConnectedUserIntegrationDTO.McpToolInfo(mcpTool.getId(), "getEmail", null, "Get an email"));
    }

    @Test
    void testGetMcpToolsSkipsToolsOfDisabledMcpServer() {
        createMcpTool(PlatformType.EMBEDDED, false, "sendEmail");

        assertThat(connectedUserIntegrationFacade.getMcpTools(COMPONENT_NAME)).isEmpty();
    }

    @Test
    void testGetMcpToolsSkipsToolsOfNonEmbeddedMcpServer() {
        createMcpTool(PlatformType.AUTOMATION, true, "sendEmail");

        assertThat(connectedUserIntegrationFacade.getMcpTools(COMPONENT_NAME)).isEmpty();
    }

    @Test
    void testGetMcpToolsSkipsGloballyDisabledTools() {
        McpTool enabledMcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");

        createDisabledMcpTool(enabledMcpTool.getMcpComponentId(), "getEmail");

        assertThat(connectedUserIntegrationFacade.getMcpTools(COMPONENT_NAME))
            .extracting(ConnectedUserIntegrationDTO.McpToolInfo::id)
            .containsExactly(enabledMcpTool.getId());
    }

    @Test
    void testGetMcpInstanceToolsSkipsGloballyDisabledTools() {
        McpTool enabledMcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");
        McpTool disabledMcpTool = createDisabledMcpTool(enabledMcpTool.getMcpComponentId(), "getEmail");

        when(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(1L)).thenReturn(
            List.of(
                new McpIntegrationInstanceTool(1L, enabledMcpTool.getId(), true),
                new McpIntegrationInstanceTool(1L, disabledMcpTool.getId(), true)));

        assertThat(connectedUserIntegrationFacade.getMcpInstanceTools(1L)).containsExactly(
            new ConnectedUserIntegrationDTO.McpInstanceToolInfo(enabledMcpTool.getId(), true));
    }

    private McpTool createDisabledMcpTool(long mcpComponentId, String name) {
        McpTool mcpTool = new McpTool(name, Map.of(), mcpComponentId);

        mcpTool.setEnabled(false);

        return mcpToolRepository.save(mcpTool);
    }

    private McpTool createMcpTool(PlatformType type, boolean enabled, String name) {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("test-server", type, Environment.DEVELOPMENT, enabled));

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent(COMPONENT_NAME, 1, mcpServer.getId(), null));

        return mcpToolRepository.save(new McpTool(name, Map.of(), mcpComponent.getId()));
    }
}
