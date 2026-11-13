/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.configuration.dto.WorkflowToolParametersDTO;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    McpIntegrationInstanceConfigurationGraphQlControllerIntTest.ScalarConfiguration.class,
    McpIntegrationInstanceConfigurationGraphQlController.class
})
@GraphQlTest(
    controllers = McpIntegrationInstanceConfigurationGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class McpIntegrationInstanceConfigurationGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @MockitoBean
    private IntegrationService integrationService;

    @MockitoBean
    private McpIntegrationInstanceConfigurationFacade mcpIntegrationInstanceConfigurationFacade;

    @MockitoBean
    private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @Test
    void testCreateMcpIntegrationInstanceConfigurationWithToolParameters() {
        when(
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                anyLong(), anyLong(), any(), any(), any()))
                    .thenReturn(new McpIntegrationInstanceConfiguration(1L, 2L, 3L));

        graphQlTester
            .document("""
                mutation {
                    createMcpIntegrationInstanceConfiguration(input: {
                        mcpServerId: "3",
                        integrationInstanceConfigurationId: "2",
                        selectedWorkflowIds: ["workflow1"],
                        toolParameters: [{
                            workflowId: "workflow1",
                            parameters: {toolName: "my_tool"}
                        }]
                    }) {
                        id
                    }
                }
                """)
            .execute()
            .path("createMcpIntegrationInstanceConfiguration.id")
            .entity(String.class)
            .isEqualTo("1");

        verify(mcpIntegrationInstanceConfigurationFacade).createMcpIntegrationInstanceConfiguration(
            3L, 2L, List.of("workflow1"), List.of(),
            List.of(new WorkflowToolParametersDTO("workflow1", Map.of("toolName", "my_tool"))));
    }

    @Test
    void testCreateMcpIntegrationInstanceConfigurationWithoutToolParameters() {
        when(
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                anyLong(), anyLong(), any(), any(), any()))
                    .thenReturn(new McpIntegrationInstanceConfiguration(1L, 2L, 3L));

        graphQlTester
            .document("""
                mutation {
                    createMcpIntegrationInstanceConfiguration(input: {
                        mcpServerId: "3",
                        integrationInstanceConfigurationId: "2",
                        selectedWorkflowIds: ["workflow1"]
                    }) {
                        id
                    }
                }
                """)
            .execute()
            .path("createMcpIntegrationInstanceConfiguration.id")
            .entity(String.class)
            .isEqualTo("1");

        verify(mcpIntegrationInstanceConfigurationFacade).createMcpIntegrationInstanceConfiguration(
            3L, 2L, List.of("workflow1"), List.of(), List.of());
    }

    @Test
    void testUpdateMcpIntegrationInstanceConfigurationWithToolParameters() {
        when(
            mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
                anyLong(), any(), any(), any()))
                    .thenReturn(new McpIntegrationInstanceConfiguration(1L, 2L, 3L));

        graphQlTester
            .document("""
                mutation {
                    updateMcpIntegrationInstanceConfiguration(id: "1", input: {
                        selectedWorkflowIds: ["workflow1"],
                        toolParameters: [{
                            workflowId: "workflow1",
                            parameters: {toolName: "my_tool"}
                        }]
                    }) {
                        id
                    }
                }
                """)
            .execute()
            .path("updateMcpIntegrationInstanceConfiguration.id")
            .entity(String.class)
            .isEqualTo("1");

        verify(mcpIntegrationInstanceConfigurationFacade).updateMcpIntegrationInstanceConfiguration(
            1L, List.of("workflow1"), List.of(),
            List.of(new WorkflowToolParametersDTO("workflow1", Map.of("toolName", "my_tool"))));
    }

    @TestConfiguration
    static class ScalarConfiguration {

        @Bean
        RuntimeWiringConfigurer longScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
        }

        @Bean
        RuntimeWiringConfigurer mapScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
        }
    }
}
