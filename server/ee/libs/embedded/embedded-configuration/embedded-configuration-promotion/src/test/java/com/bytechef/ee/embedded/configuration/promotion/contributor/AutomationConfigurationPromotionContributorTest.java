/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.test.assertion.PromotionContributorAssertions;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectConfigurationFacade;
import com.bytechef.ee.embedded.configuration.promotion.EmbeddedConnectionCandidates;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AutomationConfigurationPromotionContributorTest {

    private static final long EXISTING_TARGET_CONNECTION_ID = 52L;
    private static final String NOTIFY_WORKFLOW_UUID = "5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0012";
    private static final String PERMISSION_EXPRESSION = "hasRole('ORDERS')";
    private static final long PRODUCTION_ID = Environment.PRODUCTION.ordinal();
    private static final long PROJECT_ID = 1050L;
    private static final String PROVISION_WORKFLOW_UUID = "5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0013";
    private static final long SOURCE_CONNECTION_ID = 41L;
    private static final long SOURCE_ID = 100L;
    private static final String SYNC_WORKFLOW_UUID = "5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0011";
    private static final long TARGET_CONNECTION_ID = 51L;
    private static final long TARGET_ID = 200L;
    private static final UUID CONFIGURATION_UUID = UUID.fromString("5b1f4c1e-8b8a-4e43-9d1f-3a3f7c7a0001");

    @Mock
    private AutomationProjectConfigurationFacade automationProjectConfigurationFacade;

    @Mock
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private EmbeddedConnectionCandidates embeddedConnectionCandidates;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    private AutomationConfigurationPromotionContributor contributor;

    @BeforeEach
    void beforeEach() {
        contributor = new AutomationConfigurationPromotionContributor(
            automationProjectConfigurationFacade, automationProjectConfigurationService, connectionService,
            embeddedConnectionCandidates, projectWorkflowService, workflowService);

        lenient().when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 1))
            .thenReturn(List.of(projectWorkflow(1, "workflow-a-v1", SYNC_WORKFLOW_UUID)));
        lenient().when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 2))
            .thenReturn(List.of(
                projectWorkflow(2, "workflow-a-v2", SYNC_WORKFLOW_UUID),
                projectWorkflow(2, "workflow-b-v2", NOTIFY_WORKFLOW_UUID)));
        lenient().when(workflowService.getWorkflow("workflow-a-v1"))
            .thenReturn(workflow("workflow-a-v1", "Sync Orders", internalOnlyInput("apiKey", false)));
        lenient().when(workflowService.getWorkflow("workflow-a-v2"))
            .thenReturn(workflow(
                "workflow-a-v2", "Sync Orders", internalOnlyInput("apiKey", false),
                internalOnlyInput("region", false),
                "{\"name\": \"note\", \"label\": \"note\", \"type\": \"string\", \"required\": false}"));
        lenient().when(workflowService.getWorkflow("workflow-b-v2"))
            .thenReturn(workflow("workflow-b-v2", "Notify", internalOnlyInput("recipient", false)));
        lenient().when(workflowService.getWorkflow("workflow-p-v2"))
            .thenReturn(workflow("workflow-p-v2", "Provision Account", internalOnlyInput("accountId", true)));
        lenient().when(connectionService.getConnections(anyList()))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Slack Dev")));
        lenient().when(embeddedConnectionCandidates.candidates("slack", Environment.PRODUCTION))
            .thenReturn(List.of(
                new PromotionConnectionCandidate(TARGET_CONNECTION_ID, "Slack Dev"),
                new PromotionConnectionCandidate(53L, "Slack Other")));
        lenient().when(automationProjectConfigurationService.getConfigurations(PROJECT_ID, PRODUCTION_ID))
            .thenReturn(List.of());
    }

    @Test
    void testGetPlatformType() {
        assertThat(contributor.getPlatformType()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testGetResourceType() {
        assertThat(contributor.getResourceType()).isEqualTo("AUTOMATION_CONFIGURATION");
    }

    @Test
    void testPreviewAndPromoteRequireTenantAdmin() {
        PromotionContributorAssertions.assertPreAuthorize(
            AutomationConfigurationPromotionContributor.class, "preview",
            new Class<?>[] {
                long.class, Environment.class, Environment.class
            },
            "isTenantAdmin()");
        PromotionContributorAssertions.assertPreAuthorize(
            AutomationConfigurationPromotionContributor.class, "promote",
            new Class<?>[] {
                long.class, Environment.class, Environment.class, Map.class, String.class
            },
            "isTenantAdmin()");
    }

    @Test
    void testPromoteCreatesDisabledCopyWithEmptyInputsAndRemappedConnections() {
        AutomationProjectConfiguration source = sourceConfiguration(2);
        AutomationProjectConfiguration created = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders", 2, false, Set.of());

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.empty());
        when(automationProjectConfigurationService.create(
            PROJECT_ID, PRODUCTION_ID, "Orders", PERMISSION_EXPRESSION, 2, false, false, CONFIGURATION_UUID))
                .thenReturn(created);
        when(automationProjectConfigurationService.getConfiguration(TARGET_ID)).thenReturn(created);

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION,
            Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID), null);

        assertThat(result.created()).isTrue();
        assertThat(result.targetId()).isEqualTo(TARGET_ID);
        assertThat(result.unresolvedConnectionIds()).isEmpty();

        InOrder facadeOrder = inOrder(automationProjectConfigurationFacade);

        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfiguration(TARGET_ID, "Orders", PERMISSION_EXPRESSION, 2, false, false);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(
                TARGET_ID, SYNC_WORKFLOW_UUID, Map.of("slack", TARGET_CONNECTION_ID), Map.of());
        facadeOrder.verify(automationProjectConfigurationFacade)
            .enableConfigurationWorkflow(TARGET_ID, NOTIFY_WORKFLOW_UUID, false);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(TARGET_ID, NOTIFY_WORKFLOW_UUID, Map.of(), Map.of());

        verifyNoMoreInteractions(automationProjectConfigurationFacade);
    }

    @Test
    void testPromoteCreateLeavesUnmappedConnectionUnresolved() {
        AutomationProjectConfiguration source = sourceConfiguration(2);
        AutomationProjectConfiguration created = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders", 2, false, Set.of());

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.empty());
        when(automationProjectConfigurationService.create(
            anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyBoolean(), anyBoolean(), any(UUID.class)))
                .thenReturn(created);
        when(automationProjectConfigurationService.getConfiguration(TARGET_ID)).thenReturn(created);

        when(embeddedConnectionCandidates.candidates("slack", Environment.PRODUCTION)).thenReturn(List.of());

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(result.unresolvedConnectionIds()).containsExactly(SOURCE_CONNECTION_ID);

        verify(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(TARGET_ID, SYNC_WORKFLOW_UUID, Map.of(), Map.of());
    }

    @Test
    void testPromoteCreateDisablesWorkflowNeedingInternalOnlyInput() {
        AutomationProjectConfiguration source = sourceConfiguration(
            2, workflowRow(PROVISION_WORKFLOW_UUID, true, Map.of(), Map.of("accountId", "development-account")));
        AutomationProjectConfiguration created = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders", 2, false, Set.of());

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 2))
            .thenReturn(List.of(projectWorkflow(2, "workflow-p-v2", PROVISION_WORKFLOW_UUID)));
        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.empty());

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");

        when(automationProjectConfigurationService.create(
            anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyBoolean(), anyBoolean(), any(UUID.class)))
                .thenReturn(created);
        when(automationProjectConfigurationService.getConfiguration(TARGET_ID)).thenReturn(created);

        contributor.promote(SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        InOrder facadeOrder = inOrder(automationProjectConfigurationFacade);

        facadeOrder.verify(automationProjectConfigurationFacade)
            .enableConfigurationWorkflow(TARGET_ID, PROVISION_WORKFLOW_UUID, false);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(TARGET_ID, PROVISION_WORKFLOW_UUID, Map.of(), Map.of());
    }

    @Test
    void testPromoteUpdateGoesThroughFacadeAndKeepsTargetInputs() {
        AutomationProjectConfiguration source = sourceConfiguration(2);
        AutomationProjectConfiguration existingTarget = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders Production", 1, true,
            Set.of(
                workflowRow(
                    SYNC_WORKFLOW_UUID, false, Map.of("slack", EXISTING_TARGET_CONNECTION_ID),
                    Map.of("apiKey", "production-key"))));
        AutomationProjectConfiguration updatedTarget = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders", 2, true,
            Set.of(
                workflowRow(
                    SYNC_WORKFLOW_UUID, false, Map.of("slack", EXISTING_TARGET_CONNECTION_ID),
                    Map.of("apiKey", "production-key"))));

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.of(existingTarget));
        when(automationProjectConfigurationService.getConfiguration(TARGET_ID)).thenReturn(updatedTarget);

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(result.created()).isFalse();
        assertThat(result.targetId()).isEqualTo(TARGET_ID);
        assertThat(result.unresolvedConnectionIds()).isEmpty();

        InOrder facadeOrder = inOrder(automationProjectConfigurationFacade);

        facadeOrder.verify(automationProjectConfigurationFacade)
            .validateVersionChange(TARGET_ID, 2);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfiguration(TARGET_ID, "Orders", PERMISSION_EXPRESSION, 2, true, false);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(
                TARGET_ID, SYNC_WORKFLOW_UUID, Map.of("slack", EXISTING_TARGET_CONNECTION_ID),
                Map.of("apiKey", "production-key"));
        facadeOrder.verify(automationProjectConfigurationFacade)
            .enableConfigurationWorkflow(TARGET_ID, NOTIFY_WORKFLOW_UUID, false);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(TARGET_ID, NOTIFY_WORKFLOW_UUID, Map.of(), Map.of());

        verifyNoMoreInteractions(automationProjectConfigurationFacade);
        verify(automationProjectConfigurationService, never()).create(
            anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyBoolean(), anyBoolean(), any(UUID.class));
    }

    @Test
    void testPreviewUpdateListsOnlyInputsTheTargetLacks() {
        AutomationProjectConfiguration source = sourceConfiguration(2);
        AutomationProjectConfiguration existingTarget = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders Production", 1, true,
            Set.of(
                workflowRow(
                    SYNC_WORKFLOW_UUID, false, Map.of("slack", EXISTING_TARGET_CONNECTION_ID),
                    Map.of("apiKey", "production-key"))));

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.of(existingTarget));

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.existingTargetId()).isEqualTo(TARGET_ID);
        assertThat(preview.existingTargetName()).isEqualTo("Orders Production");
        assertThat(preview.warnings()).containsExactlyInAnyOrder(
            "Workflow \"Sync Orders\" input \"region\" needs a value in Production.",
            "Workflow \"Notify\" input \"recipient\" needs a value in Production.");

        PromotionConnectionMapping connectionMapping = preview.connections()
            .getFirst();

        assertThat(connectionMapping.suggestedTargetConnectionId()).isEqualTo(EXISTING_TARGET_CONNECTION_ID);
    }

    @Test
    void testPromoteUpdateSurfacesFacadeRefusalUnchanged() {
        AutomationProjectConfiguration source = sourceConfiguration(1);
        AutomationProjectConfiguration existingTarget = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders", 2, true, Set.of());

        ConfigurationException refusal = new ConfigurationException(
            "Version 1 lacks workflows connected users still use: Notify.",
            AutomationProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS);

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.of(existingTarget));
        doThrow(refusal).when(automationProjectConfigurationFacade)
            .updateConfiguration(TARGET_ID, "Orders", PERMISSION_EXPRESSION, 1, true, false);

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isSameAs(refusal);

        verify(automationProjectConfigurationFacade, never())
            .updateConfigurationWorkflow(anyLong(), anyString(), anyMap(), anyMap());
        verify(automationProjectConfigurationFacade, never())
            .enableConfigurationWorkflow(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testPromoteUpdateSurfacesWorkflowRefusalForMissingInternalOnlyInput() {
        AutomationProjectConfiguration source = sourceConfiguration(
            2, workflowRow(PROVISION_WORKFLOW_UUID, true, Map.of(), Map.of("accountId", "development-account")));
        AutomationProjectConfiguration existingTarget = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders", 2, false,
            Set.of(workflowRow(PROVISION_WORKFLOW_UUID, true, Map.of(), Map.of())));

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 2))
            .thenReturn(List.of(projectWorkflow(2, "workflow-p-v2", PROVISION_WORKFLOW_UUID)));
        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.of(existingTarget));

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "Workflow \"Provision Account\" needs a value for its internal-only input \"accountId\" in Production; " +
                "the promotion is refused until it is filled there.");
        assertThat(preview.warnings()).noneSatisfy(
            warning -> assertThat(warning).contains("is created disabled until"));

        ConfigurationException refusal = new ConfigurationException(
            "Workflow Provision Account needs a value for the internal-only input accountId",
            AutomationProjectConfigurationErrorType.INTERNAL_ONLY_INPUT_MISSING);

        when(automationProjectConfigurationService.getConfiguration(TARGET_ID))
            .thenReturn(
                configuration(
                    TARGET_ID, PRODUCTION_ID, "Orders", 2, false,
                    Set.of(workflowRow(PROVISION_WORKFLOW_UUID, true, Map.of(), Map.of()))));
        doThrow(refusal).when(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(TARGET_ID, PROVISION_WORKFLOW_UUID, Map.of(), Map.of());

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isSameAs(refusal);

        verify(automationProjectConfigurationFacade, never())
            .enableConfigurationWorkflow(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testPromoteUpdateDisablesNewWorkflowNeedingInternalOnlyInput() {
        AutomationProjectConfiguration source = sourceConfiguration(
            2, workflowRow(PROVISION_WORKFLOW_UUID, true, Map.of(), Map.of("accountId", "development-account")));
        AutomationProjectConfiguration existingTarget = configuration(
            TARGET_ID, PRODUCTION_ID, "Orders", 1, false, Set.of());

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, 2))
            .thenReturn(List.of(projectWorkflow(2, "workflow-p-v2", PROVISION_WORKFLOW_UUID)));
        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.of(existingTarget));

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "Workflow \"Provision Account\" is created disabled until its internal-only input \"accountId\" is " +
                "filled in Production.");
        assertThat(preview.warnings()).noneSatisfy(
            warning -> assertThat(warning).contains("the promotion is refused"));

        when(automationProjectConfigurationService.getConfiguration(TARGET_ID))
            .thenReturn(configuration(TARGET_ID, PRODUCTION_ID, "Orders", 2, false, Set.of()));

        EnvironmentPromotionResult result = contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null);

        assertThat(result.created()).isFalse();

        InOrder facadeOrder = inOrder(automationProjectConfigurationFacade);

        facadeOrder.verify(automationProjectConfigurationFacade, times(2))
            .validateVersionChange(TARGET_ID, 2);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfiguration(TARGET_ID, "Orders", PERMISSION_EXPRESSION, 2, false, false);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .enableConfigurationWorkflow(TARGET_ID, PROVISION_WORKFLOW_UUID, false);
        facadeOrder.verify(automationProjectConfigurationFacade)
            .updateConfigurationWorkflow(TARGET_ID, PROVISION_WORKFLOW_UUID, Map.of(), Map.of());

        verifyNoMoreInteractions(automationProjectConfigurationFacade);
    }

    @Test
    void testPreviewReportsNameClashAndPromoteRefuses() {
        AutomationProjectConfiguration source = sourceConfiguration(2);
        AutomationProjectConfiguration clashing = configuration(
            300L, PRODUCTION_ID, " orders ", 2, true, Set.of());

        clashing.setUuid(UUID.randomUUID());

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.empty());
        when(automationProjectConfigurationService.getConfigurations(PROJECT_ID, PRODUCTION_ID))
            .thenReturn(List.of(clashing));

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.warnings()).contains(
            "An automation configuration named \"Orders\" already exists in Production; rename one of them " +
                "before promoting.");

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT.getErrorKey());

        verify(automationProjectConfigurationService, never()).create(
            anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyBoolean(), anyBoolean(), any(UUID.class));
        verifyNoInteractions(automationProjectConfigurationFacade);
    }

    @Test
    void testPreviewListsInputsNeedingValuesAndEmbeddedCandidates() {
        AutomationProjectConfiguration source = sourceConfiguration(2);

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);
        when(automationProjectConfigurationService.fetchConfiguration(CONFIGURATION_UUID, PRODUCTION_ID))
            .thenReturn(Optional.empty());

        EnvironmentPromotionPreview preview = contributor.preview(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION);

        assertThat(preview.resourceType()).isEqualTo("AUTOMATION_CONFIGURATION");
        assertThat(preview.existingTargetId()).isNull();
        assertThat(preview.warnings()).containsExactlyInAnyOrder(
            "The promoted automation configuration is created disabled; fill in its inputs in Production before " +
                "enabling it.",
            "Workflow \"Sync Orders\" input \"apiKey\" needs a value in Production.",
            "Workflow \"Sync Orders\" input \"region\" needs a value in Production.",
            "Workflow \"Notify\" input \"recipient\" needs a value in Production.");

        assertThat(preview.connections()).hasSize(1);

        PromotionConnectionMapping connectionMapping = preview.connections()
            .getFirst();

        assertThat(connectionMapping.sourceConnectionId()).isEqualTo(SOURCE_CONNECTION_ID);
        assertThat(connectionMapping.componentName()).isEqualTo("slack");
        assertThat(connectionMapping.usedBy()).containsExactly("Sync Orders › slack");
        assertThat(connectionMapping.suggestedTargetConnectionId()).isEqualTo(TARGET_CONNECTION_ID);
        assertThat(connectionMapping.targetCandidates()).containsExactly(
            new PromotionConnectionCandidate(TARGET_CONNECTION_ID, "Slack Dev"),
            new PromotionConnectionCandidate(53L, "Slack Other"));
    }

    @Test
    void testPromoteRejectsMappingToNonCandidateConnection() {
        AutomationProjectConfiguration source = sourceConfiguration(2);

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);

        assertThatThrownBy(() -> contributor.promote(
            SOURCE_ID, Environment.DEVELOPMENT, Environment.PRODUCTION, Map.of(SOURCE_CONNECTION_ID, 99L), null))
                .isInstanceOf(ConfigurationException.class)
                .extracting("errorKey")
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        verify(automationProjectConfigurationService, never()).create(
            anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyBoolean(), anyBoolean(), any(UUID.class));
        verifyNoInteractions(automationProjectConfigurationFacade);
    }

    @Test
    void testPreviewRejectsSourceOutsideTheStatedEnvironment() {
        AutomationProjectConfiguration source = sourceConfiguration(2);

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);

        assertThatThrownBy(() -> contributor.preview(SOURCE_ID, Environment.STAGING, Environment.PRODUCTION))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorKey")
            .isEqualTo(EnvironmentPromotionErrorType.SOURCE_ENVIRONMENT_MISMATCH.getErrorKey());
    }

    @Test
    void testPreviewRejectsSameEnvironment() {
        AutomationProjectConfiguration source = sourceConfiguration(2);

        when(automationProjectConfigurationService.getConfiguration(SOURCE_ID)).thenReturn(source);

        assertThatThrownBy(() -> contributor.preview(SOURCE_ID, Environment.DEVELOPMENT, Environment.DEVELOPMENT))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorKey")
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());
    }

    private static AutomationProjectConfiguration configuration(
        long id, long environmentId, String name, int projectVersion, boolean enabled,
        Set<AutomationProjectConfigurationWorkflow> workflows) {

        AutomationProjectConfiguration configuration = new AutomationProjectConfiguration(
            PROJECT_ID, environmentId, name, PERMISSION_EXPRESSION, projectVersion, enabled, true);

        ReflectionTestUtils.setField(configuration, "id", id);

        configuration.setUuid(CONFIGURATION_UUID);
        configuration.setWorkflows(workflows);

        return configuration;
    }

    private static Connection connection(long id, String name) {
        return Connection.builder()
            .componentName("slack")
            .connectionVersion(1)
            .id(id)
            .name(name)
            .type(PlatformType.EMBEDDED)
            .build();
    }

    private static String internalOnlyInput(String name, boolean required) {
        return ("{\"name\": \"%s\", \"label\": \"%s\", \"type\": \"string\", \"required\": %s, " +
            "\"internalOnly\": true}").formatted(name, name, required);
    }

    private static ProjectWorkflow projectWorkflow(int projectVersion, String workflowId, String workflowUuid) {
        return new ProjectWorkflow(PROJECT_ID, projectVersion, workflowId, UUID.fromString(workflowUuid));
    }

    private static AutomationProjectConfiguration sourceConfiguration(
        int projectVersion, AutomationProjectConfigurationWorkflow... extraWorkflowRows) {

        AutomationProjectConfiguration source = configuration(
            SOURCE_ID, Environment.DEVELOPMENT.ordinal(), "Orders", projectVersion, true, Set.of());

        source.setAutomationHubVisible(false);

        Set<AutomationProjectConfigurationWorkflow> workflowRows = new HashSet<>(
            Set.of(
                workflowRow(
                    SYNC_WORKFLOW_UUID, true, Map.of("slack", SOURCE_CONNECTION_ID),
                    Map.of("apiKey", "development-key", "region", "eu")),
                workflowRow(NOTIFY_WORKFLOW_UUID, false, Map.of(), Map.of("recipient", "dev@example.com"))));

        workflowRows.addAll(List.of(extraWorkflowRows));

        source.setWorkflows(workflowRows);

        return source;
    }

    private static Workflow workflow(String id, String label, String... inputs) {
        return new Workflow(
            id,
            "{\"label\": \"%s\", \"inputs\": [%s], \"triggers\": [], \"tasks\": []}".formatted(
                label, String.join(",", inputs)),
            Workflow.Format.JSON);
    }

    private static AutomationProjectConfigurationWorkflow workflowRow(
        String workflowUuid, boolean enabled, Map<String, Long> providedConnections,
        Map<String, Object> providedInputs) {

        UUID uuid = UUID.fromString(workflowUuid);

        AutomationProjectConfigurationWorkflow workflowRow = new AutomationProjectConfigurationWorkflow(uuid, enabled);

        ReflectionTestUtils.setField(workflowRow, "id", uuid.getLeastSignificantBits());

        workflowRow.setProvidedConnections(providedConnections);
        workflowRow.setProvidedInputs(providedInputs);

        return workflowRow;
    }
}
