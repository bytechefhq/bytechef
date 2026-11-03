/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectConfigurationAdminFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
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
    AutomationWorkflowProjectConfigurationGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationWorkflowProjectConfigurationGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@MockitoBean(types = AutomationWorkflowProjectConfigurationAdminFacade.class)
class AutomationWorkflowProjectConfigurationGraphQlControllerIntTest {

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
    private AutomationWorkflowProjectConfigurationGraphQlController automationWorkflowProjectConfigurationGraphQlController;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAutomationWorkflowProjectConfigurationsOfAnEnvironment() {
        AutomationWorkflowProjectConfigurationDTO configuration = new AutomationWorkflowProjectConfigurationDTO(
            5L, 7L, 1L, "Standard", null, 1, true, true, 0, List.of(), Instant.parse("2026-10-28T10:00:00Z"), null);

        when(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(7L, 1L))
            .thenReturn(List.of(configuration));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurations(projectId: "7", environmentId: "1") {
                        id
                        environmentId
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurations[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("automationWorkflowProjectConfigurations[0].environmentId")
            .entity(String.class)
            .isEqualTo("1");

        verify(automationWorkflowProjectConfigurationAdminFacade, never()).getConfigurations(7L);
    }

    @Test
    void testAutomationWorkflowProjectConfigurationsReturnsWorkflowsWithTheirSwitches() {
        AutomationWorkflowProjectConfigurationDTO configuration = new AutomationWorkflowProjectConfigurationDTO(
            5L, 7L, 0L, "Beta", "metadata['beta'] == 'true'", 2, true, false, 3,
            List.of(
                new AutomationWorkflowProjectConfigurationDTO.Workflow("uuid-one", "workflow-id-one", "One", true,
                    null, Map.of("openai", 42L), Map.of("channel", "general")),
                new AutomationWorkflowProjectConfigurationDTO.Workflow("uuid-two", "workflow-id-two", "Two", false,
                    null, Map.of(), Map.of())),
            Instant.parse("2026-10-28T10:00:00Z"), null);

        when(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(7L))
            .thenReturn(List.of(configuration));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurations(projectId: "7") {
                        id
                        projectId
                        environmentId
                        name
                        permissionExpression
                        connectedUserCount
                        projectVersion
                        enabled
                        automationHubVisible
                        lastModifiedDate
                        workflows {
                            workflowUuid
                            workflowId
                            label
                            enabled
                            providedConnections
                            providedInputs
                        }
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurations[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("automationWorkflowProjectConfigurations[0].name")
            .entity(String.class)
            .isEqualTo("Beta")
            .path("automationWorkflowProjectConfigurations[0].permissionExpression")
            .entity(String.class)
            .isEqualTo("metadata['beta'] == 'true'")
            .path("automationWorkflowProjectConfigurations[0].connectedUserCount")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("automationWorkflowProjectConfigurations[0].projectVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationWorkflowProjectConfigurations[0].automationHubVisible")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationWorkflowProjectConfigurations[*].workflows[*].enabled")
            .entityList(Boolean.class)
            .containsExactly(true, false)
            .path("automationWorkflowProjectConfigurations[0].workflows[1].label")
            .entity(String.class)
            .isEqualTo("Two")
            .path("automationWorkflowProjectConfigurations[0].workflows[*].workflowId")
            .entityList(String.class)
            .containsExactly("workflow-id-one", "workflow-id-two")
            .path("automationWorkflowProjectConfigurations[0].workflows[0].providedConnections.openai")
            .entity(Long.class)
            .isEqualTo(42L)
            .path("automationWorkflowProjectConfigurations[0].workflows[0].providedInputs.channel")
            .entity(String.class)
            .isEqualTo("general")
            .path("automationWorkflowProjectConfigurations[0].workflows[1].providedConnections")
            .entity(Map.class)
            .isEqualTo(Map.of());
    }

    @Test
    void testAutomationWorkflowProjectConfigurationWorkflowProvidableValuesReturnsTheComponentReferenceOfAnInput() {
        when(automationWorkflowProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(5L, "uuid-one"))
            .thenReturn(
                new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO(
                    List.of(),
                    List.of(
                        new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input(
                            "channel", "Slack Channel", "STRING", true, "slack", 1, "channel",
                            List.of(
                                new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection(
                                    31L, "Company Slack")))),
                    Map.of(), Map.of()));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationWorkflowProvidableValues(id: "5", workflowUuid: "uuid-one") {
                        inputs {
                            name
                            componentName
                            componentVersion
                            groupName
                            optionConnections {
                                id
                                name
                            }
                        }
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.inputs[0].componentName")
            .entity(String.class)
            .isEqualTo("slack")
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.inputs[0].componentVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.inputs[0].groupName")
            .entity(String.class)
            .isEqualTo("channel")
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.inputs[0].optionConnections[0].id")
            .entity(String.class)
            .isEqualTo("31")
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.inputs[0].optionConnections[0].name")
            .entity(String.class)
            .isEqualTo("Company Slack");
    }

    @Test
    void testAutomationWorkflowProjectConfigurationWorkflowInputOptionsPassesTheConnectionAndDependencies() {
        when(automationWorkflowProjectConfigurationAdminFacade.getConfigurationWorkflowInputOptions(
            5L, "uuid-one", "channel", "channel", 31L, Map.of("workspace", "acme"), "gen"))
                .thenReturn(
                    List.of(
                        new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.InputOption(
                            "general", "C123", "Company-wide")));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationWorkflowInputOptions(
                        id: "5", workflowUuid: "uuid-one", inputName: "channel", propertyName: "channel",
                        connectionId: "31", lookupDependsOnValues: {workspace: "acme"}, searchText: "gen"
                    ) {
                        label
                        value
                        description
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationWorkflowInputOptions[0].label")
            .entity(String.class)
            .isEqualTo("general")
            .path("automationWorkflowProjectConfigurationWorkflowInputOptions[0].value")
            .entity(String.class)
            .isEqualTo("C123")
            .path("automationWorkflowProjectConfigurationWorkflowInputOptions[0].description")
            .entity(String.class)
            .isEqualTo("Company-wide");
    }

    @Test
    void testAutomationWorkflowProjectConfigurationWorkflowProvidableValuesReturnsSlotsInputsAndProvidedValues() {
        when(automationWorkflowProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(5L, "uuid-one"))
            .thenReturn(
                new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO(
                    List.of(
                        new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot(
                            "openai", "OpenAI", true,
                            List.of(
                                new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection(
                                    42L, "Company OpenAI")))),
                    List.of(
                        new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input(
                            "channel", "Channel", "STRING", true)),
                    Map.of("openai", 42L), Map.of("channel", "general")));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationWorkflowProvidableValues(id: "5", workflowUuid: "uuid-one") {
                        connectionSlots {
                            componentName
                            title
                            required
                            providableConnections {
                                id
                                name
                            }
                        }
                        inputs {
                            name
                            label
                            type
                            required
                        }
                        providedConnections
                        providedInputs
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.connectionSlots[0].componentName")
            .entity(String.class)
            .isEqualTo("openai")
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.connectionSlots[0].title")
            .entity(String.class)
            .isEqualTo("OpenAI")
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.connectionSlots[0].required")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path(
                "automationWorkflowProjectConfigurationWorkflowProvidableValues.connectionSlots[0].providableConnections[0].id")
            .entity(String.class)
            .isEqualTo("42")
            .path(
                "automationWorkflowProjectConfigurationWorkflowProvidableValues.connectionSlots[0].providableConnections[0].name")
            .entity(String.class)
            .isEqualTo("Company OpenAI")
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.inputs[0].name")
            .entity(String.class)
            .isEqualTo("channel")
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.inputs[0].required")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.providedConnections.openai")
            .entity(Long.class)
            .isEqualTo(42L)
            .path("automationWorkflowProjectConfigurationWorkflowProvidableValues.providedInputs.channel")
            .entity(String.class)
            .isEqualTo("general");
    }

    @Test
    void testAutomationWorkflowProjectConfigurationsListReadsNoLongerCarryConnectionSlotsOrInputs() {
        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurations(projectId: "7") {
                        workflows {
                            connectionSlots {
                                componentName
                            }
                        }
                    }
                }
                """)
            .execute()
            .errors()
            .expect(error -> Objects.toString(error.getMessage(), "")
                .contains("connectionSlots"))
            .verify();
    }

    @Test
    void testAutomationWorkflowProjectConfigurationVersionImpactReturnsTheWorkflowsItWouldPause() {
        when(automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(5L, 4))
            .thenReturn(
                new AutomationWorkflowProjectConfigurationVersionImpactDTO(
                    List.of(
                        new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-one", "Notify Slack", List.of("channel"), List.of("jira"), 12))));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationVersionImpact(id: "5", projectVersion: 4) {
                        workflows {
                            workflowUuid
                            label
                            newRequiredInputs
                            newRequiredConnections
                            affectedConnectedUserCount
                        }
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-one")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].label")
            .entity(String.class)
            .isEqualTo("Notify Slack")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].newRequiredInputs")
            .entityList(String.class)
            .containsExactly("channel")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].newRequiredConnections")
            .entityList(String.class)
            .containsExactly("jira")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].affectedConnectedUserCount")
            .entity(Integer.class)
            .isEqualTo(12);
    }

    @Test
    void testAutomationWorkflowProjectConfigurationDeleteImpactReturnsTheCounts() {
        when(automationWorkflowProjectConfigurationAdminFacade.getDeleteImpact(5L))
            .thenReturn(new AutomationWorkflowProjectConfigurationDeleteImpactDTO(3, 7));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationDeleteImpact(id: "5") {
                        connectedUserCount
                        referenceWorkflowCount
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationDeleteImpact.connectedUserCount")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("automationWorkflowProjectConfigurationDeleteImpact.referenceWorkflowCount")
            .entity(Integer.class)
            .isEqualTo(7);
    }

    @Test
    void testAutomationWorkflowProjectConfigurationConnectedUsersReturnsTheAssignedUsers() {
        when(automationWorkflowProjectConfigurationAdminFacade.getConfigurationConnectedUsers(5L))
            .thenReturn(
                List.of(
                    new AutomationWorkflowProjectConfigurationConnectedUserDTO(
                        21L, "external-21", "Jane", "jane@example.com"),
                    new AutomationWorkflowProjectConfigurationConnectedUserDTO(22L, "external-22", null, null)));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationConnectedUsers(id: "5") {
                        connectedUserId
                        externalId
                        name
                        email
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationConnectedUsers[*].connectedUserId")
            .entityList(String.class)
            .containsExactly("21", "22")
            .path("automationWorkflowProjectConfigurationConnectedUsers[0].externalId")
            .entity(String.class)
            .isEqualTo("external-21")
            .path("automationWorkflowProjectConfigurationConnectedUsers[0].name")
            .entity(String.class)
            .isEqualTo("Jane")
            .path("automationWorkflowProjectConfigurationConnectedUsers[0].email")
            .entity(String.class)
            .isEqualTo("jane@example.com")
            .path("automationWorkflowProjectConfigurationConnectedUsers[1].name")
            .valueIsNull()
            .path("automationWorkflowProjectConfigurationConnectedUsers[1].email")
            .valueIsNull();
    }

    @Test
    void testAutomationWorkflowProjectConfigurationMoveImpactReturnsWorkflowsAndSkippedUsers() {
        when(automationWorkflowProjectConfigurationAdminFacade.getMoveImpact(5L, 6L, List.of(21L, 22L)))
            .thenReturn(
                new ConfigurationMoveImpactDTO(
                    List.of(
                        new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-one", "Notify Slack", List.of("channel"), List.of("jira"), 1)),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            22L, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
                            List.of("Sync Leads")))));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationMoveImpact(
                        sourceId: "5", targetId: "6", connectedUserIds: ["21", "22"]) {
                        workflows {
                            workflowUuid
                            label
                            newRequiredInputs
                            newRequiredConnections
                            affectedConnectedUserCount
                        }
                        skipped {
                            connectedUserId
                            reason
                            workflowLabels
                        }
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationMoveImpact.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-one")
            .path("automationWorkflowProjectConfigurationMoveImpact.workflows[0].newRequiredInputs")
            .entityList(String.class)
            .containsExactly("channel")
            .path("automationWorkflowProjectConfigurationMoveImpact.workflows[0].newRequiredConnections")
            .entityList(String.class)
            .containsExactly("jira")
            .path("automationWorkflowProjectConfigurationMoveImpact.workflows[0].affectedConnectedUserCount")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationWorkflowProjectConfigurationMoveImpact.skipped[0].connectedUserId")
            .entity(String.class)
            .isEqualTo("22")
            .path("automationWorkflowProjectConfigurationMoveImpact.skipped[0].reason")
            .entity(String.class)
            .isEqualTo("TARGET_LACKS_REFERENCED_WORKFLOWS")
            .path("automationWorkflowProjectConfigurationMoveImpact.skipped[0].workflowLabels")
            .entityList(String.class)
            .containsExactly("Sync Leads");
    }

    @Test
    void testMoveAutomationWorkflowProjectConfigurationConnectedUsersReturnsTheResult() {
        when(automationWorkflowProjectConfigurationAdminFacade.moveConnectedUsers(5L, 6L, List.of(21L, 22L, 23L)))
            .thenReturn(
                new ConfigurationMoveResultDTO(
                    List.of(21L, 23L),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            22L, ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of())),
                    List.of(23L)));

        this.graphQlTester
            .document("""
                mutation {
                    moveAutomationWorkflowProjectConfigurationConnectedUsers(
                        sourceId: "5", targetId: "6", connectedUserIds: ["21", "22", "23"]) {
                        movedConnectedUserIds
                        needsAttentionConnectedUserIds
                        skipped {
                            connectedUserId
                            reason
                            workflowLabels
                        }
                    }
                }
                """)
            .execute()
            .path("moveAutomationWorkflowProjectConfigurationConnectedUsers.movedConnectedUserIds")
            .entityList(String.class)
            .containsExactly("21", "23")
            .path("moveAutomationWorkflowProjectConfigurationConnectedUsers.needsAttentionConnectedUserIds")
            .entityList(String.class)
            .containsExactly("23")
            .path("moveAutomationWorkflowProjectConfigurationConnectedUsers.skipped[0].connectedUserId")
            .entity(String.class)
            .isEqualTo("22")
            .path("moveAutomationWorkflowProjectConfigurationConnectedUsers.skipped[0].reason")
            .entity(String.class)
            .isEqualTo("NOT_ON_SOURCE")
            .path("moveAutomationWorkflowProjectConfigurationConnectedUsers.skipped[0].workflowLabels")
            .entityList(String.class)
            .hasSize(0);
    }

    @Test
    void testCreateAutomationWorkflowProjectConfigurationDelegatesAndReturnsId() {
        when(automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            7L, 0L, "Beta", "metadata['beta'] == 'true'", 2, true, false))
                .thenReturn(11L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationWorkflowProjectConfiguration(
                        projectId: "7", environmentId: "0", name: "Beta",
                        permissionExpression: "metadata['beta'] == 'true'", projectVersion: 2, enabled: true,
                        automationHubVisible: false)
                }
                """)
            .execute()
            .path("createAutomationWorkflowProjectConfiguration")
            .entity(String.class)
            .isEqualTo("11");
    }

    @Test
    void testCreateAutomationWorkflowProjectConfigurationWithoutPermissionExpressionPassesNull() {
        when(automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            7L, 0L, "Alpha", null, 2, true, true))
                .thenReturn(12L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationWorkflowProjectConfiguration(
                        projectId: "7", environmentId: "0", name: "Alpha", projectVersion: 2, enabled: true,
                        automationHubVisible: true)
                }
                """)
            .execute()
            .path("createAutomationWorkflowProjectConfiguration")
            .entity(String.class)
            .isEqualTo("12");
    }

    @Test
    void testUpdateAutomationWorkflowProjectConfigurationPassesNameAndPermissionExpression() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationWorkflowProjectConfiguration(
                        id: "5", name: "Gamma", permissionExpression: "metadata['gamma'] == 'true'",
                        projectVersion: 3, enabled: false, automationHubVisible: true)
                }
                """)
            .execute()
            .path("updateAutomationWorkflowProjectConfiguration")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectConfigurationAdminFacade).updateConfiguration(
            5L, "Gamma", "metadata['gamma'] == 'true'", 3, false, true);
    }

    @Test
    void testEnableAutomationWorkflowProjectConfigurationWorkflowDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    enableAutomationWorkflowProjectConfigurationWorkflow(id: "5", workflowUuid: "uuid", enable: false)
                }
                """)
            .execute()
            .path("enableAutomationWorkflowProjectConfigurationWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectConfigurationAdminFacade)
            .enableConfigurationWorkflow(5L, "uuid", false);
    }

    @Test
    void testUpdateAutomationWorkflowProjectConfigurationWorkflowConvertsConnectionIdsAndDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationWorkflowProjectConfigurationWorkflow(
                        id: "5", workflowUuid: "uuid", providedConnections: {openai: 42, slack: "43"},
                        providedInputs: {channel: "general", limit: 3})
                }
                """)
            .execute()
            .path("updateAutomationWorkflowProjectConfigurationWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectConfigurationAdminFacade)
            .updateConfigurationWorkflow(
                5L, "uuid", Map.of("openai", 42L, "slack", 43L), Map.of("channel", "general", "limit", 3));
    }

    @Test
    void testUpdateAutomationWorkflowProjectConfigurationWorkflowWithoutValuesClearsThem() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationWorkflowProjectConfigurationWorkflow(id: "5", workflowUuid: "uuid")
                }
                """)
            .execute()
            .path("updateAutomationWorkflowProjectConfigurationWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectConfigurationAdminFacade)
            .updateConfigurationWorkflow(5L, "uuid", Map.of(), Map.of());
    }

    @Test
    void testUpdateAutomationWorkflowProjectConfigurationWorkflowRejectsANonNumericConnectionId() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationWorkflowProjectConfigurationWorkflow(
                        id: "5", workflowUuid: "uuid", providedConnections: {slack: "abc"})
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationGraphQlController
                .updateAutomationWorkflowProjectConfigurationWorkflow(
                    "5", "uuid", Map.of("slack", "abc"), Map.of()))
                        .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                            assertThat(configurationException.getMessage()).contains("abc", "slack");
                            assertThat(configurationException.getErrorKey()).isEqualTo(
                                AutomationWorkflowProjectConfigurationErrorType.INVALID_PROVIDED_VALUE.getErrorKey());
                        });

        verify(automationWorkflowProjectConfigurationAdminFacade, never())
            .updateConfigurationWorkflow(anyLong(), anyString(), anyMap(), any());
    }

    @Test
    void testDeleteAutomationWorkflowProjectConfigurationDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteAutomationWorkflowProjectConfiguration(id: "5")
                }
                """)
            .execute()
            .path("deleteAutomationWorkflowProjectConfiguration")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectConfigurationAdminFacade).deleteConfiguration(5L);
    }
}
