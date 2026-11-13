/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectAdminFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    ConnectedUserProjectGraphQlController.class,
    ConnectedUserProjectGraphQlControllerIntTest.GraphQlErrorConfiguration.class
})
@GraphQlTest(
    controllers = ConnectedUserProjectGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectedUserProjectGraphQlControllerIntTest {

    private static final String AUTOMATION_UUID = "4a1b2c3d-5e6f-4789-8a9b-0c1d2e3f4a5b";
    private static final String COPIED_FROM_WORKFLOW_UUID = "7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f0a";

    @MockitoBean
    private ConnectedUserProjectAdminFacade connectedUserProjectAdminFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testConnectedUserProjectsReturnsSourceWorkflowUuids() {
        when(environmentService.getEnvironment(0L)).thenReturn(Environment.DEVELOPMENT);
        when(connectedUserProjectAdminFacade.getConnectedUserProjects(7L, Environment.DEVELOPMENT))
            .thenReturn(List.of(createConnectedUserProjectDTO(null)));

        this.graphQlTester
            .document("""
                query {
                    connectedUserProjects(connectedUserId: "7", environmentId: "0") {
                        connectedUserProjectWorkflows {
                            id
                            attentionReason
                            automationUuid
                            copiedFromWorkflowUuid
                        }
                    }
                }
                """)
            .execute()
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[*].id")
            .entityList(String.class)
            .containsExactly("1", "2")
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[0].attentionReason")
            .entity(String.class)
            .isEqualTo("TURNED_OFF_BY_ADMINISTRATOR")
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[1].attentionReason")
            .valueIsNull()
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[0].automationUuid")
            .entity(String.class)
            .isEqualTo(AUTOMATION_UUID)
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[0].copiedFromWorkflowUuid")
            .valueIsNull()
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[1].automationUuid")
            .valueIsNull()
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[1].copiedFromWorkflowUuid")
            .entity(String.class)
            .isEqualTo(COPIED_FROM_WORKFLOW_UUID);
    }

    @Test
    void testConnectedUserProjectsReturnsTheAutomationProjectConfigurations() {
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), null, true, "external-7", 7L, null, 0);

        when(environmentService.getEnvironment(0L)).thenReturn(Environment.DEVELOPMENT);
        when(connectedUserProjectAdminFacade.getConnectedUserProjects(7L, Environment.DEVELOPMENT))
            .thenReturn(List.of(createConnectedUserProjectDTO(connectedUser)));
        when(connectedUserProjectAdminFacade.getConnectedUserProjectConfigurations(7L))
            .thenReturn(
                List.of(
                    new ConnectedUserAutomationProjectConfigurationDTO(5L, 11L, "Beta", 3, true),
                    new ConnectedUserAutomationProjectConfigurationDTO(6L, 12L, "DEVELOPMENT", 1, false)));

        this.graphQlTester
            .document("""
                query {
                    connectedUserProjects(connectedUserId: "7", environmentId: "0") {
                        automationProjectConfigurations {
                            projectId
                            configurationId
                            configurationName
                            configurationProjectVersion
                            configurationEnabled
                        }
                    }
                }
                """)
            .execute()
            .path("connectedUserProjects[0].automationProjectConfigurations[*].projectId")
            .entityList(String.class)
            .containsExactly("5", "6")
            .path("connectedUserProjects[0].automationProjectConfigurations[*].configurationId")
            .entityList(String.class)
            .containsExactly("11", "12")
            .path("connectedUserProjects[0].automationProjectConfigurations[0].configurationName")
            .entity(String.class)
            .isEqualTo("Beta")
            .path("connectedUserProjects[0].automationProjectConfigurations[0].configurationProjectVersion")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("connectedUserProjects[0].automationProjectConfigurations[*].configurationEnabled")
            .entityList(Boolean.class)
            .containsExactly(true, false);
    }

    @Test
    void testConnectedUserProjectWorkflowReferenceValues() {
        when(connectedUserProjectAdminFacade.getProjectWorkflowReferenceValues(5L))
            .thenReturn(
                new ConnectedUserWorkflowReferenceValuesDTO(Map.of("slack", 42L), Map.of("channel", "#alerts")));

        this.graphQlTester
            .document("""
                query {
                    connectedUserProjectWorkflowReferenceValues(id: "5") {
                        connections {
                            componentName
                            connectionId
                        }
                        inputs
                    }
                }
                """)
            .execute()
            .path("connectedUserProjectWorkflowReferenceValues.connections[0].componentName")
            .entity(String.class)
            .isEqualTo("slack")
            .path("connectedUserProjectWorkflowReferenceValues.connections[0].connectionId")
            .entity(String.class)
            .isEqualTo("42")
            .path("connectedUserProjectWorkflowReferenceValues.inputs.channel")
            .entity(String.class)
            .isEqualTo("#alerts");
    }

    @Test
    void testProvisionableConnectedUserProjects() {
        List<ProvisionableAutomationProjectDTO.Workflow> alphaWorkflows = List.of(
            new ProvisionableAutomationProjectDTO.Workflow(
                "uuid-1", "Label", List.of(new ConnectedUserWorkflowTemplateDTO.Input("name", "Name", "STRING", true)),
                true, false,
                List.of(new ConnectedUserWorkflowTemplateDTO.Component("slack", "Slack", "<svg/>", true, null))));

        when(connectedUserProjectAdminFacade.getProvisionableProjects(7L))
            .thenReturn(List.of(
                new ProvisionableAutomationProjectDTO(
                    5L, "Project", 2, alphaWorkflows,
                    List.of(
                        new ProvisionableAutomationProjectDTO.Configuration(11L, "Alpha", 2, true,
                            alphaWorkflows),
                        new ProvisionableAutomationProjectDTO.Configuration(
                            12L, "Beta", 3, false,
                            List.of(new ProvisionableAutomationProjectDTO.Workflow(
                                "uuid-2", "Other", List.of(), false, false, List.of())))),
                    11L)));

        this.graphQlTester
            .document("""
                query {
                    provisionableConnectedUserProjects(connectedUserId: "7") {
                        projectId
                        name
                        projectVersion
                        workflows {
                            workflowUuid
                            label
                            provisioned
                            dangling
                            inputs {
                                name
                                label
                                type
                                required
                            }
                            components {
                                name
                                title
                                icon
                            }
                        }
                        configurations {
                            id
                            name
                            projectVersion
                            selfServe
                            workflows {
                                workflowUuid
                            }
                        }
                        assignedConfigurationId
                    }
                }
                """)
            .execute()
            .path("provisionableConnectedUserProjects[0].projectId")
            .entity(String.class)
            .isEqualTo("5")
            .path("provisionableConnectedUserProjects[0].name")
            .entity(String.class)
            .isEqualTo("Project")
            .path("provisionableConnectedUserProjects[0].projectVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("provisionableConnectedUserProjects[0].workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-1")
            .path("provisionableConnectedUserProjects[0].workflows[0].provisioned")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("provisionableConnectedUserProjects[0].workflows[0].dangling")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("provisionableConnectedUserProjects[0].workflows[0].inputs[0].name")
            .entity(String.class)
            .isEqualTo("name")
            .path("provisionableConnectedUserProjects[0].workflows[0].inputs[0].required")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("provisionableConnectedUserProjects[0].workflows[0].components[0].name")
            .entity(String.class)
            .isEqualTo("slack")
            .path("provisionableConnectedUserProjects[0].workflows[0].components[0].title")
            .entity(String.class)
            .isEqualTo("Slack")
            .path("provisionableConnectedUserProjects[0].workflows[0].components[0].icon")
            .entity(String.class)
            .isEqualTo("<svg/>")
            .path("provisionableConnectedUserProjects[0].assignedConfigurationId")
            .entity(String.class)
            .isEqualTo("11")
            .path("provisionableConnectedUserProjects[0].configurations[*].id")
            .entityList(String.class)
            .containsExactly("11", "12")
            .path("provisionableConnectedUserProjects[0].configurations[*].name")
            .entityList(String.class)
            .containsExactly("Alpha", "Beta")
            .path("provisionableConnectedUserProjects[0].configurations[*].projectVersion")
            .entityList(Integer.class)
            .containsExactly(2, 3)
            .path("provisionableConnectedUserProjects[0].configurations[*].selfServe")
            .entityList(Boolean.class)
            .containsExactly(true, false)
            .path("provisionableConnectedUserProjects[0].configurations[1].workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-2");
    }

    @Test
    void testProvisionableConnectedUserProjectsWithoutAssignment() {
        when(connectedUserProjectAdminFacade.getProvisionableProjects(7L))
            .thenReturn(List.of(new ProvisionableAutomationProjectDTO(5L, "Project", 2, List.of(), List.of(),
                null)));

        this.graphQlTester
            .document("""
                query {
                    provisionableConnectedUserProjects(connectedUserId: "7") {
                        assignedConfigurationId
                    }
                }
                """)
            .execute()
            .path("provisionableConnectedUserProjects[0].assignedConfigurationId")
            .valueIsNull();
    }

    @Test
    void testProvisionConnectedUserProjectWorkflows() {
        this.graphQlTester
            .document("""
                mutation {
                    provisionConnectedUserProjectWorkflows(
                        connectedUserId: "7",
                        configurationId: "11",
                        workflows: [
                            {
                                workflowUuid: "uuid-1", inputs: {name: "x"},
                                connections: [{componentName: "slack", connectionId: "41"}]
                            },
                            {workflowUuid: "uuid-2"}
                        ])
                }
                """)
            .execute()
            .path("provisionConnectedUserProjectWorkflows")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(connectedUserProjectAdminFacade).provisionProjectWorkflows(
            7L, 11L,
            List.of(
                new ProvisionConnectedUserProjectWorkflowDTO("uuid-1", Map.of("name", "x"), Map.of("slack", 41L)),
                new ProvisionConnectedUserProjectWorkflowDTO("uuid-2", null)));
    }

    @Test
    void testProvisionConnectedUserProjectWorkflowsRejectsTwoConnectionsForOneComponent() {
        this.graphQlTester
            .document("""
                mutation {
                    provisionConnectedUserProjectWorkflows(
                        connectedUserId: "7",
                        configurationId: "11",
                        workflows: [
                            {
                                workflowUuid: "uuid-1",
                                connections: [
                                    {componentName: "slack", connectionId: "41"},
                                    {componentName: "slack", connectionId: "42"}
                                ]
                            }
                        ])
                }
                """)
            .execute()
            .errors()
            .expect(error -> {
                String message = error.getMessage();

                return error.getErrorType() == ErrorType.BAD_REQUEST && message != null && message.contains("slack");
            });

        verify(connectedUserProjectAdminFacade, never()).provisionProjectWorkflows(anyLong(), anyLong(), anyList());
    }

    private static ConnectedUserProjectDTO createConnectedUserProjectDTO(@Nullable ConnectedUser connectedUser) {
        ConnectedUserProjectWorkflowDTO referenceWorkflow = new ConnectedUserProjectWorkflowDTO(
            1L, 7L, true, null, 0L, null, AUTOMATION_UUID, 1, ConnectedUserProjectWorkflowDTO.Kind.REFERENCE,
            AUTOMATION_UUID, null, false, List.of(), List.of(), Map.of(), "TURNED_OFF_BY_ADMINISTRATOR", null);

        ConnectedUserProjectWorkflowDTO copyWorkflow = new ConnectedUserProjectWorkflowDTO(
            2L, 7L, true, null, 3L, null, "copy-workflow-uuid", 1, ConnectedUserProjectWorkflowDTO.Kind.COPY, null,
            COPIED_FROM_WORKFLOW_UUID, false, List.of(), List.of(), Map.of(), null, null);

        return new ConnectedUserProjectDTO(
            3L, connectedUser, List.of(referenceWorkflow, copyWorkflow), Environment.DEVELOPMENT, 0L, null, 3L, null,
            null,
            null, null, null, 0);
    }

    @Configuration
    @ComponentScan("com.bytechef.graphql.error")
    static class GraphQlErrorConfiguration {
    }
}
