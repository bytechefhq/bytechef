/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationWorkflowConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationProjectConfigurationFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@ContextConfiguration(classes = {
    McpAutomationProjectConfigurationGraphQlControllerIntTest.ScalarConfiguration.class,
    McpAutomationProjectConfigurationGraphQlController.class
})
@GraphQlTest(
    controllers = McpAutomationProjectConfigurationGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class McpAutomationProjectConfigurationGraphQlControllerIntTest {

    private static final String WORKFLOW_UUID = "7eed0000-0000-0000-0000-0000000000a1";

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private McpAutomationProjectConfigurationFacade mcpAutomationProjectConfigurationFacade;

    @Test
    void testMcpAutomationProjectConfigurationsByServerId() {
        when(mcpAutomationProjectConfigurationFacade.getMcpServerMcpAutomationProjectConfigurations(3L))
            .thenReturn(List.of(createDTO(true)));

        graphQlTester.document("""
            query {
                mcpAutomationProjectConfigurationsByServerId(mcpServerId: "3") {
                    id
                    mcpServerId
                    automationProjectConfigurationId
                    automationProjectConfigurationName
                    automationProjectConfigurationEnabled
                    projectId
                    projectName
                    projectVersion
                    workflows {
                        id
                        workflowUuid
                        label
                        parameters
                        enabled
                        available
                    }
                }
            }
            """)
            .execute()
            .path("mcpAutomationProjectConfigurationsByServerId[0].automationProjectConfigurationName")
            .entity(String.class)
            .isEqualTo("Configuration")
            .path("mcpAutomationProjectConfigurationsByServerId[0].workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo(WORKFLOW_UUID)
            .path("mcpAutomationProjectConfigurationsByServerId[0].workflows[0].available")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("mcpAutomationProjectConfigurationsByServerId[0].workflows[0].parameters.name")
            .entity(String.class)
            .isEqualTo("tool");
    }

    @Test
    void testMcpAutomationProjectConfigurationsByServerIdReturnsProjectName() {
        when(mcpAutomationProjectConfigurationFacade.getMcpServerMcpAutomationProjectConfigurations(3L))
            .thenReturn(List.of(createDTO(true)));

        graphQlTester.document("""
            query {
                mcpAutomationProjectConfigurationsByServerId(mcpServerId: "3") {
                    projectName
                }
            }
            """)
            .execute()
            .path("mcpAutomationProjectConfigurationsByServerId[0].projectName")
            .entity(String.class)
            .isEqualTo("Project");
    }

    @Test
    void testMcpAutomationProjectConfigurationToolWorkflowUuids() {
        when(mcpAutomationProjectConfigurationFacade.getToolWorkflowUuids(5L))
            .thenReturn(List.of(WORKFLOW_UUID));

        graphQlTester.document("""
            query {
                mcpAutomationProjectConfigurationToolWorkflowUuids(automationProjectConfigurationId: "5")
            }
            """)
            .execute()
            .path("mcpAutomationProjectConfigurationToolWorkflowUuids")
            .entityList(String.class)
            .containsExactly(WORKFLOW_UUID);
    }

    @Test
    void testMcpAutomationProjectConfigurationsByServerIdMarksUnavailable() {
        when(mcpAutomationProjectConfigurationFacade.getMcpServerMcpAutomationProjectConfigurations(3L))
            .thenReturn(List.of(createDTO(false)));

        graphQlTester.document("""
            query {
                mcpAutomationProjectConfigurationsByServerId(mcpServerId: "3") {
                    workflows {
                        available
                    }
                }
            }
            """)
            .execute()
            .path("mcpAutomationProjectConfigurationsByServerId[0].workflows[0].available")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testMcpAutomationProjectConfigurationWorkflowProperties() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions("triggers", WorkflowTrigger.class, List.of()))
            .thenReturn(
                List.of(
                    new WorkflowTrigger(
                        Map.of(
                            "name", "trigger_1", "type", "workflow/v1/newWorkflowCall", "parameters",
                            Map.of(
                                "inputSchema",
                                "{\"type\":\"object\",\"properties\":{\"customerId\":{\"type\":\"string\"}}}")))));
        when(mcpAutomationProjectConfigurationFacade.fetchMcpAutomationProjectConfigurationWorkflowPinnedWorkflow(9L))
            .thenReturn(Optional.of(workflow));

        graphQlTester.document("""
            query {
                mcpAutomationProjectConfigurationWorkflowProperties(mcpAutomationProjectConfigurationWorkflowId: "9") {
                    name
                }
            }
            """)
            .execute()
            .path("mcpAutomationProjectConfigurationWorkflowProperties[*].name")
            .entityList(String.class)
            .containsExactly("toolName", "toolDescription", "customerId");
    }

    @Test
    void testMcpAutomationProjectConfigurationWorkflowPropertiesWithoutWorkflowCallTrigger() {
        when(mcpAutomationProjectConfigurationFacade.fetchMcpAutomationProjectConfigurationWorkflowPinnedWorkflow(9L))
            .thenReturn(Optional.empty());

        graphQlTester.document("""
            query {
                mcpAutomationProjectConfigurationWorkflowProperties(mcpAutomationProjectConfigurationWorkflowId: "9") {
                    name
                }
            }
            """)
            .execute()
            .path("mcpAutomationProjectConfigurationWorkflowProperties")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testCreateMcpAutomationProjectConfiguration() {
        when(
            mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                eq(3L), eq(5L), eq(List.of(WORKFLOW_UUID)), anyList()))
                    .thenReturn(createLink());
        when(mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(11L))
            .thenReturn(createDTO(true));

        graphQlTester.document("""
            mutation {
                createMcpAutomationProjectConfiguration(
                    input: {
                        mcpServerId: "3", automationProjectConfigurationId: "5", workflowUuids: ["%s"],
                        workflowConfigurations: [{workflowUuid: "%s", parameters: {name: "tool"}}]
                    }) {
                    id
                    workflows {
                        workflowUuid
                    }
                }
            }
            """.replace("%s", WORKFLOW_UUID))
            .execute()
            .path("createMcpAutomationProjectConfiguration.id")
            .entity(String.class)
            .isEqualTo("11")
            .path("createMcpAutomationProjectConfiguration.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo(WORKFLOW_UUID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<McpAutomationWorkflowConfigurationDTO>> workflowConfigurationsCaptor =
            ArgumentCaptor.forClass(List.class);

        verify(mcpAutomationProjectConfigurationFacade).createMcpAutomationProjectConfiguration(
            eq(3L), eq(5L), eq(List.of(WORKFLOW_UUID)), workflowConfigurationsCaptor.capture());

        assertThat(workflowConfigurationsCaptor.getValue())
            .containsExactly(new McpAutomationWorkflowConfigurationDTO(WORKFLOW_UUID, Map.of("name", "tool")));
    }

    @Test
    void testCreateMcpAutomationProjectConfigurationWithoutWorkflowConfigurations() {
        when(
            mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                3L, 5L, List.of(WORKFLOW_UUID), List.of()))
                    .thenReturn(createLink());
        when(mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(11L))
            .thenReturn(createDTO(true));

        graphQlTester.document("""
            mutation {
                createMcpAutomationProjectConfiguration(
                    input: {mcpServerId: "3", automationProjectConfigurationId: "5", workflowUuids: ["%s"]}) {
                    id
                }
            }
            """.replace("%s", WORKFLOW_UUID))
            .execute()
            .path("createMcpAutomationProjectConfiguration.id")
            .entity(String.class)
            .isEqualTo("11");
    }

    @Test
    void testCreateMcpAutomationProjectConfigurationReportsRejection() {
        when(
            mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                3L, 5L, List.of(WORKFLOW_UUID), List.of()))
                    .thenThrow(new AccessDeniedException("denied"));

        graphQlTester.document("""
            mutation {
                createMcpAutomationProjectConfiguration(
                    input: {mcpServerId: "3", automationProjectConfigurationId: "5", workflowUuids: ["%s"]}) {
                    id
                }
            }
            """.replace("%s", WORKFLOW_UUID))
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));
    }

    @Test
    void testUpdateMcpAutomationProjectConfiguration() {
        when(
            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                eq(11L), eq(List.of(WORKFLOW_UUID)), anyList()))
                    .thenReturn(createLink());
        when(mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(11L))
            .thenReturn(createDTO(true));

        graphQlTester.document("""
            mutation {
                updateMcpAutomationProjectConfiguration(
                    id: "11",
                    input: {
                        workflowUuids: ["%s"], workflowConfigurations: [{workflowUuid: "%s"}]
                    }) {
                    id
                    workflows {
                        workflowUuid
                    }
                }
            }
            """.replace("%s", WORKFLOW_UUID))
            .execute()
            .path("updateMcpAutomationProjectConfiguration.id")
            .entity(String.class)
            .isEqualTo("11");

        verify(mcpAutomationProjectConfigurationFacade).updateMcpAutomationProjectConfiguration(
            11L, List.of(WORKFLOW_UUID), List.of(new McpAutomationWorkflowConfigurationDTO(WORKFLOW_UUID, Map.of())));
    }

    @Test
    void testDeleteMcpAutomationProjectConfiguration() {
        graphQlTester.document("""
            mutation {
                deleteMcpAutomationProjectConfiguration(id: "11")
            }
            """)
            .execute()
            .path("deleteMcpAutomationProjectConfiguration")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mcpAutomationProjectConfigurationFacade).deleteMcpAutomationProjectConfiguration(11L);
    }

    @Test
    void testUpdateMcpAutomationProjectConfigurationWorkflowParameters() {
        when(
            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowParameters(
                eq(21L), anyMap()))
                    .thenReturn(createLinkWorkflow());
        when(mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(11L))
            .thenReturn(createDTO(true));

        graphQlTester.document("""
            mutation {
                updateMcpAutomationProjectConfigurationWorkflowParameters(id: "21", parameters: {name: "tool"}) {
                    id
                    parameters
                }
            }
            """)
            .execute()
            .path("updateMcpAutomationProjectConfigurationWorkflowParameters.id")
            .entity(String.class)
            .isEqualTo("21")
            .path("updateMcpAutomationProjectConfigurationWorkflowParameters.parameters.name")
            .entity(String.class)
            .isEqualTo("tool");

        verify(mcpAutomationProjectConfigurationFacade)
            .updateMcpAutomationProjectConfigurationWorkflowParameters(21L, Map.of("name", "tool"));
    }

    @Test
    void testUpdateMcpAutomationProjectConfigurationWorkflowEnabled() {
        when(mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowEnabled(21L, false))
            .thenReturn(createLinkWorkflow());
        when(mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(11L))
            .thenReturn(createDTO(true));

        graphQlTester.document("""
            mutation {
                updateMcpAutomationProjectConfigurationWorkflowEnabled(id: "21", enabled: false) {
                    id
                    enabled
                }
            }
            """)
            .execute()
            .path("updateMcpAutomationProjectConfigurationWorkflowEnabled.id")
            .entity(String.class)
            .isEqualTo("21");

        verify(mcpAutomationProjectConfigurationFacade)
            .updateMcpAutomationProjectConfigurationWorkflowEnabled(21L, false);
    }

    private static McpAutomationProjectConfigurationDTO createDTO(boolean available) {
        return new McpAutomationProjectConfigurationDTO(
            11L, 3L, 5L, "Configuration", 7L, "Project", 1, true,
            List.of(
                new McpAutomationProjectConfigurationWorkflowDTO(
                    21L, WORKFLOW_UUID, "Workflow", Map.of("name", "tool"), true, available)));
    }

    private static McpAutomationProjectConfiguration createLink() {
        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
            new McpAutomationProjectConfiguration(5L, 3L);

        mcpAutomationProjectConfiguration.setId(11L);

        return mcpAutomationProjectConfiguration;
    }

    private static McpAutomationProjectConfigurationWorkflow createLinkWorkflow() {
        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow =
            new McpAutomationProjectConfigurationWorkflow(11L, WORKFLOW_UUID);

        mcpAutomationProjectConfigurationWorkflow.setId(21L);

        return mcpAutomationProjectConfigurationWorkflow;
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

        @Bean
        RuntimeWiringConfigurer propertyTypeResolverWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.type(
                "Property", typeWiring -> typeWiring.typeResolver(
                    environment -> environment.getSchema()
                        .getObjectType("StringProperty")));
        }
    }
}
