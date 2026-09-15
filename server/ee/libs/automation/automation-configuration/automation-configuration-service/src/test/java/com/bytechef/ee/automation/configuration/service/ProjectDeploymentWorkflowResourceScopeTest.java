/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ProjectDeploymentWorkflowEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentWorkflowOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@code updateProjectDeploymentWorkflow} names the {@code 'ProjectDeploymentWorkflow'} token because the row it writes
 * is selected by its own id — the deployment id the REST path also carries is caller-supplied independently of it.
 * These assertions prove the token is a real check and not a lockout, and that the environment it is judged in is the
 * one belonging to the row's deployment.
 * <p>
 * The second case is the one that matters for the escalation this token exists to stop: a member who holds
 * {@code DEPLOYMENT_EDIT} in Development must be refused a row whose deployment lives in Production, however the
 * request labels itself.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentWorkflowResourceScopeTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 77L;
    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 42L;

    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);
    private final ProjectDeploymentRepository projectDeploymentRepository = mock(ProjectDeploymentRepository.class);
    private final ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository =
        mock(ProjectDeploymentWorkflowRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
    private final WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

    @Test
    void testDeploymentWorkflowScopeIsGrantedFromTheRoleHeldInTheRowsEnvironment() {
        givenWorkflowRowInAProductionDeployment();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        assertThat(
            service().hasResourceScope(
                PROJECT_DEPLOYMENT_WORKFLOW_ID, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT")).isTrue();
    }

    @Test
    void testDeploymentWorkflowScopeIsRefusedWhenTheRoleIsHeldOnlyInAnotherEnvironment() {
        givenWorkflowRowInAProductionDeployment();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        assertThat(
            service().hasResourceScope(
                PROJECT_DEPLOYMENT_WORKFLOW_ID, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT")).isFalse();
    }

    /**
     * Deleting {@code ProjectDeploymentWorkflowOwnershipResolver} would not break any guard test, because those mock
     * {@code PermissionService}. It would silently turn the guard into a lockout: this is the assertion that fails
     * instead.
     */
    @Test
    void testDeploymentWorkflowScopeIsRefusedForEveryoneWithoutTheOwnershipResolver() {
        givenWorkflowRowInAProductionDeployment();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        PermissionServiceImpl serviceWithoutResolver = service(List.of(), environmentResolvers());

        assertThat(
            serviceWithoutResolver.hasResourceScope(
                PROJECT_DEPLOYMENT_WORKFLOW_ID, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT")).isFalse();
    }

    /**
     * Without the environment resolver the check falls back to the environment-unaware union, so a Development-only
     * editor would pass on a Production row. That is the fallback {@code hasResourceScope} documents, which is why the
     * resolver has to exist for this token rather than being inherited from {@code 'ProjectDeployment'}.
     */
    @Test
    void testDeploymentWorkflowScopeIgnoresTheEnvironmentWithoutTheEnvironmentResolver() {
        givenWorkflowRowInAProductionDeployment();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());

        assertThat(
            service().hasResourceScope(
                PROJECT_DEPLOYMENT_WORKFLOW_ID, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT"))
                    .as("with the environment resolver registered the Production row must be refused")
                    .isFalse();

        PermissionServiceImpl serviceWithoutEnvironmentResolver = service(ownershipResolvers(), List.of());

        assertThat(
            serviceWithoutEnvironmentResolver.hasResourceScope(
                PROJECT_DEPLOYMENT_WORKFLOW_ID, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT"))
                    .as("without it the same caller passes on the environment-unaware union, which is the fallback " +
                        "the resolver exists to avoid")
                    .isTrue();
    }

    private void givenWorkflowRowInAProductionDeployment() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(projectDeploymentWorkflowRepository.findById(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(Optional.of(projectDeploymentWorkflow));
        when(projectRepository.findByProjectDeploymentId(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(project));
        when(projectDeploymentRepository.findById(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(projectDeployment));
    }

    private PermissionServiceImpl service() {
        return service(ownershipResolvers(), environmentResolvers());
    }

    private PermissionServiceImpl service(
        List<ResourceOwnershipResolver> resourceOwnershipResolvers,
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers) {

        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, resourceOwnershipResolvers, resourceEnvironmentResolvers);
    }

    private List<ResourceOwnershipResolver> ownershipResolvers() {
        return List.of(
            new ProjectDeploymentWorkflowOwnershipResolver(projectDeploymentWorkflowRepository, projectRepository));
    }

    private List<ResourceEnvironmentResolver> environmentResolvers() {
        return List.of(
            new ProjectDeploymentWorkflowEnvironmentResolver(
                projectDeploymentRepository, projectDeploymentWorkflowRepository));
    }

}
