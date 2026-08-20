/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.audit.ProjectAuditPublisher;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.facade.ProjectSharingFacade;
import com.bytechef.ee.automation.configuration.facade.ProjectSharingFacadeImpl;
import com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Proves that {@code @PreAuthorize} expressions are actually enforced at runtime by the Spring Security proxy, not
 * merely present in source. Four expression forms are covered: {@code hasPermission(...)},
 * {@code isTenantAdmin() or isCurrentUser(#id)}, the root {@code isResourceOwner(#id, 'Type')} built-in, and the
 * {@code @permissionService.<method>(...)} bean-reference form the two sharing facades use — that last one resolves
 * through the {@code BeanFactoryResolver} that {@code DefaultMethodSecurityExpressionHandler} installs, which nothing
 * else pins.
 *
 * <p>
 * The bean-reference cases call the REAL {@code ProjectSharingFacadeImpl} and {@code WorkspaceConnectionFacadeImpl}
 * rather than a {@code Guarded*} stand-in, so deleting a production annotation turns them red; the stand-ins above only
 * pin the expression forms and are kept honest by {@code PreAuthorizeAnnotationTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PreAuthorizeProxyEnforcementIntTest.Config.class, properties = "bytechef.edition=ee")
class PreAuthorizeProxyEnforcementIntTest {

    private static final long CONNECTION_ID = 5L;
    private static final long GRANTEE_USER_ID = 7L;
    private static final long PROJECT_ID = 3L;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private GuardedProjectMutations guardedProjectMutations;

    @Autowired
    private GuardedProjectFacadeReads guardedProjectFacadeReads;

    @Autowired
    private GuardedResourceOwnerReads guardedResourceOwnerReads;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    @Autowired
    private ProjectSharingFacade projectSharingFacade;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ResourceGrantService resourceGrantService;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Re-established each test so the positive control's specific true-stub does not leak across methods (the
        // PermissionService mock is shared via the cached Spring context). Every gate defaults to denied here.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(anyLong())).thenReturn(false);
        when(permissionService.isResourceOwner(anyString(), anyLong())).thenReturn(false);
        when(permissionService.hasResourceRole(anyLong(), anyString(), anyString())).thenReturn(false);
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScope(anyLong(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScopeForProject(anyLong(), anyString())).thenReturn(false);

        // Collaborators of the two real sharing facades, stubbed so a call that gets PAST the gate reaches a
        // successful body. Re-stubbed per test for the same reason as the gates above: the Spring context, and with
        // it every mock in it, is cached across methods.
        reset(projectService, resourceGrantService, workspaceConnectionService);

        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenReturn(List.of(new WorkspaceConnection(CONNECTION_ID, WORKSPACE_ID)));
        when(resourceGrantService.getGrantedUserIds("Project", PROJECT_ID)).thenReturn(List.of(GRANTEE_USER_ID));
        when(resourceGrantService.getGrantedUserIds("Connection", CONNECTION_ID)).thenReturn(List.of(GRANTEE_USER_ID));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteProjectDeniedWhenCallerLacksProjectScope() {
        // 'Project' annotations route through permissionService.hasResourceScope(projectId, 'Project', scope), like
        // every other targetType. The proxy must deny when that check returns false (configured in setup). Note that
        // this assertion alone does not pin the route: setup stubs every gate false, so it would be green whichever
        // one the evaluator consulted. testAllowedWhenPermissionServiceGrants is what pins it.
        assertThatThrownBy(() -> guardedProjectMutations.deleteProject(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetProjectDeniedWhenCallerLacksProjectScope() {
        assertThatThrownBy(() -> guardedProjectMutations.getProject(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetUserWorkspacesDeniedForNonAdminOtherUser() {
        // isCurrentUser returns false (configured in setup) so only tenant admins can read another user's memberships.
        assertThatThrownBy(() -> guardedProjectFacadeReads.getUserWorkspaces(999L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetResourceDeniedWhenCallerIsNotResourceOwner() {
        // isResourceOwner returns false (configured in setup) so the isResourceOwner(#id, 'Type') built-in denies.
        assertThatThrownBy(() -> guardedResourceOwnerReads.getApiKey(9L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAllowedWhenPermissionServiceGrants() {
        // Positive control, and the only assertion here that pins WHICH PermissionService method a 'Project'
        // annotation reaches. Once hasResourceScope grants, the same proxy chain allows the call -- while
        // hasWorkspaceScopeForProject stays stubbed false, so routing the four-argument hasPermission there instead
        // turns this red. If this fails, the rest of the denied-path assertions could be green for the wrong reason
        // (e.g., method security disabled, or a route to a method nothing grants).
        when(permissionService.hasResourceScope(1L, "Project", "WORKFLOW_VIEW")).thenReturn(true);

        guardedProjectMutations.getProject(1L);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesAddWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(2L, 1L, WorkspaceRole.VIEWER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesRemoveWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(2L, 1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesUpdateWorkspaceUserRole() {
        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(2L, 1L, WorkspaceRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesSetEnvironmentRole() {
        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                2L, 1L, Environment.PRODUCTION, WorkspaceRole.VIEWER, null))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesRemoveEnvironmentRole() {
        assertThatThrownBy(
            () -> workspaceUserService.removeEnvironmentRole(2L, 1L, Environment.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesGetWorkspaceWorkspaceUsers() {
        assertThatThrownBy(() -> workspaceUserService.getWorkspaceWorkspaceUsers(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetProjectGrantsDeniedWhenCallerIsNeitherOwnerNorProjectAdmin() {
        // Bean-reference form: "@permissionService.isResourceOwner('Project', #projectId) || @permissionService
        // .hasResourceRole(...)". Both disjuncts are stubbed false in setup, so the proxy must deny. The call runs
        // against the REAL ProjectSharingFacadeImpl bean, so dropping its @PreAuthorize turns this red.
        assertThatThrownBy(() -> projectSharingFacade.getProjectGrants(WORKSPACE_ID, PROJECT_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetProjectGrantsAllowedForResourceOwner() {
        when(permissionService.isResourceOwner("Project", PROJECT_ID)).thenReturn(true);

        assertThat(projectSharingFacade.getProjectGrants(WORKSPACE_ID, PROJECT_ID)).containsExactly(GRANTEE_USER_ID);
    }

    @Test
    void testGetProjectGrantsAllowedForProjectAdmin() {
        // The second disjunct on its own: proves Spring resolves BOTH bean-reference calls, not just the first.
        when(permissionService.hasResourceRole(PROJECT_ID, "Project", "ADMIN")).thenReturn(true);

        assertThat(projectSharingFacade.getProjectGrants(WORKSPACE_ID, PROJECT_ID)).containsExactly(GRANTEE_USER_ID);
    }

    @Test
    void testGetConnectionGrantsDeniedWhenCallerIsNeitherOwnerNorConnectionAdmin() {
        assertThatThrownBy(() -> workspaceConnectionFacade.getConnectionGrants(WORKSPACE_ID, CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetConnectionGrantsAllowedForResourceOwner() {
        when(permissionService.isResourceOwner("Connection", CONNECTION_ID)).thenReturn(true);

        assertThat(workspaceConnectionFacade.getConnectionGrants(WORKSPACE_ID, CONNECTION_ID))
            .containsExactly(GRANTEE_USER_ID);
    }

    @Test
    void testGetConnectionGrantsAllowedForConnectionAdmin() {
        when(permissionService.hasResourceRole(CONNECTION_ID, "Connection", "ADMIN")).thenReturn(true);

        assertThat(workspaceConnectionFacade.getConnectionGrants(WORKSPACE_ID, CONNECTION_ID))
            .containsExactly(GRANTEE_USER_ID);
    }

    // @SpringBootConfiguration (not @TestConfiguration) because @SpringBootTest(classes = Config.class) requires a
    // primary Spring Boot configuration class; @TestConfiguration is a supplemental config and Spring Boot explicitly
    // rejects it as the primary ("Classes annotated with @TestConfiguration are not considered"). The synthetic
    // Guarded* stand-ins and the real WorkspaceUserServiceImpl share one context; the mocked PermissionService backs
    // both.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        GuardedProjectMutations.class, GuardedProjectFacadeReads.class, GuardedResourceOwnerReads.class,
        WorkspaceUserServiceImpl.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }

        @Bean
        WorkspaceUserAuditPublisher workspaceUserAuditPublisher() {
            return mock(WorkspaceUserAuditPublisher.class);
        }

        // The remaining WorkspaceUserServiceImpl collaborators, mocked like the ones above — the proxy fires the
        // @PreAuthorize check before any of them is touched, so stubs are all these enforcement tests need.
        @Bean
        CustomRoleRepository customRoleRepository() {
            return mock(CustomRoleRepository.class);
        }

        @Bean
        UserInvitationService userInvitationService() {
            return mock(UserInvitationService.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        WorkspaceService workspaceService() {
            return mock(WorkspaceService.class);
        }

        @Bean
        ProjectService projectService() {
            return mock(ProjectService.class);
        }

        @Bean
        ResourceGrantService resourceGrantService() {
            return mock(ResourceGrantService.class);
        }

        @Bean
        WorkspaceConnectionService workspaceConnectionService() {
            return mock(WorkspaceConnectionService.class);
        }

        // The two sharing facades are the REAL production classes, not Guarded* stand-ins: the bean-reference
        // @PreAuthorize form is only proven to fire if the annotation under test is the one shipped. Their
        // collaborators are mocks because the proxy decides before any of them is touched, and the allowed path
        // needs only the few stubbed in the test's setup. The return type is the INTERFACE deliberately -- method
        // security wraps an interface-implementing bean in a JDK proxy, which is not assignable to the impl class.
        @Bean
        ProjectSharingFacade projectSharingFacade(
            ProjectService projectService, ResourceGrantService resourceGrantService) {

            return new ProjectSharingFacadeImpl(
                mock(ProjectAuditPublisher.class), projectService, resourceGrantService,
                mock(ResourceVisibilityPolicyRegistry.class), mock(WorkspaceUserService.class));
        }

        @Bean
        WorkspaceConnectionFacade workspaceConnectionFacade(
            ProjectService projectService, ResourceGrantService resourceGrantService,
            WorkspaceConnectionService workspaceConnectionService) {

            @SuppressWarnings("unchecked")
            ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

            return new WorkspaceConnectionFacadeImpl(
                mock(ApplicationEventPublisher.class), mock(ConnectionFacade.class),
                mock(ConnectionLifecycleFacade.class), mock(ConnectionService.class),
                mock(ResourceVisibilityResolver.class), meterRegistryProvider,
                mock(ProjectDeploymentWorkflowService.class), projectService, resourceGrantService,
                mock(ResourceVisibilityPolicyRegistry.class), mock(TagService.class), mock(UserService.class),
                mock(WorkflowTestConfigurationService.class), workspaceConnectionService, mock(WorkspaceFacade.class),
                mock(WorkspaceUserService.class));
        }
    }

    /**
     * Mirrors the {@code 'Project'} {@code @PreAuthorize} expressions on the project facade/service impls.
     * {@code AutomationPermissionEvaluator}'s four-argument
     * {@code hasPermission(auth, targetId, targetType, permission)} — the form every {@code hasPermission(#id, 'Type',
     * 'SCOPE')} annotation compiles to — routes <em>every</em> targetType, {@code 'Project'} included, to
     * {@code permissionService.hasResourceScope(targetId, targetType, permission)}. It never branches on the type name.
     * {@code hasWorkspaceScopeForProject} is reached only from the two-argument form, and only for a
     * {@code ProjectDeploymentDTO} target, which is a different family of annotation from the ones stood in for here.
     *
     * <p>
     * Which of the two the proxy actually takes is pinned by {@link #testAllowedWhenPermissionServiceGrants()} rather
     * than by the denial tests: every gate is stubbed false in {@code @BeforeEach}, so a denial is green whichever
     * method the evaluator calls. The positive control grants {@code hasResourceScope} alone and leaves
     * {@code hasWorkspaceScopeForProject} false, so it turns red if the routing moves.
     *
     * <p>
     * Kept in sync by {@link PreAuthorizeAnnotationTest}, which pins the expressions on the production impls. If the
     * production annotation changes without updating this stand-in, the test still fires the proxy — it just exercises
     * the old expression, so the reflection test in {@code PreAuthorizeAnnotationTest} is the source of truth for
     * drift.
     */
    @Service
    static class GuardedProjectMutations {

        @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')")
        public void deleteProject(long projectId) {
        }

        @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')")
        public void getProject(long projectId) {
        }
    }

    @Service
    static class GuardedProjectFacadeReads {

        @PreAuthorize("isTenantAdmin() or isCurrentUser(#id)")
        public void getUserWorkspaces(long id) {
        }
    }

    @Service
    static class GuardedResourceOwnerReads {

        @PreAuthorize("isResourceOwner(#id, 'ApiKey')")
        public void getApiKey(long id) {
        }
    }
}
