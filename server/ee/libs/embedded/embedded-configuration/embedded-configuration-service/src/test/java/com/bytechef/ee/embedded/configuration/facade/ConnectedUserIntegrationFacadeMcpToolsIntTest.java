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
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
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
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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
    private static final long INTEGRATION_ID = 1L;

    @Autowired
    private ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationComponentService mcpIntegrationComponentService;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_integration_component");
        jdbcTemplate.update("DELETE FROM integration");

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testGetMcpToolsUsesClusterElementTitleAsLabel() {
        McpTool mcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");

        List<ConnectedUserIntegrationDTO.McpToolInfo> mcpToolInfos =
            getMcpTools(INTEGRATION_ID);

        assertThat(mcpToolInfos).containsExactly(
            new ConnectedUserIntegrationDTO.McpToolInfo(mcpTool.getId(), "sendEmail", "Send Email", "Send an email"));
    }

    @Test
    void testGetMcpToolsKeepsNullLabelWhenClusterElementHasNoTitle() {
        McpTool mcpTool = createMcpTool(PlatformType.EMBEDDED, true, "getEmail");

        List<ConnectedUserIntegrationDTO.McpToolInfo> mcpToolInfos =
            getMcpTools(INTEGRATION_ID);

        assertThat(mcpToolInfos).containsExactly(
            new ConnectedUserIntegrationDTO.McpToolInfo(mcpTool.getId(), "getEmail", null, "Get an email"));
    }

    @Test
    void testGetMcpToolsSkipsToolsOfDisabledMcpServer() {
        createMcpTool(PlatformType.EMBEDDED, false, "sendEmail");

        assertThat(getMcpTools(INTEGRATION_ID)).isEmpty();
    }

    @Test
    void testGetMcpToolsSkipsToolsOfNonEmbeddedMcpServer() {
        createMcpTool(PlatformType.AUTOMATION, true, "sendEmail");

        assertThat(getMcpTools(INTEGRATION_ID)).isEmpty();
    }

    @Test
    void testGetMcpToolsSkipsGloballyDisabledTools() {
        McpTool enabledMcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");

        createDisabledMcpTool(enabledMcpTool.getMcpComponentId(), "getEmail");

        assertThat(getMcpTools(INTEGRATION_ID))
            .extracting(ConnectedUserIntegrationDTO.McpToolInfo::id)
            .containsExactly(enabledMcpTool.getId());
    }

    @Test
    void testGetMcpToolsListsAToolBoundToTheIntegration() {
        McpTool mcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");

        long integrationId = insertIntegration("Gmail");

        mcpIntegrationComponentService.create(mcpTool.getMcpComponentId(), integrationId);

        assertThat(getMcpTools(integrationId))
            .extracting(ConnectedUserIntegrationDTO.McpToolInfo::id)
            .containsExactly(mcpTool.getId());
    }

    @Test
    void testGetMcpToolsSkipsAToolBoundToAnotherIntegrationOnTheSameComponent() {
        McpTool mcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");

        long integrationId = insertIntegration("Gmail");
        long siblingIntegrationId = insertIntegration("Gmail Code");

        mcpIntegrationComponentService.create(mcpTool.getMcpComponentId(), siblingIntegrationId);

        assertThat(getMcpTools(integrationId)).isEmpty();
    }

    @Test
    void testGetMcpInstanceToolsSkipsGloballyDisabledTools() {
        McpTool enabledMcpTool = createMcpTool(PlatformType.EMBEDDED, true, "sendEmail");
        McpTool disabledMcpTool = createDisabledMcpTool(enabledMcpTool.getMcpComponentId(), "getEmail");

        when(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(1L)).thenReturn(
            List.of(
                new McpIntegrationInstanceTool(1L, enabledMcpTool.getId(), true),
                new McpIntegrationInstanceTool(1L, disabledMcpTool.getId(), true)));

        Set<Long> enabledEmbeddedMcpToolIds = connectedUserIntegrationFacade.getEnabledEmbeddedMcpToolIds(
            connectedUserIntegrationFacade.getEnabledEmbeddedMcpServerIds());

        assertThat(connectedUserIntegrationFacade.getMcpInstanceTools(1L, enabledEmbeddedMcpToolIds)).containsExactly(
            new ConnectedUserIntegrationDTO.McpInstanceToolInfo(enabledMcpTool.getId(), true));
    }

    private long insertIntegration(String name) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            Long.class, name, COMPONENT_NAME);

        return Objects.requireNonNull(id);
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

    private List<ConnectedUserIntegrationDTO.McpToolInfo> getMcpTools(long integrationId) {
        return connectedUserIntegrationFacade.getMcpTools(
            integrationId, COMPONENT_NAME, connectedUserIntegrationFacade.getEnabledEmbeddedMcpServerIds());
    }
}
