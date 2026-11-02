/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpAuditIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.audit.McpToolAuditEvents;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.service.TagService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedMcpIntTestConfiguration.class, EmbeddedMcpServerFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
@MockitoBean(types = {
    ComponentDefinitionService.class, IntegrationInstanceConfigurationService.class, IntegrationService.class,
    McpComponentService.class, McpIntegrationComponentService.class, McpIntegrationInstanceToolService.class,
    McpServerFacade.class, McpServerService.class, McpToolService.class, PermissionService.class, TagService.class
})
class EmbeddedMcpServerFacadeAuditIntTest {

    private static final long MCP_COMPONENT_ID = 4L;
    private static final long MCP_SERVER_ID = 2L;
    private static final long MCP_TOOL_ID = 21L;

    @Autowired
    private EmbeddedMcpServerFacade embeddedMcpServerFacade;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolService mcpToolService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testUpdateEmbeddedMcpToolEnabledRecordsTheToolItsComponentAndTheNewState() {
        McpComponent mcpComponent = new McpComponent("gmail", 1, MCP_SERVER_ID, null);

        mcpComponent.setId(MCP_COMPONENT_ID);

        when(mcpToolService.fetchMcpTool(MCP_TOOL_ID))
            .thenReturn(Optional.of(new McpTool(MCP_TOOL_ID, "searchEmail", Map.of(), MCP_COMPONENT_ID)));
        when(mcpComponentService.getMcpComponent(MCP_COMPONENT_ID)).thenReturn(mcpComponent);
        when(mcpServerService.getMcpServer(MCP_SERVER_ID))
            .thenReturn(new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(MCP_TOOL_ID, false);

        verify(mcpToolService).updateEnabled(MCP_TOOL_ID, false);

        assertThat(findSingleEventData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpToolId", String.valueOf(MCP_TOOL_ID))
            .containsEntry("mcpComponentId", String.valueOf(MCP_COMPONENT_ID))
            .containsEntry("name", "searchEmail")
            .containsEntry("enabled", "false");
    }

    @Test
    void testUpdateEmbeddedMcpToolEnabledOfAnUnknownToolRecordsAFailure() {
        when(mcpToolService.fetchMcpTool(MCP_TOOL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(MCP_TOOL_ID, true))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(findSingleEventData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("mcpToolId", String.valueOf(MCP_TOOL_ID))
            .containsEntry("enabled", "true")
            .doesNotContainKeys("mcpComponentId", "name");
    }

    private Map<String, String> findSingleEventData() {
        return EmbeddedMcpAuditIntTestConfiguration.findSingleEventData(
            persistenceAuditEventRepository, McpToolAuditEvents.MCP_TOOL_ENABLED_UPDATED);
    }

    @Configuration
    @Import({
        EmbeddedMcpAuditIntTestConfiguration.class, EmbeddedMcpServerFacadeImpl.class
    })
    static class AuditConfiguration {
    }
}
