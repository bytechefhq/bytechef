/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class A2aServerPromotionHandlerTest {

    private static final long PROJECT_ID = 42L;
    private static final String PUBLIC_URL = "https://app.bytechef.test";
    private static final String SECRET_KEY = "secret-key";
    private static final String SERVER_UUID = "5f2c9a10-4444-4b1c-9d2e-3f4a5b6c7d8e";
    private static final long SOURCE_A2A_PROJECT_ID = 10L;
    private static final Map<String, Object> SOURCE_PARAMETERS =
        Map.of("skillName", "getOrders", "skillDescription", "Fetch orders", "skillTags", List.of("orders"));
    private static final long SOURCE_CONNECTION_ID = 11L;
    private static final long SOURCE_ID = 1L;
    private static final long SOURCE_PROJECT_DEPLOYMENT_ID = 200L;
    private static final long SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID = 210L;
    private static final int SOURCE_PROJECT_VERSION = 3;
    private static final String SOURCE_SERVER_NAME = "Orders A2A";
    private static final String SOURCE_SERVER_DESCRIPTION = "Order agent";
    private static final String SOURCE_WORKFLOW_ID = "wf-source-v3";
    private static final long TARGET_A2A_PROJECT_ID = 20L;
    private static final long TARGET_CONNECTION_ID = 22L;
    private static final long TARGET_ID = 100L;
    private static final long TARGET_PROJECT_DEPLOYMENT_ID = 300L;
    private static final long TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID = 310L;
    private static final long TARGET_A2A_PROJECT_WORKFLOW_ID = 320L;
    private static final int TARGET_PROJECT_VERSION = 2;
    private static final String TARGET_WORKFLOW_ID = "wf-target-v2";
    private static final String WORKFLOW_UUID = "9a8b7c6d-2222-4e3f-8a1b-0c9d8e7f6a5b";
    private static final long WORKSPACE_ID = 5L;

    @Mock
    private A2aProjectFacade a2aProjectFacade;

    @Mock
    private A2aProjectService a2aProjectService;

    @Mock
    private A2aProjectWorkflowService a2aProjectWorkflowService;

    @Mock
    private A2aServerService a2aServerService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ConnectionEnvironmentMapper connectionEnvironmentMapper;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentPromoter projectDeploymentPromoter;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    private A2aServerPromotionHandler a2aServerPromotionHandler;

    @BeforeEach
    void setUp() {
        when(applicationProperties.getPublicUrl()).thenReturn(PUBLIC_URL);

        // The project promoter and the reconciler it drives are exercised for real rather than mocked: the
        // delete-then-sync-then-create ordering they own is the part of this handler most likely to break, and
        // mocks would assert nothing about it.
        a2aServerPromotionHandler = new A2aServerPromotionHandler(
            a2aProjectFacade, a2aProjectService, a2aProjectWorkflowService, a2aServerService, applicationProperties,
            connectionEnvironmentMapper, connectionService, projectDeploymentPromoter, projectDeploymentService,
            projectDeploymentWorkflowService, projectService,
            new ServerProjectPromoter(
                projectDeploymentPromoter,
                new ProjectWorkflowMappingReconciler(projectDeploymentWorkflowService, projectWorkflowService)));
    }

    @Test
    void testGetResourceTypeIsA2aServer() {
        assertThat(a2aServerPromotionHandler.getResourceType()).isEqualTo(PromotionResourceType.A2A_SERVER);
    }

    @Test
    void testPreviewOnFreshTargetReportsCreate() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubNoExistingTarget();
        stubSuggestedMappings(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));

        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Sheets")));

        EnvironmentPromotionPreview preview = a2aServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.resourceType()).isEqualTo(PromotionResourceType.A2A_SERVER);
        assertThat(preview.sourceEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(preview.existingTargetId()).isNull();
        assertThat(preview.existingTargetName()).isNull();
        assertThat(preview.projects())
            .containsExactly(new PromotionProjectPreview(PROJECT_ID, "Billing", SOURCE_PROJECT_VERSION, null));
        assertThat(preview.warnings())
            .contains(
                "The promoted server is created disabled; enable it after reviewing its connections.",
                "A new server URL and secret key will be generated for the promoted server.");
        assertThat(preview.connections())
            .containsExactly(
                new PromotionConnectionMapping(
                    SOURCE_CONNECTION_ID, "Sheets", "googleSheets", 1, TARGET_CONNECTION_ID,
                    List.of("Orders › googleSheets_1")));
    }

    @Test
    void testPreviewOnExistingTargetReportsTheTargetProjectVersion() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubExistingTarget();
        stubTargetProjects(targetA2aProject(TARGET_A2A_PROJECT_ID));
        stubSuggestedMappings(Map.of());

        when(projectDeploymentPromoter.existingTargetBindings(any(), any()))
            .thenReturn(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Sheets")));

        EnvironmentPromotionPreview preview = a2aServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.existingTargetId()).isEqualTo(TARGET_ID);
        assertThat(preview.existingTargetName()).isEqualTo("Target-owned name");
        assertThat(preview.projects())
            .containsExactly(
                new PromotionProjectPreview(PROJECT_ID, "Billing", SOURCE_PROJECT_VERSION, TARGET_PROJECT_VERSION));

        // What the target already wired outranks the suggestion, so the dialog does not propose re-pointing it.
        assertThat(preview.connections())
            .extracting(PromotionConnectionMapping::suggestedTargetConnectionId)
            .containsExactly(TARGET_CONNECTION_ID);
    }

    @Test
    void testPreviewWarnsWhenTheSourceExposesOneProjectTwice() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID), sourceA2aProject(SOURCE_A2A_PROJECT_ID + 1));
        stubSourceBindings();
        stubProject();
        stubNoExistingTarget();
        stubSuggestedMappings(Map.of());

        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Sheets")));

        EnvironmentPromotionPreview preview = a2aServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.warnings())
            .contains(
                "The source A2A server exposes project 42 more than once; entries are matched in ascending id order");
        assertThat(preview.projects()).hasSize(2);
    }

    @Test
    void testPreviewWarnsWhenTheTargetExposesOneProjectTwice() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubExistingTarget();
        stubTargetProjects(targetA2aProject(TARGET_A2A_PROJECT_ID), targetA2aProject(TARGET_A2A_PROJECT_ID + 1));
        stubSuggestedMappings(Map.of());

        when(projectDeploymentPromoter.existingTargetBindings(any(), any())).thenReturn(Map.of());
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Sheets")));

        EnvironmentPromotionPreview preview = a2aServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.warnings())
            .contains(
                "The target A2A server exposes project 42 more than once; entries are matched in ascending id order");
    }

    @Test
    void testPreviewRejectsPromotionIntoTheSameEnvironment() {
        when(a2aServerService.getA2aServer(SOURCE_ID)).thenReturn(sourceA2aServer());

        assertThatThrownBy(() -> a2aServerPromotionHandler.preview(SOURCE_ID, Environment.DEVELOPMENT))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());
    }

    @Test
    void testPromoteCreatesTheTargetServerDisabledWithTheSourceLineageUuid() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubConnectionMappings(Map.of(), Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubTargetProjectCreation();
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubTargetMappings(targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        EnvironmentPromotionResult result =
            a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        ArgumentCaptor<A2aServer> a2aServerArgumentCaptor = ArgumentCaptor.forClass(A2aServer.class);

        verify(a2aServerService).create(a2aServerArgumentCaptor.capture());

        A2aServer createdA2aServer = a2aServerArgumentCaptor.getValue();

        assertThat(createdA2aServer.getName()).isEqualTo(SOURCE_SERVER_NAME);
        assertThat(createdA2aServer.getDescription()).isEqualTo(SOURCE_SERVER_DESCRIPTION);
        assertThat(createdA2aServer.isEnabled()).isFalse();
        assertThat(createdA2aServer.isAuthenticationRequired()).isTrue();
        assertThat(createdA2aServer.getUuid()).isEqualTo(UUID.fromString(SERVER_UUID));
        assertThat(createdA2aServer.getEnvironment()).isEqualTo(Environment.STAGING);

        verify(a2aProjectFacade).createA2aProject(
            TARGET_ID, PROJECT_ID, SOURCE_PROJECT_VERSION, List.of(SOURCE_WORKFLOW_ID));
        verify(projectDeploymentPromoter).sync(
            any(), any(), eq(Map.of()), eq(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID)), eq(true));
        verify(a2aProjectWorkflowService).updateParameters(TARGET_A2A_PROJECT_WORKFLOW_ID, SOURCE_PARAMETERS);
        verify(a2aServerService, never()).update(any(A2aServer.class));

        assertThat(result.created()).isTrue();
        assertThat(result.targetId()).isEqualTo(TARGET_ID);
        assertThat(result.targetUrl())
            .isEqualTo(PUBLIC_URL + "/api/automation/a2a/" + SECRET_KEY + "/.well-known/agent-card.json");
    }

    @Test
    void testPromoteRejectsConnectionMappingForConnectionTheSourceDoesNotUse() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();

        assertThatThrownBy(
            () -> a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of(999L, 22L)))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        // Pins the ORDER, not merely the outcome: the source-side scope check must reject before the mapper's
        // target-side validation ever runs, so the mapper must see no interaction at all.
        verifyNoInteractions(connectionEnvironmentMapper);
        verify(a2aServerService, never()).create(any(A2aServer.class));
        verify(a2aServerService, never()).fetchA2aServer(any(UUID.class), any(Environment.class));
    }

    @Test
    void testPromoteUpdateLeavesTheTargetServerRowAlone() {
        stubUpdatePath();
        stubTargetMappings(targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        EnvironmentPromotionResult result =
            a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        // §4 ⚑3: what the target environment owns (name, description, enabled, authenticationRequired, secretKey)
        // must never be overwritten on a re-promotion. The handler never calls create or update on the server row
        // at all when it already exists.
        verify(a2aServerService, never()).update(any(A2aServer.class));
        verify(a2aServerService, never()).create(any(A2aServer.class));
        verify(projectDeploymentPromoter).sync(any(), any(), anyMap(), anyMap(), eq(false));

        assertThat(result.created()).isFalse();
        assertThat(result.targetId()).isEqualTo(TARGET_ID);
        assertThat(result.targetUrl())
            .isEqualTo(PUBLIC_URL + "/api/automation/a2a/" + SECRET_KEY + "/.well-known/agent-card.json");
    }

    /**
     * {@code a2a_project_workflow} holds a foreign key to {@code project_deployment_workflow} and the sync deletes the
     * deployment-workflow rows the source no longer exposes, so the child has to go first. The selection-level facade
     * method must not be involved at all — it reconciles deployment-workflow rows itself, blind to the sync's mapping.
     */
    @Test
    void testPromoteUpdateDeletesStaleMappingRowsBeforeSyncingAndNeverCallsUpdateA2aProject() {
        stubUpdatePath();
        stubTargetMappings(
            targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID),
            targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID + 500,
                TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID + 1));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        InOrder inOrder = inOrder(a2aProjectWorkflowService, projectDeploymentPromoter);

        inOrder.verify(a2aProjectWorkflowService)
            .delete(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID + 500);
        inOrder.verify(projectDeploymentPromoter)
            .sync(any(), any(), anyMap(), anyMap(), eq(false));
        inOrder.verify(a2aProjectWorkflowService)
            .updateParameters(eq(TARGET_A2A_PROJECT_WORKFLOW_ID), anyMap());

        verify(a2aProjectFacade, never()).updateA2aProject(anyLong(), any());
    }

    @Test
    void testPromoteUpdateCreatesAMappingRowForAWorkflowTheTargetDoesNotExposeYet() {
        stubUpdatePath();
        stubTargetMappings();
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        when(a2aProjectWorkflowService.create(TARGET_A2A_PROJECT_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));

        a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(a2aProjectWorkflowService).create(TARGET_A2A_PROJECT_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID);
        verify(a2aProjectWorkflowService).updateParameters(TARGET_A2A_PROJECT_WORKFLOW_ID, SOURCE_PARAMETERS);
    }

    @Test
    void testPromoteUpdateDeletesATargetProjectTheSourceNoLongerExposes() {
        stubSource();
        stubSourceProjects();
        stubConnectionMappings(Map.of(), Map.of());
        stubExistingTarget();
        stubTargetProjects(targetA2aProject(TARGET_A2A_PROJECT_ID));

        a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(a2aProjectFacade).deleteA2aProject(TARGET_A2A_PROJECT_ID);
    }

    @Test
    void testPromoteUpdateCreatesATargetProjectForASourceProjectTheTargetIsMissing() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubConnectionMappings(Map.of(), Map.of());
        stubExistingTarget();
        stubTargetProjects();
        stubTargetProjectCreation();
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubTargetMappings(targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(a2aProjectFacade).createA2aProject(
            TARGET_ID, PROJECT_ID, SOURCE_PROJECT_VERSION, List.of(SOURCE_WORKFLOW_ID));

        // A project the target had to be given is new even though the server itself is not, so it adopts the
        // source's inputs and enabled flags rather than preserving values it never had.
        verify(projectDeploymentPromoter).sync(any(), any(), anyMap(), anyMap(), eq(true));
    }

    @Test
    void testPromoteReportsUnresolvedConnectionsWithoutFailingThePromotion() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubConnectionMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubTargetProjectCreation();
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubTargetMappings(targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(
            Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID),
            List.of(SOURCE_CONNECTION_ID));

        EnvironmentPromotionResult result =
            a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        assertThat(result.unresolvedConnectionIds()).containsExactly(SOURCE_CONNECTION_ID);
        assertThat(result.created()).isTrue();
    }

    @Test
    void testPromoteValidatesEveryProjectBeforeWritingAnything() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubConnectionMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubTargetProjectCreation();
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubTargetMappings(targetA2aProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        a2aServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        InOrder inOrder = inOrder(projectDeploymentPromoter, a2aServerService);

        inOrder.verify(projectDeploymentPromoter)
            .validatePromotable(PROJECT_ID, SOURCE_PROJECT_VERSION);
        inOrder.verify(a2aServerService)
            .create(any(A2aServer.class));
    }

    @Test
    void testPromoteRejectsPromotionIntoTheSameEnvironment() {
        when(a2aServerService.getA2aServer(SOURCE_ID)).thenReturn(sourceA2aServer());

        assertThatThrownBy(() -> a2aServerPromotionHandler.promote(SOURCE_ID, Environment.DEVELOPMENT, Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());

        verifyNoInteractions(a2aProjectFacade);
    }

    /**
     * The whole update-path fixture: one source project on version 3 matched to a target project still on version 2,
     * the two running different per-version workflow ids under one lineage uuid.
     */
    private void stubUpdatePath() {
        stubSource();
        stubSourceProjects(sourceA2aProject(SOURCE_A2A_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubConnectionMappings(Map.of(), Map.of());
        stubExistingTarget();
        stubTargetProjects(targetA2aProject(TARGET_A2A_PROJECT_ID));
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(TARGET_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(TARGET_PROJECT_VERSION, TARGET_WORKFLOW_ID);
    }

    private void stubConnectionMappings(Map<Long, Long> requestedMappings, Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.validate(anyLong(), any(Environment.class), anyMap()))
            .thenReturn(requestedMappings);

        stubSuggestedMappings(suggestedMappings);
    }

    private void stubExistingTarget() {
        when(a2aServerService.fetchA2aServer(UUID.fromString(SERVER_UUID), Environment.STAGING))
            .thenReturn(Optional.of(targetA2aServer()));
    }

    private void stubNoExistingTarget() {
        when(a2aServerService.fetchA2aServer(UUID.fromString(SERVER_UUID), Environment.STAGING))
            .thenReturn(Optional.empty());
    }

    private void stubProject() {
        when(projectService.getProject(PROJECT_ID)).thenReturn(project());
    }

    private void stubProjectWorkflows(int projectVersion, String workflowId) {
        ProjectWorkflow projectWorkflow =
            new ProjectWorkflow(PROJECT_ID, projectVersion, workflowId, UUID.fromString(WORKFLOW_UUID));

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, projectVersion))
            .thenReturn(List.of(projectWorkflow));
    }

    private void stubSource() {
        when(a2aServerService.getA2aServer(SOURCE_ID)).thenReturn(sourceA2aServer());
    }

    private void stubSourceBindings() {
        when(projectDeploymentPromoter.collectSourceBindings(any(ProjectDeployment.class)))
            .thenReturn(
                List.of(
                    new SourceBinding(
                        WORKFLOW_UUID, "Orders", "googleSheets_1", "connectionId", SOURCE_CONNECTION_ID)));
    }

    private void stubSourceProjectDeploymentWorkflows() {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(SOURCE_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(projectDeploymentWorkflow(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, SOURCE_WORKFLOW_ID)));
    }

    private void stubSourceProjects(A2aProject... a2aProjects) {
        when(a2aProjectService.getA2aServerA2aProjects(SOURCE_ID)).thenReturn(List.of(a2aProjects));

        if (a2aProjects.length > 0) {
            when(projectDeploymentService.getProjectDeployment(SOURCE_PROJECT_DEPLOYMENT_ID))
                .thenReturn(
                    projectDeployment(
                        SOURCE_PROJECT_DEPLOYMENT_ID, SOURCE_PROJECT_VERSION, Environment.DEVELOPMENT));
        }

        for (A2aProject a2aProject : a2aProjects) {
            when(a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(a2aProject.getId()))
                .thenReturn(
                    List.of(sourceA2aProjectWorkflow(a2aProject.getId(), SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID)));
        }
    }

    private void stubSuggestedMappings(Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.suggest(anyLong(), anySet(), any(Environment.class)))
            .thenReturn(suggestedMappings);
    }

    private void stubSync(Map<Long, Long> workflowIdMapping, List<Long> unresolvedConnectionIds) {
        when(projectDeploymentPromoter.sync(any(), any(), anyMap(), anyMap(), any(Boolean.class)))
            .thenReturn(new SyncResult(workflowIdMapping, unresolvedConnectionIds, List.of()));
    }

    private void stubTargetMappings(A2aProjectWorkflow... a2aProjectWorkflows) {
        when(a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(TARGET_A2A_PROJECT_ID))
            .thenReturn(List.of(a2aProjectWorkflows));
    }

    private void stubTargetProjectCreation() {
        when(
            a2aProjectFacade.createA2aProject(
                TARGET_ID, PROJECT_ID, SOURCE_PROJECT_VERSION, List.of(SOURCE_WORKFLOW_ID)))
                    .thenReturn(targetA2aProject(TARGET_A2A_PROJECT_ID));
        when(projectDeploymentService.getProjectDeployment(TARGET_PROJECT_DEPLOYMENT_ID))
            .thenReturn(projectDeployment(TARGET_PROJECT_DEPLOYMENT_ID, SOURCE_PROJECT_VERSION, Environment.STAGING));
    }

    private void stubTargetProjectDeploymentWorkflows(String workflowId) {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(TARGET_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(projectDeploymentWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID, workflowId)));
    }

    private void stubTargetProjects(A2aProject... a2aProjects) {
        when(a2aProjectService.getA2aServerA2aProjects(TARGET_ID)).thenReturn(List.of(a2aProjects));

        if (a2aProjects.length > 0) {
            when(projectDeploymentService.getProjectDeployment(TARGET_PROJECT_DEPLOYMENT_ID))
                .thenReturn(
                    projectDeployment(TARGET_PROJECT_DEPLOYMENT_ID, TARGET_PROJECT_VERSION, Environment.STAGING));
        }
    }

    private void stubTargetServerCreation() {
        when(a2aServerService.create(any(A2aServer.class))).thenReturn(targetA2aServer());
    }

    private static Connection connection(long id, String name) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setName(name);
        connection.setComponentName("googleSheets");
        connection.setConnectionVersion(1);

        return connection;
    }

    private static A2aProject a2aProject(long id, long projectDeploymentId) {
        A2aProject a2aProject = new A2aProject(projectDeploymentId, SOURCE_ID);

        a2aProject.setId(id);

        return a2aProject;
    }

    private static Project project() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setName("Billing");
        project.setWorkspaceId(WORKSPACE_ID);

        return project;
    }

    private static ProjectDeployment projectDeployment(long id, int projectVersion, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(environment);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(long id, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static A2aProject sourceA2aProject(long id) {
        return a2aProject(id, SOURCE_PROJECT_DEPLOYMENT_ID);
    }

    private static A2aProjectWorkflow sourceA2aProjectWorkflow(long a2aProjectId, long projectDeploymentWorkflowId) {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(a2aProjectId, projectDeploymentWorkflowId);

        a2aProjectWorkflow.setId(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID + 1000);
        a2aProjectWorkflow.setParameters(SOURCE_PARAMETERS);

        return a2aProjectWorkflow;
    }

    private static A2aServer sourceA2aServer() {
        A2aServer a2aServer = new A2aServer(SOURCE_SERVER_NAME, SOURCE_SERVER_DESCRIPTION, PlatformType.AUTOMATION,
            Environment.DEVELOPMENT);

        a2aServer.setId(SOURCE_ID);
        a2aServer.setUuid(UUID.fromString(SERVER_UUID));
        a2aServer.setAuthenticationRequired(true);

        return a2aServer;
    }

    private static A2aProject targetA2aProject(long id) {
        return a2aProject(id, TARGET_PROJECT_DEPLOYMENT_ID);
    }

    private static A2aProjectWorkflow targetA2aProjectWorkflow(long projectDeploymentWorkflowId) {
        return targetA2aProjectWorkflow(TARGET_A2A_PROJECT_WORKFLOW_ID, projectDeploymentWorkflowId);
    }

    private static A2aProjectWorkflow targetA2aProjectWorkflow(long id, long projectDeploymentWorkflowId) {
        A2aProjectWorkflow a2aProjectWorkflow =
            new A2aProjectWorkflow(TARGET_A2A_PROJECT_ID, projectDeploymentWorkflowId);

        a2aProjectWorkflow.setId(id);

        return a2aProjectWorkflow;
    }

    /**
     * Deliberately named and configured differently from the source, so every update-path test pins that neither name,
     * description, enabled, nor authenticationRequired crosses.
     */
    private static A2aServer targetA2aServer() {
        A2aServer a2aServer =
            new A2aServer("Target-owned name", "Target-owned description", PlatformType.AUTOMATION,
                Environment.STAGING);

        a2aServer.setId(TARGET_ID);
        a2aServer.setUuid(UUID.fromString(SERVER_UUID));
        a2aServer.setSecretKey(SECRET_KEY);
        a2aServer.setEnabled(false);
        a2aServer.setAuthenticationRequired(false);

        return a2aServer;
    }
}
