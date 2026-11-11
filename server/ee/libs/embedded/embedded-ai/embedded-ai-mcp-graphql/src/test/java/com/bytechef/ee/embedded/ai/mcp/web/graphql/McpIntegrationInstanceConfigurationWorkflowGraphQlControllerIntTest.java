/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceConfigurationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@ExtendWith(ObjectMapperSetupExtension.class)
@ContextConfiguration(classes = {
    McpIntegrationInstanceConfigurationWorkflowGraphQlControllerIntTest.ScalarConfiguration.class,
    McpIntegrationInstanceConfigurationWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = McpIntegrationInstanceConfigurationWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class McpIntegrationInstanceConfigurationWorkflowGraphQlControllerIntTest {

    private static final String INPUT_SCHEMA =
        "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}";

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @MockitoBean
    private McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @Test
    void testToolEligibleIntegrationInstanceConfigurationWorkflowProperties() {
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of(
                "name", "newWorkflowCall_1", "type", "workflow/v1/newWorkflowCall",
                "parameters", Map.of("inputSchema", INPUT_SCHEMA)));

        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(workflowTrigger));
        when(
            mcpIntegrationInstanceConfigurationWorkflowFacade.getToolEligibleIntegrationInstanceConfigurationWorkflow(
                5L, "workflow1"))
                    .thenReturn(workflow);

        graphQlTester
            .document("""
                query {
                    toolEligibleIntegrationInstanceConfigurationWorkflowProperties(
                        integrationInstanceConfigurationId: "5", workflowId: "workflow1") {
                        name
                    }
                }
                """)
            .execute()
            .path("toolEligibleIntegrationInstanceConfigurationWorkflowProperties[*].name")
            .entityList(String.class)
            .containsExactly("toolName", "toolDescription", "message");

        verify(mcpIntegrationInstanceConfigurationWorkflowFacade)
            .getToolEligibleIntegrationInstanceConfigurationWorkflow(5L, "workflow1");
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
