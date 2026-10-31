/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationComponent;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EmbeddedMcpIntTestConfiguration.class, properties = "bytechef.edition=ee")
class McpIntegrationComponentServiceIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationComponentService mcpIntegrationComponentService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    private long gmailCodeIntegrationId;
    private long gmailIntegrationId;
    private long mcpComponentId;
    private long mcpServerId;

    @BeforeEach
    void beforeEach() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        mcpServerId = Objects.requireNonNull(mcpServer.getId());

        mcpComponentId = createMcpComponent(mcpServerId);

        gmailIntegrationId = insertIntegration("Gmail");
        gmailCodeIntegrationId = insertIntegration("Gmail Code");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_integration_component");
        jdbcTemplate.update("DELETE FROM integration");

        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testFetchMcpIntegrationComponentReturnsTheBoundIntegration() {
        mcpIntegrationComponentService.create(mcpComponentId, gmailIntegrationId);

        assertThat(mcpIntegrationComponentService.fetchMcpIntegrationComponent(mcpComponentId))
            .hasValueSatisfying(mcpIntegrationComponent -> assertThat(mcpIntegrationComponent.getIntegrationId())
                .isEqualTo(gmailIntegrationId));
    }

    @Test
    void testFetchMcpIntegrationComponentOfAnUnboundComponentIsEmpty() {
        assertThat(mcpIntegrationComponentService.fetchMcpIntegrationComponent(mcpComponentId)).isEmpty();
    }

    @Test
    void testGetIntegrationMcpIntegrationComponentsReturnsOnlyThatIntegrationsComponents() {
        McpServer siblingMcpServer = mcpServerRepository.save(
            new McpServer("Google Code", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        long siblingMcpComponentId = createMcpComponent(Objects.requireNonNull(siblingMcpServer.getId()));

        mcpIntegrationComponentService.create(mcpComponentId, gmailIntegrationId);
        mcpIntegrationComponentService.create(siblingMcpComponentId, gmailCodeIntegrationId);

        assertThat(mcpIntegrationComponentService.getIntegrationMcpIntegrationComponents(gmailIntegrationId))
            .extracting(McpIntegrationComponent::getMcpComponentId)
            .containsExactly(mcpComponentId);
    }

    @Test
    void testCreateRejectsASecondBindingOfTheSameComponent() {
        mcpIntegrationComponentService.create(mcpComponentId, gmailIntegrationId);

        assertThatThrownBy(() -> mcpIntegrationComponentService.create(mcpComponentId, gmailCodeIntegrationId))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testDeletingTheMcpComponentDeletesItsBinding() {
        mcpIntegrationComponentService.create(mcpComponentId, gmailIntegrationId);

        mcpComponentRepository.deleteById(mcpComponentId);

        assertThat(mcpIntegrationComponentService.getIntegrationMcpIntegrationComponents(gmailIntegrationId))
            .isEmpty();
    }

    @Test
    void testDeletingTheIntegrationDeletesItsBindingButKeepsTheMcpComponent() {
        mcpIntegrationComponentService.create(mcpComponentId, gmailIntegrationId);

        jdbcTemplate.update("DELETE FROM integration WHERE id = ?", gmailIntegrationId);

        assertThat(mcpIntegrationComponentService.fetchMcpIntegrationComponent(mcpComponentId)).isEmpty();
        assertThat(mcpComponentRepository.findById(mcpComponentId)).isPresent();
    }

    private long createMcpComponent(long mcpServerId) {
        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("googleMail", 1, mcpServerId, null));

        return Objects.requireNonNull(mcpComponent.getId());
    }

    private long insertIntegration(String name) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, 'googleMail', false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            Long.class, name);

        return Objects.requireNonNull(id);
    }
}
