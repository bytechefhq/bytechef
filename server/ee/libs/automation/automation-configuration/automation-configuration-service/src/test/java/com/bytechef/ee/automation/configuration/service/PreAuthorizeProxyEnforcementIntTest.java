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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacadeImpl;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.facade.WorkspaceUserFacade;
import com.bytechef.ee.automation.configuration.facade.WorkspaceUserFacadeImpl;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Proves the {@code @PreAuthorize} proxy actually fires, and that each guard asks the {@link PermissionService} method
 * the annotation names.
 * <p>
 * Every denial assertion is paired with a {@code verify} of the exact call the gate made and with a positive control
 * that grants the same call. Without those two, a denial assertion is satisfied by Mockito's default {@code false} for
 * <em>any</em> unstubbed boolean method — so it would stay green if the guard named a completely different check, or if
 * it were deleted and the body happened to throw for another reason. The mock is {@code reset} before each test rather
 * than merely re-stubbed, so a {@code verify} counts one test's invocations and not the class's.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PreAuthorizeProxyEnforcementIntTest.Config.class, properties = "bytechef.edition=ee")
class PreAuthorizeProxyEnforcementIntTest {

    private static final long CUSTOM_ROLE_ID = 3L;
    private static final String MEMBER_MANAGE = "WORKSPACE_MEMBER_MANAGE";
    private static final long USER_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private CustomRoleRepository customRoleRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @Autowired
    private GuardedProjectMutations guardedProjectMutations;

    @Autowired
    private GuardedProjectFacadeReads guardedProjectFacadeReads;

    @Autowired
    private GuardedResourceOwnerReads guardedResourceOwnerReads;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @Autowired
    private WorkspaceUserFacade workspaceUserFacade;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Reset, not just re-stub: the PermissionService mock is a singleton in the cached Spring context, so without
        // this every verify() would count the invocations of the tests that ran before it, and a positive control's
        // true-stub would leak into the next denial assertion.
        // The repositories are singletons in the same cached context, so a positive control's stubbing would otherwise
        // survive into the next test's denial assertion.
        reset(permissionService, customRoleRepository, workspaceUserRepository, projectFacade);

        // Exactly the methods the production annotations route to. Stubbing anything else would be decoration: a
        // denial that rests on Mockito's default false cannot distinguish "the named check refused" from "nothing
        // named this check".
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(anyLong())).thenReturn(false);
        when(permissionService.isResourceOwner(anyString(), anyLong())).thenReturn(false);
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(anyLong(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScope(anyLong(), anyString(), any())).thenReturn(false);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteProjectDeniedWhenCallerLacksProjectScope() {
        // 'Project' annotations route through AutomationPermissionEvaluator's four-argument hasPermission, which calls
        // permissionService.hasResourceScope(id, targetType, scope) -- not hasWorkspaceScopeForProject, which only the
        // ProjectDeploymentDTO promotion form reaches.
        assertThatThrownBy(() -> guardedProjectMutations.deleteProject(1L))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasResourceScope(1L, "Project", "PROJECT_DELETE");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testGetProjectDeniedWhenCallerLacksProjectScope() {
        assertThatThrownBy(() -> guardedProjectMutations.getProject(1L))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasResourceScope(1L, "Project", "WORKFLOW_VIEW");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testGetUserWorkspacesDeniedForNonAdminOtherUser() {
        // isCurrentUser returns false (configured in setup) so only tenant admins can read another user's memberships.
        assertThatThrownBy(() -> guardedProjectFacadeReads.getUserWorkspaces(999L))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).isTenantAdmin();
        verify(permissionService).isCurrentUser(999L);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testGetResourceDeniedWhenCallerIsNotResourceOwner() {
        // isResourceOwner returns false (configured in setup) so the isResourceOwner(#id, 'Type') built-in denies. The
        // built-in takes (id, type) and the service takes (type, id) -- the verify pins that the root does not swap
        // them.
        assertThatThrownBy(() -> guardedResourceOwnerReads.getApiKey(9L))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).isResourceOwner("ApiKey", 9L);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testAllowedWhenPermissionServiceGrants() {
        // Positive control: once hasResourceScope grants, the same proxy chain allows the call. If this fails, the
        // rest of the denied-path assertions could be green for the wrong reason (e.g., method security disabled).
        when(permissionService.hasResourceScope(1L, "Project", "WORKFLOW_VIEW")).thenReturn(true);

        guardedProjectMutations.getProject(1L);

        verify(permissionService).hasResourceScope(1L, "Project", "WORKFLOW_VIEW");
    }

    @Test
    void testShortWorkflowExecutionsOverloadDeniesAnotherWorkspace() {
        assertThatThrownBy(
            () -> projectWorkflowExecutionFacade.getWorkflowExecutions(
                false, 0L, null, null, null, null, null, null, null, 2L, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScope(2L, "EXECUTION_VIEW", Environment.values()[0]);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(projectFacade);
    }

    @Test
    void testShortWorkflowExecutionsOverloadAllowsAGrantedWorkspace() {
        when(permissionService.hasWorkspaceScope(2L, "EXECUTION_VIEW", Environment.values()[0])).thenReturn(true);

        projectWorkflowExecutionFacade.getWorkflowExecutions(
            false, 0L, null, null, null, null, null, null, null, 2L, 0);

        verify(projectFacade).getWorkspaceProjectWorkflows(2L);
    }

    @Test
    void testEmbeddedOnlyMemberListsEmbeddedExecutionsWithoutTheWorkspaceGate() {
        when(permissionService.hasEmbeddedScope(eq("EXECUTION_VIEW"), any(Environment.class))).thenReturn(true);
        when(permissionService.hasEmbeddedScope(eq("CONNECTED_USER_VIEW"), any(Environment.class))).thenReturn(true);

        projectWorkflowExecutionFacade.getWorkflowExecutions(
            true, 0L, null, null, null, null, null, null, null, 2L, 0);

        verify(permissionService).hasEmbeddedScope("EXECUTION_VIEW", Environment.values()[0]);
        verify(permissionService).hasEmbeddedScope("CONNECTED_USER_VIEW", Environment.values()[0]);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(projectFacade);
    }

    @Test
    void testEmbeddedRunsAreDeniedWithoutConnectedUserView() {
        when(permissionService.hasEmbeddedScope(eq("EXECUTION_VIEW"), any(Environment.class))).thenReturn(true);

        assertThatThrownBy(
            () -> projectWorkflowExecutionFacade.getWorkflowExecutions(
                true, 0L, null, null, null, null, null, null, null, 2L, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasEmbeddedScope("CONNECTED_USER_VIEW", Environment.values()[0]);
        verifyNoInteractions(projectFacade);
    }

    @Test
    void testEmbeddedRunsAreDeniedWithoutTheEmbeddedScope() {
        assertThatThrownBy(
            () -> projectWorkflowExecutionFacade.getWorkflowExecutions(
                true, 0L, null, null, null, null, null, null, null, 2L, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasEmbeddedScope("EXECUTION_VIEW", Environment.values()[0]);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testWorkspaceUserFacadeEnforcesAddWorkspaceUser() {
        assertThatThrownBy(
            () -> workspaceUserFacade.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testWorkspaceUserFacadeAllowsAddWorkspaceUserWhenTheScopeIsGranted() {
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE)).thenReturn(true);

        workspaceUserFacade.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, null);

        // Reached the body: the write and the cache eviction only happen past the proxy.
        verify(permissionService).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testWorkspaceUserFacadeEnforcesRemoveWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserFacade.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testWorkspaceUserFacadeEnforcesUpdateWorkspaceUserRole() {
        assertThatThrownBy(
            () -> workspaceUserFacade.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    /**
     * {@code assignCustomRole} is the third way to change a member's effective permissions, and it was pinned by
     * nothing — deleting its annotation left every test in this module green. It carries the same guard as the built-in
     * role writes because a custom role can carry any scope the built-ins do.
     */
    @Test
    void testWorkspaceUserFacadeEnforcesAssignCustomRole() {
        assertThatThrownBy(() -> workspaceUserFacade.assignCustomRole(USER_ID, WORKSPACE_ID, CUSTOM_ROLE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testWorkspaceUserFacadeAllowsAssignCustomRoleWhenTheScopeIsGranted() {
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE)).thenReturn(true);

        // The caller also holds the role's scopes; granting a role that carries more than the caller holds is refused.
        when(permissionService.hasWorkspaceScope(anyLong(), anyString(), any(Environment.class))).thenReturn(true);

        stubAnAssignableCustomRoleAndMembership();

        WorkspaceUser workspaceUser = workspaceUserFacade.assignCustomRole(USER_ID, WORKSPACE_ID, CUSTOM_ROLE_ID);

        // The write itself, past the proxy. Asserting merely that the body threw something other than
        // AccessDeniedException would make this red the day assignCustomRole legitimately stops throwing — a failure
        // that says nothing about the gate.
        assertThat(workspaceUser.getCustomRoleId()).isEqualTo(CUSTOM_ROLE_ID);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verify(workspaceUserRepository).save(workspaceUser);
    }

    /**
     * A present custom role and an existing workspace-wide membership on a non-admin built-in role, so the body of
     * {@code assignCustomRole} runs to its write instead of failing on a lookup. The role is deliberately not an
     * {@code ADMIN} one: that would pull in the last-admin check, which is a different guard.
     */
    private void stubAnAssignableCustomRoleAndMembership() {
        when(customRoleRepository.findById(CUSTOM_ROLE_ID))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER.ordinal())));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testWorkspaceUserFacadeEnforcesSetEnvironmentRole() {
        assertThatThrownBy(
            () -> workspaceUserFacade.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null))
                    .isInstanceOf(AccessDeniedException.class);

        // The three-argument overload, which the environment-unaware two-argument one does not satisfy: a member who
        // administers Development must not write a Production role.
        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testWorkspaceUserFacadeEnforcesRemoveEnvironmentRole() {
        assertThatThrownBy(
            () -> workspaceUserFacade.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testWorkspaceUserFacadeEnforcesGetWorkspaceWorkspaceUsers() {
        assertThatThrownBy(() -> workspaceUserFacade.getWorkspaceWorkspaceUsers(WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testWorkspaceUserFacadeAllowsGetWorkspaceWorkspaceUsersWhenTheScopeIsGranted() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).thenReturn(true);

        workspaceUserFacade.getWorkspaceWorkspaceUsers(WORKSPACE_ID);

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW");
        verifyNoMoreInteractions(permissionService);
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
        WorkspaceUserFacadeImpl.class, WorkspaceUserServiceImpl.class
    })
    static class Config {

        @Bean
        ProjectFacade projectFacade() {
            return mock(ProjectFacade.class);
        }

        @Bean
        ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade(
            PermissionService permissionService, ProjectFacade projectFacade) {

            return new ProjectWorkflowExecutionFacadeImpl(
                mock(ComponentDefinitionService.class),
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserProjectDeploymentResolver.class),
                mock(ContextService.class),
                new StaticListableBeanFactory().getBeanProvider(EmbeddedResourceResolver.class), mock(Evaluator.class),
                mock(EnvironmentService.class),
                mock(WorkflowExecutionRowService.class), mock(JobService.class), permissionService,
                mock(PrincipalJobService.class), projectFacade, mock(ProjectDeploymentService.class),
                mock(ProjectService.class), mock(ProjectWorkflowService.class),
                mock(TaskDispatcherDefinitionService.class), mock(TaskExecutionService.class),
                mock(TaskFileStorage.class), mock(TriggerExecutionService.class), mock(TriggerFileStorage.class),
                mock(WorkflowService.class));
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        PermissionScopeRegistry permissionScopeRegistry() {
            return mock(PermissionScopeRegistry.class);
        }

        @Bean
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }

        // The remaining WorkspaceUserServiceImpl collaborators, mocked like the ones above — a denial fires the
        // @PreAuthorize check before any of them is touched, and the positive controls only need them to answer
        // Mockito's defaults.
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
            WorkspaceService workspaceService = mock(WorkspaceService.class);

            // Every workspace the enforcement tests name exists. The membership writes check existence before they
            // insert, so an unstubbed mock would fail the allow-direction tests with WORKSPACE_NOT_FOUND and hide
            // whether the proxy admitted the call at all.
            when(workspaceService.workspaceExists(anyLong())).thenReturn(true);

            return workspaceService;
        }
    }

    /**
     * Exercises the EE evaluator routing of the {@code hasPermission(id, 'Type', 'SCOPE')} form to
     * {@code permissionService.hasResourceScope(id, resourceType, scope)}, which the mocked {@link PermissionService}
     * stubs. It is not a stand-in for the production guards: the guards on {@code ProjectFacadeImpl} and
     * {@code ProjectServiceImpl} are called through the real proxy in {@code ProjectFacadeIntTest} and
     * {@code ProjectServiceTest}.
     */
    @Service
    static class GuardedProjectMutations {

        private final AtomicInteger invocations = new AtomicInteger();

        @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')")
        public void deleteProject(long projectId) {
            invocations.incrementAndGet();
        }

        @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')")
        public void getProject(long projectId) {
            invocations.incrementAndGet();
        }
    }

    @Service
    static class GuardedProjectFacadeReads {

        private final AtomicInteger invocations = new AtomicInteger();

        @PreAuthorize("isTenantAdmin() or isCurrentUser(#id)")
        public void getUserWorkspaces(long id) {
            invocations.incrementAndGet();
        }
    }

    @Service
    static class GuardedResourceOwnerReads {

        private final AtomicInteger invocations = new AtomicInteger();

        @PreAuthorize("isResourceOwner(#id, 'ApiKey')")
        public void getApiKey(long id) {
            invocations.incrementAndGet();
        }
    }
}
