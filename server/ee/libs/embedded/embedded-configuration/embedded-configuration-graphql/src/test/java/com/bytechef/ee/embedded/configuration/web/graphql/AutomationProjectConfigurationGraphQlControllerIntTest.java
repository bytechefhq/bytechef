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

import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectConfigurationAdminFacade;
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
    AutomationProjectConfigurationGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationProjectConfigurationGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@MockitoBean(types = AutomationProjectConfigurationAdminFacade.class)
class AutomationProjectConfigurationGraphQlControllerIntTest {

    @Autowired
    private AutomationProjectConfigurationAdminFacade automationProjectConfigurationAdminFacade;

    @Autowired
    private AutomationProjectConfigurationGraphQlController automationProjectConfigurationGraphQlController;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAutomationProjectConfigurationsOfAnEnvironment() {
        AutomationProjectConfigurationDTO configuration = new AutomationProjectConfigurationDTO(
            5L, 7L, 1L, "Standard", null, 1, true, true, 0, List.of(), Instant.parse("2026-10-28T10:00:00Z"), null);

        when(automationProjectConfigurationAdminFacade.getConfigurations(7L, 1L))
            .thenReturn(List.of(configuration));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurations(projectId: "7", environmentId: "1") {
                        id
                        environmentId
                    }
                }
                """)
            .execute()
            .path("automationProjectConfigurations[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("automationProjectConfigurations[0].environmentId")
            .entity(String.class)
            .isEqualTo("1");

        verify(automationProjectConfigurationAdminFacade, never()).getConfigurations(7L);
    }

    @Test
    void testAutomationProjectConfigurationsReturnsWorkflowsWithTheirSwitches() {
        AutomationProjectConfigurationDTO configuration = new AutomationProjectConfigurationDTO(
            5L, 7L, 0L, "Beta", "metadata['beta'] == 'true'", 2, true, false, 3,
            List.of(
                new AutomationProjectConfigurationDTO.Workflow("uuid-one", "workflow-id-one", "One", true,
                    null, Map.of("openai", 42L), Map.of("channel", "general")),
                new AutomationProjectConfigurationDTO.Workflow("uuid-two", "workflow-id-two", "Two", false,
                    null, Map.of(), Map.of())),
            Instant.parse("2026-10-28T10:00:00Z"), null);

        when(automationProjectConfigurationAdminFacade.getConfigurations(7L))
            .thenReturn(List.of(configuration));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurations(projectId: "7") {
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
            .path("automationProjectConfigurations[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("automationProjectConfigurations[0].name")
            .entity(String.class)
            .isEqualTo("Beta")
            .path("automationProjectConfigurations[0].permissionExpression")
            .entity(String.class)
            .isEqualTo("metadata['beta'] == 'true'")
            .path("automationProjectConfigurations[0].connectedUserCount")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("automationProjectConfigurations[0].projectVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationProjectConfigurations[0].automationHubVisible")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationProjectConfigurations[*].workflows[*].enabled")
            .entityList(Boolean.class)
            .containsExactly(true, false)
            .path("automationProjectConfigurations[0].workflows[1].label")
            .entity(String.class)
            .isEqualTo("Two")
            .path("automationProjectConfigurations[0].workflows[*].workflowId")
            .entityList(String.class)
            .containsExactly("workflow-id-one", "workflow-id-two")
            .path("automationProjectConfigurations[0].workflows[0].providedConnections.openai")
            .entity(Long.class)
            .isEqualTo(42L)
            .path("automationProjectConfigurations[0].workflows[0].providedInputs.channel")
            .entity(String.class)
            .isEqualTo("general")
            .path("automationProjectConfigurations[0].workflows[1].providedConnections")
            .entity(Map.class)
            .isEqualTo(Map.of());
    }

    @Test
    void testAutomationProjectConfigurationWorkflowProvidableValuesReturnsTheComponentReferenceOfAnInput() {
        when(automationProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(5L, "uuid-one", null))
            .thenReturn(
                new AutomationProjectConfigurationWorkflowProvidableValuesDTO(
                    List.of(),
                    List.of(
                        new AutomationProjectConfigurationWorkflowProvidableValuesDTO.Input(
                            "channel", "Slack Channel", "STRING", true, "slack", 1, "channel",
                            List.of(
                                new AutomationProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection(
                                    31L, "Company Slack")))),
                    Map.of(), Map.of()));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationWorkflowProvidableValues(id: "5", workflowUuid: "uuid-one") {
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
            .path("automationProjectConfigurationWorkflowProvidableValues.inputs[0].componentName")
            .entity(String.class)
            .isEqualTo("slack")
            .path("automationProjectConfigurationWorkflowProvidableValues.inputs[0].componentVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationProjectConfigurationWorkflowProvidableValues.inputs[0].groupName")
            .entity(String.class)
            .isEqualTo("channel")
            .path("automationProjectConfigurationWorkflowProvidableValues.inputs[0].optionConnections[0].id")
            .entity(String.class)
            .isEqualTo("31")
            .path("automationProjectConfigurationWorkflowProvidableValues.inputs[0].optionConnections[0].name")
            .entity(String.class)
            .isEqualTo("Company Slack");
    }

    @Test
    void testAutomationProjectConfigurationWorkflowProvidableValuesPassesTheProjectVersion() {
        when(automationProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(5L, "uuid-one", 3))
            .thenReturn(
                new AutomationProjectConfigurationWorkflowProvidableValuesDTO(
                    List.of(), List.of(), Map.of(), Map.of()));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationWorkflowProvidableValues(
                        id: "5", workflowUuid: "uuid-one", projectVersion: 3) {
                        providedConnections
                    }
                }
                """)
            .execute()
            .path("automationProjectConfigurationWorkflowProvidableValues.providedConnections")
            .entity(Map.class)
            .isEqualTo(Map.of());

        verify(automationProjectConfigurationAdminFacade).getConfigurationWorkflowProvidableValues(5L, "uuid-one", 3);
    }

    @Test
    void testAutomationProjectConfigurationWorkflowInputOptionsPassesTheConnectionAndDependencies() {
        when(automationProjectConfigurationAdminFacade.getConfigurationWorkflowInputOptions(
            5L, "uuid-one", null, "channel", "channel", 31L, Map.of("workspace", "acme"), "gen"))
                .thenReturn(
                    List.of(
                        new AutomationProjectConfigurationWorkflowProvidableValuesDTO.InputOption(
                            "general", "C123", "Company-wide")));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationWorkflowInputOptions(
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
            .path("automationProjectConfigurationWorkflowInputOptions[0].label")
            .entity(String.class)
            .isEqualTo("general")
            .path("automationProjectConfigurationWorkflowInputOptions[0].value")
            .entity(String.class)
            .isEqualTo("C123")
            .path("automationProjectConfigurationWorkflowInputOptions[0].description")
            .entity(String.class)
            .isEqualTo("Company-wide");
    }

    @Test
    void testAutomationProjectConfigurationWorkflowProvidableValuesReturnsSlotsInputsAndProvidedValues() {
        when(automationProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(5L, "uuid-one", null))
            .thenReturn(
                new AutomationProjectConfigurationWorkflowProvidableValuesDTO(
                    List.of(
                        new AutomationProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot(
                            "openai", "OpenAI", true,
                            List.of(
                                new AutomationProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection(
                                    42L, "Company OpenAI")))),
                    List.of(
                        new AutomationProjectConfigurationWorkflowProvidableValuesDTO.Input(
                            "channel", "Channel", "STRING", true)),
                    Map.of("openai", 42L), Map.of("channel", "general")));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationWorkflowProvidableValues(id: "5", workflowUuid: "uuid-one") {
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
            .path("automationProjectConfigurationWorkflowProvidableValues.connectionSlots[0].componentName")
            .entity(String.class)
            .isEqualTo("openai")
            .path("automationProjectConfigurationWorkflowProvidableValues.connectionSlots[0].title")
            .entity(String.class)
            .isEqualTo("OpenAI")
            .path("automationProjectConfigurationWorkflowProvidableValues.connectionSlots[0].required")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path(
                "automationProjectConfigurationWorkflowProvidableValues.connectionSlots[0].providableConnections[0].id")
            .entity(String.class)
            .isEqualTo("42")
            .path(
                "automationProjectConfigurationWorkflowProvidableValues.connectionSlots[0].providableConnections[0].name")
            .entity(String.class)
            .isEqualTo("Company OpenAI")
            .path("automationProjectConfigurationWorkflowProvidableValues.inputs[0].name")
            .entity(String.class)
            .isEqualTo("channel")
            .path("automationProjectConfigurationWorkflowProvidableValues.inputs[0].required")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationProjectConfigurationWorkflowProvidableValues.providedConnections.openai")
            .entity(Long.class)
            .isEqualTo(42L)
            .path("automationProjectConfigurationWorkflowProvidableValues.providedInputs.channel")
            .entity(String.class)
            .isEqualTo("general");
    }

    @Test
    void testAutomationProjectConfigurationsListReadsNoLongerCarryConnectionSlotsOrInputs() {
        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurations(projectId: "7") {
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
    void testAutomationProjectConfigurationVersionImpactReturnsTheWorkflowsItWouldPause() {
        when(automationProjectConfigurationAdminFacade.getVersionImpact(5L, 4))
            .thenReturn(
                new AutomationProjectConfigurationVersionImpactDTO(
                    List.of(
                        new AutomationProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-one", "Notify Slack", List.of("channel"), List.of("jira"), 12))));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationVersionImpact(id: "5", projectVersion: 4) {
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
            .path("automationProjectConfigurationVersionImpact.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-one")
            .path("automationProjectConfigurationVersionImpact.workflows[0].label")
            .entity(String.class)
            .isEqualTo("Notify Slack")
            .path("automationProjectConfigurationVersionImpact.workflows[0].newRequiredInputs")
            .entityList(String.class)
            .containsExactly("channel")
            .path("automationProjectConfigurationVersionImpact.workflows[0].newRequiredConnections")
            .entityList(String.class)
            .containsExactly("jira")
            .path("automationProjectConfigurationVersionImpact.workflows[0].affectedConnectedUserCount")
            .entity(Integer.class)
            .isEqualTo(12);
    }

    @Test
    void testAutomationProjectConfigurationDeleteImpactReturnsTheCounts() {
        when(automationProjectConfigurationAdminFacade.getDeleteImpact(5L))
            .thenReturn(new AutomationProjectConfigurationDeleteImpactDTO(3, 7));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationDeleteImpact(id: "5") {
                        connectedUserCount
                        referenceWorkflowCount
                    }
                }
                """)
            .execute()
            .path("automationProjectConfigurationDeleteImpact.connectedUserCount")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("automationProjectConfigurationDeleteImpact.referenceWorkflowCount")
            .entity(Integer.class)
            .isEqualTo(7);
    }

    @Test
    void testAutomationProjectConfigurationConnectedUsersReturnsTheAssignedUsers() {
        when(automationProjectConfigurationAdminFacade.getConfigurationConnectedUsers(5L))
            .thenReturn(
                List.of(
                    new AutomationProjectConfigurationConnectedUserDTO(
                        21L, "external-21", "Jane", "jane@example.com"),
                    new AutomationProjectConfigurationConnectedUserDTO(22L, "external-22", null, null)));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationConnectedUsers(id: "5") {
                        connectedUserId
                        externalId
                        name
                        email
                    }
                }
                """)
            .execute()
            .path("automationProjectConfigurationConnectedUsers[*].connectedUserId")
            .entityList(String.class)
            .containsExactly("21", "22")
            .path("automationProjectConfigurationConnectedUsers[0].externalId")
            .entity(String.class)
            .isEqualTo("external-21")
            .path("automationProjectConfigurationConnectedUsers[0].name")
            .entity(String.class)
            .isEqualTo("Jane")
            .path("automationProjectConfigurationConnectedUsers[0].email")
            .entity(String.class)
            .isEqualTo("jane@example.com")
            .path("automationProjectConfigurationConnectedUsers[1].name")
            .valueIsNull()
            .path("automationProjectConfigurationConnectedUsers[1].email")
            .valueIsNull();
    }

    @Test
    void testAutomationProjectConfigurationMoveImpactReturnsWorkflowsAndSkippedUsers() {
        when(automationProjectConfigurationAdminFacade.getMoveImpact(5L, 6L, List.of(21L, 22L)))
            .thenReturn(
                new ConfigurationMoveImpactDTO(
                    List.of(
                        new AutomationProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-one", "Notify Slack", List.of("channel"), List.of("jira"), 1)),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            22L, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
                            List.of("Sync Leads")))));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationMoveImpact(
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
            .path("automationProjectConfigurationMoveImpact.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-one")
            .path("automationProjectConfigurationMoveImpact.workflows[0].newRequiredInputs")
            .entityList(String.class)
            .containsExactly("channel")
            .path("automationProjectConfigurationMoveImpact.workflows[0].newRequiredConnections")
            .entityList(String.class)
            .containsExactly("jira")
            .path("automationProjectConfigurationMoveImpact.workflows[0].affectedConnectedUserCount")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationProjectConfigurationMoveImpact.skipped[0].connectedUserId")
            .entity(String.class)
            .isEqualTo("22")
            .path("automationProjectConfigurationMoveImpact.skipped[0].reason")
            .entity(String.class)
            .isEqualTo("TARGET_LACKS_REFERENCED_WORKFLOWS")
            .path("automationProjectConfigurationMoveImpact.skipped[0].workflowLabels")
            .entityList(String.class)
            .containsExactly("Sync Leads");
    }

    @Test
    void testMoveAutomationProjectConfigurationConnectedUsersReturnsTheResult() {
        when(automationProjectConfigurationAdminFacade.moveConnectedUsers(5L, 6L, List.of(21L, 22L, 23L)))
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
                    moveAutomationProjectConfigurationConnectedUsers(
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
            .path("moveAutomationProjectConfigurationConnectedUsers.movedConnectedUserIds")
            .entityList(String.class)
            .containsExactly("21", "23")
            .path("moveAutomationProjectConfigurationConnectedUsers.needsAttentionConnectedUserIds")
            .entityList(String.class)
            .containsExactly("23")
            .path("moveAutomationProjectConfigurationConnectedUsers.skipped[0].connectedUserId")
            .entity(String.class)
            .isEqualTo("22")
            .path("moveAutomationProjectConfigurationConnectedUsers.skipped[0].reason")
            .entity(String.class)
            .isEqualTo("NOT_ON_SOURCE")
            .path("moveAutomationProjectConfigurationConnectedUsers.skipped[0].workflowLabels")
            .entityList(String.class)
            .hasSize(0);
    }

    @Test
    void testAutomationProjectConfigurationUpgradeImpactReturnsWorkflowsAndSkippedUsers() {
        when(automationProjectConfigurationAdminFacade.getUpgradeImpact(6L, List.of(21L, 22L)))
            .thenReturn(
                new ConfigurationMoveImpactDTO(
                    List.of(
                        new AutomationProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-one", "Notify Slack", List.of("channel"), List.of("jira"), 1)),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            22L, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
                            List.of("Sync Leads")))));

        this.graphQlTester
            .document("""
                query {
                    automationProjectConfigurationUpgradeImpact(
                        targetId: "6", connectedUserIds: ["21", "22"]) {
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
            .path("automationProjectConfigurationUpgradeImpact.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-one")
            .path("automationProjectConfigurationUpgradeImpact.workflows[0].newRequiredInputs")
            .entityList(String.class)
            .containsExactly("channel")
            .path("automationProjectConfigurationUpgradeImpact.workflows[0].newRequiredConnections")
            .entityList(String.class)
            .containsExactly("jira")
            .path("automationProjectConfigurationUpgradeImpact.workflows[0].affectedConnectedUserCount")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationProjectConfigurationUpgradeImpact.skipped[0].connectedUserId")
            .entity(String.class)
            .isEqualTo("22")
            .path("automationProjectConfigurationUpgradeImpact.skipped[0].reason")
            .entity(String.class)
            .isEqualTo("TARGET_LACKS_REFERENCED_WORKFLOWS")
            .path("automationProjectConfigurationUpgradeImpact.skipped[0].workflowLabels")
            .entityList(String.class)
            .containsExactly("Sync Leads");

        verify(automationProjectConfigurationAdminFacade).getUpgradeImpact(6L, List.of(21L, 22L));
    }

    @Test
    void testUpgradeAutomationProjectConfigurationConnectedUsersReturnsTheResult() {
        when(automationProjectConfigurationAdminFacade.upgradeConnectedUsers(6L, List.of(21L, 22L, 23L)))
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
                    upgradeAutomationProjectConfigurationConnectedUsers(
                        targetId: "6", connectedUserIds: ["21", "22", "23"]) {
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
            .path("upgradeAutomationProjectConfigurationConnectedUsers.movedConnectedUserIds")
            .entityList(String.class)
            .containsExactly("21", "23")
            .path("upgradeAutomationProjectConfigurationConnectedUsers.needsAttentionConnectedUserIds")
            .entityList(String.class)
            .containsExactly("23")
            .path("upgradeAutomationProjectConfigurationConnectedUsers.skipped[0].connectedUserId")
            .entity(String.class)
            .isEqualTo("22")
            .path("upgradeAutomationProjectConfigurationConnectedUsers.skipped[0].reason")
            .entity(String.class)
            .isEqualTo("NOT_ON_SOURCE")
            .path("upgradeAutomationProjectConfigurationConnectedUsers.skipped[0].workflowLabels")
            .entityList(String.class)
            .hasSize(0);

        verify(automationProjectConfigurationAdminFacade).upgradeConnectedUsers(6L, List.of(21L, 22L, 23L));
    }

    @Test
    void testCreateAutomationProjectConfigurationDelegatesAndReturnsId() {
        when(automationProjectConfigurationAdminFacade.createConfiguration(
            7L, 0L, "Beta", "metadata['beta'] == 'true'", 2, true, false))
                .thenReturn(11L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationProjectConfiguration(
                        projectId: "7", environmentId: "0", name: "Beta",
                        permissionExpression: "metadata['beta'] == 'true'", projectVersion: 2, enabled: true,
                        automationHubVisible: false)
                }
                """)
            .execute()
            .path("createAutomationProjectConfiguration")
            .entity(String.class)
            .isEqualTo("11");
    }

    @Test
    void testCreateAutomationProjectConfigurationWithoutPermissionExpressionPassesNull() {
        when(automationProjectConfigurationAdminFacade.createConfiguration(
            7L, 0L, "Alpha", null, 2, true, true))
                .thenReturn(12L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationProjectConfiguration(
                        projectId: "7", environmentId: "0", name: "Alpha", projectVersion: 2, enabled: true,
                        automationHubVisible: true)
                }
                """)
            .execute()
            .path("createAutomationProjectConfiguration")
            .entity(String.class)
            .isEqualTo("12");
    }

    @Test
    void testUpdateAutomationProjectConfigurationPassesNameAndPermissionExpression() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationProjectConfiguration(
                        id: "5", name: "Gamma", permissionExpression: "metadata['gamma'] == 'true'",
                        projectVersion: 3, enabled: false, automationHubVisible: true)
                }
                """)
            .execute()
            .path("updateAutomationProjectConfiguration")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectConfigurationAdminFacade).updateConfiguration(
            5L, "Gamma", "metadata['gamma'] == 'true'", 3, false, true);
    }

    @Test
    void testEnableAutomationProjectConfigurationWorkflowDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    enableAutomationProjectConfigurationWorkflow(id: "5", workflowUuid: "uuid", enable: false)
                }
                """)
            .execute()
            .path("enableAutomationProjectConfigurationWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectConfigurationAdminFacade)
            .enableConfigurationWorkflow(5L, "uuid", false);
    }

    @Test
    void testUpdateAutomationProjectConfigurationWorkflowConvertsConnectionIdsAndDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationProjectConfigurationWorkflow(
                        id: "5", workflowUuid: "uuid", providedConnections: {openai: 42, slack: "43"},
                        providedInputs: {channel: "general", limit: 3})
                }
                """)
            .execute()
            .path("updateAutomationProjectConfigurationWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectConfigurationAdminFacade)
            .updateConfigurationWorkflow(
                5L, "uuid", Map.of("openai", 42L, "slack", 43L), Map.of("channel", "general", "limit", 3));
    }

    @Test
    void testUpdateAutomationProjectConfigurationWorkflowWithoutValuesClearsThem() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationProjectConfigurationWorkflow(id: "5", workflowUuid: "uuid")
                }
                """)
            .execute()
            .path("updateAutomationProjectConfigurationWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectConfigurationAdminFacade)
            .updateConfigurationWorkflow(5L, "uuid", Map.of(), Map.of());
    }

    @Test
    void testUpdateAutomationProjectConfigurationWorkflowRejectsANonNumericConnectionId() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationProjectConfigurationWorkflow(
                        id: "5", workflowUuid: "uuid", providedConnections: {slack: "abc"})
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        assertThatThrownBy(
            () -> automationProjectConfigurationGraphQlController
                .updateAutomationProjectConfigurationWorkflow(
                    "5", "uuid", Map.of("slack", "abc"), Map.of()))
                        .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                            assertThat(configurationException.getMessage()).contains("abc", "slack");
                            assertThat(configurationException.getErrorKey()).isEqualTo(
                                AutomationProjectConfigurationErrorType.INVALID_PROVIDED_VALUE.getErrorKey());
                        });

        verify(automationProjectConfigurationAdminFacade, never())
            .updateConfigurationWorkflow(anyLong(), anyString(), anyMap(), any());
    }

    @Test
    void testDeleteAutomationProjectConfigurationDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteAutomationProjectConfiguration(id: "5")
                }
                """)
            .execute()
            .path("deleteAutomationProjectConfiguration")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectConfigurationAdminFacade).deleteConfiguration(5L);
    }
}
