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
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ProjectDeploymentEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentOwnershipResolver;
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
 * The deployment guards on {@code ProjectDeploymentFacadeImpl} name the {@code 'ProjectDeployment'} resource token,
 * which is only a check because {@code ProjectDeploymentOwnershipResolver} is registered for it — the registry lookup
 * in {@code hasResourceScope} fails closed, so an unregistered token denies every non-tenant-admin instead of checking
 * anything. Nothing else in the suite would notice that: the guard tests mock {@code PermissionService} itself.
 * <p>
 * Registering the ownership resolver also brings the pre-existing {@code ProjectDeploymentEnvironmentResolver} into
 * play, which is what makes a by-id deployment check ask about the deployment's own environment rather than unioning
 * every environment the caller can reach.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentResourceScopeTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 42L;

    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);
    private final ProjectDeploymentRepository projectDeploymentRepository = mock(ProjectDeploymentRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
    private final WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

    @Test
    void testDeploymentScopeIsGrantedFromTheRoleHeldInTheDeploymentsEnvironment() {
        givenDeploymentInProductionOwnedByWorkspace();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        assertThat(service().hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT")).isTrue();
    }

    @Test
    void testDeploymentScopeIsRefusedWhenTheRoleIsHeldOnlyInAnotherEnvironment() {
        givenDeploymentInProductionOwnedByWorkspace();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        assertThat(service().hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT")).isFalse();
    }

    /**
     * Deleting {@code ProjectDeploymentOwnershipResolver} would not break any guard test, because those mock
     * {@code PermissionService}. It would silently turn every deployment guard into a lockout: this is the assertion
     * that fails instead.
     */
    @Test
    void testDeploymentScopeIsRefusedForEveryoneWithoutTheOwnershipResolver() {
        givenDeploymentInProductionOwnedByWorkspace();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        PermissionServiceImpl serviceWithoutResolver = service(List.of(), environmentResolvers());

        assertThat(serviceWithoutResolver.hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment",
            "DEPLOYMENT_EDIT")).isFalse();
    }

    private void givenDeploymentInProductionOwnedByWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(projectRepository.findByProjectDeploymentId(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(project));
        when(projectDeploymentRepository.findById(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(projectDeployment));
    }

    private PermissionServiceImpl service() {
        return service(
            List.of(new ProjectDeploymentOwnershipResolver(projectRepository)), environmentResolvers());
    }

    private PermissionServiceImpl service(
        List<ResourceOwnershipResolver> resourceOwnershipResolvers,
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers) {

        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, resourceOwnershipResolvers, resourceEnvironmentResolvers);
    }

    private List<ResourceEnvironmentResolver> environmentResolvers() {
        return List.of(new ProjectDeploymentEnvironmentResolver(projectDeploymentRepository));
    }

}
