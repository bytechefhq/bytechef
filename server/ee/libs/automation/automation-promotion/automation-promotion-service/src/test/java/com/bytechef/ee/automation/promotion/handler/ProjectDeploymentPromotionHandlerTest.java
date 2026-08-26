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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.tag.domain.Tag;
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
class ProjectDeploymentPromotionHandlerTest {

    private static final UUID DEPLOYMENT_UUID = UUID.fromString("0f7a1e2c-1111-4a3b-8c4d-5e6f70819293");
    private static final long PROJECT_ID = 42L;
    private static final long SOURCE_CONNECTION_ID = 11L;
    private static final long SOURCE_ID = 1L;
    private static final long TARGET_CONNECTION_ID = 22L;
    private static final long TARGET_ID = 100L;
    private static final String WORKFLOW_UUID = "9a8b7c6d-2222-4e3f-8a1b-0c9d8e7f6a5b";
    private static final long WORKSPACE_ID = 5L;

    @Mock
    private ConnectionEnvironmentMapper connectionEnvironmentMapper;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Mock
    private ProjectDeploymentPromoter projectDeploymentPromoter;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    private ProjectDeploymentPromotionHandler projectDeploymentPromotionHandler;

    @BeforeEach
    void setUp() {
        projectDeploymentPromotionHandler = new ProjectDeploymentPromotionHandler(
            connectionEnvironmentMapper, connectionService, projectDeploymentFacade, projectDeploymentPromoter,
            projectDeploymentService, projectService);
    }

    @Test
    void testGetResourceTypeIsProjectDeployment() {
        assertThat(projectDeploymentPromotionHandler.getResourceType())
            .isEqualTo(PromotionResourceType.PROJECT_DEPLOYMENT);
    }

    @Test
    void testPreviewOnFreshTargetReportsCreate() {
        ProjectDeployment source = stubSource("billing");

        stubNoExistingTarget();
        stubSourceBindings(source);
        stubSuggestedMappings(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        stubConnectionLookup();

        EnvironmentPromotionPreview preview =
            projectDeploymentPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.resourceType()).isEqualTo(PromotionResourceType.PROJECT_DEPLOYMENT);
        assertThat(preview.sourceEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(preview.existingTargetId()).isNull();
        assertThat(preview.existingTargetName()).isNull();
        assertThat(preview.projects())
            .containsExactly(new PromotionProjectPreview(PROJECT_ID, "Billing", 3, null));
        assertThat(preview.warnings())
            .containsExactly(
                "The promoted deployment is created disabled; enable it after reviewing its connections.");
    }

    @Test
    void testPreviewOnExistingTargetReportsProjectVersionAndExistingBindings() {
        ProjectDeployment source = stubSource("billing");

        ProjectDeployment target = targetProjectDeployment(2);

        stubExistingTarget(target);
        stubSourceBindings(source);
        stubSuggestedMappings(Map.of());

        when(projectDeploymentPromoter.existingTargetBindings(source, target))
            .thenReturn(Map.of(SOURCE_CONNECTION_ID, 33L));
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection()));

        EnvironmentPromotionPreview preview =
            projectDeploymentPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.existingTargetId()).isEqualTo(TARGET_ID);
        assertThat(preview.existingTargetName()).isEqualTo("target-billing");
        assertThat(preview.projects())
            .containsExactly(new PromotionProjectPreview(PROJECT_ID, "Billing", 3, 2));
        assertThat(preview.warnings()).isEmpty();
    }

    @Test
    void testPreviewToTheSourceEnvironmentIsRejected() {
        when(projectDeploymentService.getProjectDeployment(SOURCE_ID)).thenReturn(sourceProjectDeployment("billing"));

        assertThatThrownBy(() -> projectDeploymentPromotionHandler.preview(SOURCE_ID, Environment.DEVELOPMENT))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());
    }

    @Test
    void testPromoteToTheSourceEnvironmentIsRejected() {
        when(projectDeploymentService.getProjectDeployment(SOURCE_ID)).thenReturn(sourceProjectDeployment("billing"));

        assertThatThrownBy(
            () -> projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.DEVELOPMENT, Map.of()))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());

        verifyNoInteractions(projectDeploymentPromoter);
    }

    @Test
    void testPromoteRejectsSyntheticApiCollectionDeployment() {
        assertSyntheticSourceRejected(SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX + "billing");
    }

    @Test
    void testPromoteRejectsSyntheticMcpServerDeployment() {
        assertSyntheticSourceRejected(SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX + "billing");
    }

    @Test
    void testPromoteRejectsSyntheticA2aServerDeployment() {
        assertSyntheticSourceRejected(SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX + "billing");
    }

    private void assertSyntheticSourceRejected(String syntheticName) {
        when(projectDeploymentService.getProjectDeployment(SOURCE_ID))
            .thenReturn(sourceProjectDeployment(syntheticName));

        assertThatThrownBy(
            () -> projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of()))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE.getErrorKey());

        verifyNoInteractions(projectDeploymentPromoter);
    }

    @Test
    void testPromoteCreatesTargetDeploymentDisabledWithSourceUuid() {
        ProjectDeployment source = stubSource("billing");

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        stubNoExistingTarget();
        stubNoNameConflict();
        stubTargetCreation();
        stubSync(List.of(SOURCE_CONNECTION_ID));

        EnvironmentPromotionResult result =
            projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        ArgumentCaptor<ProjectDeployment> createdCaptor = ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentFacade).createProjectDeployment(
            createdCaptor.capture(), eq(List.<ProjectDeploymentWorkflow>of()), eq(List.<Tag>of()));

        ProjectDeployment created = createdCaptor.getValue();

        assertThat(created.getId()).isNull();
        assertThat(created.getUuid()).isEqualTo(DEPLOYMENT_UUID);
        assertThat(created.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(created.getName()).isEqualTo("billing");
        assertThat(created.getDescription()).isEqualTo("Billing deployment");
        assertThat(created.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(created.getProjectVersion()).isEqualTo(3);
        assertThat(created.isEnabled()).isFalse();

        verify(projectDeploymentPromoter).sync(
            eq(source), any(ProjectDeployment.class), eq(Map.of()),
            eq(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID)), eq(true));

        assertThat(result).isEqualTo(
            new EnvironmentPromotionResult(TARGET_ID, true, null, List.of(SOURCE_CONNECTION_ID)));
    }

    @Test
    void testPromoteUpdatesExistingTargetWithoutTouchingEnvironmentLocalFields() {
        ProjectDeployment source = stubSource("billing");

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());

        ProjectDeployment target = targetProjectDeployment(2);

        target.setName("target-owned-name");
        target.setDescription("target-owned-description");
        target.setEnabled(true);

        stubExistingTarget(target);
        stubSync(List.of());

        EnvironmentPromotionResult result =
            projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(projectDeploymentPromoter).sync(source, target, Map.of(), Map.of(), false);
        verify(projectDeploymentFacade, never()).createProjectDeployment(
            any(ProjectDeployment.class), eq(List.<ProjectDeploymentWorkflow>of()), eq(List.<Tag>of()));

        // Spec §4 ⚑3: name, description and the enabled flag are environment-local. The handler never calls a
        // setter for any of them on the update path, so the target entity sync() was handed still carries whatever
        // the target environment itself chose — not the source's values.
        assertThat(target.getName()).isEqualTo("target-owned-name");
        assertThat(target.getDescription()).isEqualTo("target-owned-description");
        assertThat(target.isEnabled()).isTrue();

        assertThat(result).isEqualTo(new EnvironmentPromotionResult(TARGET_ID, false, null, List.of()));
    }

    @Test
    void testPromoteValidatesPromotableBeforeAnyWrite() {
        ProjectDeployment source = stubSource("billing");

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubNoNameConflict();
        stubTargetCreation();
        stubSync(List.of());

        projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        InOrder inOrder = inOrder(projectDeploymentPromoter, projectDeploymentFacade);

        inOrder.verify(projectDeploymentPromoter)
            .validatePromotable(PROJECT_ID, 3);
        inOrder.verify(projectDeploymentFacade)
            .createProjectDeployment(any(ProjectDeployment.class), anyList(), anyList());
        inOrder.verify(projectDeploymentPromoter)
            .sync(any(), any(), anyMap(), anyMap(), eq(true));
    }

    @Test
    void testPromoteRejectsTargetNameConflictBeforeCreating() {
        ProjectDeployment source = stubSource("billing");

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());
        stubNoExistingTarget();

        ProjectDeployment conflictingProjectDeployment = new ProjectDeployment();

        conflictingProjectDeployment.setId(999L);
        conflictingProjectDeployment.setName("billing");
        conflictingProjectDeployment.setProjectId(PROJECT_ID);
        conflictingProjectDeployment.setEnvironment(Environment.STAGING);
        conflictingProjectDeployment.setUuid(UUID.randomUUID());

        when(projectDeploymentService.getProjectDeployments(null, Environment.STAGING, PROJECT_ID, null, null))
            .thenReturn(List.of(conflictingProjectDeployment));

        assertThatThrownBy(
            () -> projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of()))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT.getErrorKey());

        verify(projectDeploymentFacade, never()).createProjectDeployment(
            any(ProjectDeployment.class), anyList(), anyList());
    }

    @Test
    void testPromoteRejectsConnectionMappingForConnectionTheSourceDoesNotUse() {
        ProjectDeployment source = stubSource("billing");

        stubSourceBindings(source);

        assertThatThrownBy(
            () -> projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of(999L, 22L)))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        // ConnectionEnvironmentMapper#validate constrains the TARGET side only, so a smuggled source id must be
        // rejected here rather than handed to it.
        verifyNoInteractions(connectionEnvironmentMapper);
        verify(projectDeploymentFacade, never()).createProjectDeployment(
            any(ProjectDeployment.class), anyList(), anyList());
    }

    @Test
    void testPromoteReportsUnresolvedConnectionsWithoutFailing() {
        ProjectDeployment source = stubSource("billing");

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubNoNameConflict();
        stubTargetCreation();
        stubSync(List.of(SOURCE_CONNECTION_ID));

        EnvironmentPromotionResult result =
            projectDeploymentPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        assertThat(result.unresolvedConnectionIds()).containsExactly(SOURCE_CONNECTION_ID);
        assertThat(result.created()).isTrue();
    }

    private void stubConnectionLookup() {
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID))).thenReturn(List.of(connection()));
    }

    private void stubExistingTarget(ProjectDeployment target) {
        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_UUID, Environment.STAGING))
            .thenReturn(Optional.of(target));
    }

    private void stubMappings(Map<Long, Long> requestedMappings, Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.validate(anyLong(), any(Environment.class), anyMap()))
            .thenReturn(requestedMappings);

        stubSuggestedMappings(suggestedMappings);
    }

    private void stubNoExistingTarget() {
        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_UUID, Environment.STAGING))
            .thenReturn(Optional.empty());
    }

    private void stubNoNameConflict() {
        when(projectDeploymentService.getProjectDeployments(null, Environment.STAGING, PROJECT_ID, null, null))
            .thenReturn(List.of());
    }

    private ProjectDeployment stubSource(String name) {
        ProjectDeployment source = sourceProjectDeployment(name);

        when(projectDeploymentService.getProjectDeployment(SOURCE_ID)).thenReturn(source);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project());

        return source;
    }

    private void stubSourceBindings(ProjectDeployment source) {
        when(projectDeploymentPromoter.collectSourceBindings(source))
            .thenReturn(
                List.of(new SourceBinding(WORKFLOW_UUID, "Orders", "googleSheets_1", "connectionId",
                    SOURCE_CONNECTION_ID)));
    }

    private void stubSuggestedMappings(Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.suggest(anyLong(), anySet(), any(Environment.class)))
            .thenReturn(suggestedMappings);
    }

    private void stubSync(List<Long> unresolvedConnectionIds) {
        when(projectDeploymentPromoter.sync(any(), any(), anyMap(), anyMap(), any(Boolean.class)))
            .thenReturn(new SyncResult(Map.of(), unresolvedConnectionIds, List.of()));
    }

    private void stubTargetCreation() {
        ProjectDeployment createdProjectDeployment = new ProjectDeployment();

        createdProjectDeployment.setId(TARGET_ID);
        createdProjectDeployment.setUuid(DEPLOYMENT_UUID);
        createdProjectDeployment.setProjectId(PROJECT_ID);
        createdProjectDeployment.setProjectVersion(3);
        createdProjectDeployment.setEnvironment(Environment.STAGING);
        createdProjectDeployment.setName("billing");

        when(
            projectDeploymentFacade.createProjectDeployment(
                any(ProjectDeployment.class), eq(List.<ProjectDeploymentWorkflow>of()), eq(List.<Tag>of())))
                    .thenReturn(TARGET_ID);
        when(projectDeploymentService.getProjectDeployment(TARGET_ID)).thenReturn(createdProjectDeployment);
    }

    private static Connection connection() {
        Connection connection = new Connection();

        connection.setId(SOURCE_CONNECTION_ID);
        connection.setName("Sheets");
        connection.setComponentName("googleSheets");
        connection.setConnectionVersion(1);

        return connection;
    }

    private static Project project() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setName("Billing");
        project.setWorkspaceId(WORKSPACE_ID);

        return project;
    }

    private static ProjectDeployment sourceProjectDeployment(String name) {
        ProjectDeployment source = new ProjectDeployment();

        source.setId(SOURCE_ID);
        source.setUuid(DEPLOYMENT_UUID);
        source.setProjectId(PROJECT_ID);
        source.setProjectVersion(3);
        source.setEnvironment(Environment.DEVELOPMENT);
        source.setName(name);
        source.setDescription("Billing deployment");
        source.setEnabled(true);

        return source;
    }

    private static ProjectDeployment targetProjectDeployment(int projectVersion) {
        ProjectDeployment target = new ProjectDeployment();

        target.setId(TARGET_ID);
        target.setUuid(DEPLOYMENT_UUID);
        target.setProjectId(PROJECT_ID);
        target.setProjectVersion(projectVersion);
        target.setEnvironment(Environment.STAGING);
        target.setName("target-billing");

        return target;
    }
}
