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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionRoot;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider.Decision;
import com.bytechef.automation.configuration.security.ProjectDeploymentEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentJobPrincipalAuthenticationResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentWorkflowEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentWorkflowOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.security.WorkspaceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.security.AiSkillOwnershipResolver;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.worker.security.JobPrincipalAuthenticationRunner;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionServiceTest {

    private static final long USER_ID = 42L;
    private static final long PROJECT_ID = 100L;
    private static final long WORKSPACE_ID = 7L;
    private static final String CONNECTED_USER_EXTERNAL_ID = "external-user-1";
    private static final String LOGIN = "alice";
    private CurrentUserResolver currentUserResolver;
    private PermissionScopeRegistry permissionScopeRegistry;
    private ProjectRepository projectRepository;
    private WorkspaceScopeCacheService workspaceScopeCacheService;
    private UserService userService;
    private WorkspaceUserRepository workspaceUserRepository;
    private PermissionServiceImpl permissionService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        projectRepository = mock(ProjectRepository.class);
        workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
        userService = mock(UserService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        // CurrentUserResolver is the extracted "who is the caller" component (M5 in the RBAC review). It uses the same
        // SecurityUtils + UserService stubs that this test already wires, so we instantiate it directly rather than
        // mocking it — verifying the integration between PermissionServiceImpl and the resolver matters more
        // than isolating either side.
        currentUserResolver = new CurrentUserResolver(userService);

        permissionService = new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(), List.of(),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));

        securityUtilsMock = mockStatic(SecurityUtils.class);

        // Default: not tenant admin, current user is "alice"
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(false);
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(LOGIN));

        // Force the no-request-context branch in getCurrentUserId so we don't depend on RequestContextHolder state.
        RequestContextHolder.resetRequestAttributes();

        User user = new User();
        user.setId(USER_ID);
        user.setLogin(LOGIN);

        lenient().when(userService.getUser(LOGIN))
            .thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void testIsTenantAdminTrueWhenAuthorityPresent() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.isTenantAdmin()).isTrue();
    }

    @Test
    void testIsTenantAdminFalseWhenAuthorityAbsent() {
        assertThat(permissionService.isTenantAdmin()).isFalse();
    }

    @Test
    void testIsCurrentUserTrueForMatchingUser() {
        assertThat(permissionService.isCurrentUser(USER_ID)).isTrue();
    }

    @Test
    void testIsCurrentUserFalseForDifferentUser() {
        assertThat(permissionService.isCurrentUser(USER_ID + 1)).isFalse();
    }

    @Test
    void testIsCurrentUserFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.isCurrentUser(USER_ID)).isFalse();
    }

    @Test
    void testHasWorkspaceRoleShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();

        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasWorkspaceRoleAdminUserSatisfiesAllMinimums() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isTrue();
    }

    @Test
    void testHasWorkspaceRoleViewerCannotEdit() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER.ordinal())));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleFalseForNonMember() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleDeniesAMemberInExplicitMode() {
        // A behaviour change worth pinning: this member holds ADMIN in every environment there is, and is still denied,
        // because a per-environment role is not a workspace-wide one and there is no single role of theirs to compare.
        // The rows are stubbed to show the denial is not "no membership found" — hasWorkspaceRole deliberately never
        // looks at them, which the verify below holds in place.
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.DEVELOPMENT),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.STAGING),
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "ADMIN")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();

        verify(workspaceUserRepository, never()).findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasResourceRoleDeniesAMemberInExplicitMode() {
        // hasResourceRole resolves the owning workspace and then asks hasWorkspaceRole, so it inherits that denial.
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        PermissionServiceImpl service = createService(
            resolver("Connection", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        assertThat(service.hasResourceRole(1L, "Connection", "ADMIN")).isFalse();

        verify(workspaceUserRepository, never()).findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGetMyWorkspaceRoleNullForAMemberInExplicitMode() {
        // The honest answer, and the other half of the explicit-mode reading: they hold no one role across the
        // workspace,
        // so the members view must render the per-environment roles rather than this.
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(
                List.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION)));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();

        verify(workspaceUserRepository, never()).findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasWorkspaceRoleFailsClosedOnInvalidRoleOrdinal() {
        // A corrupted or legacy workspace_role ordinal hydrated via Spring Data JDBC (which bypasses the constructor's
        // range validation) must fail closed — deny — rather than throw ArrayIndexOutOfBoundsException as a 500.
        WorkspaceUser corrupted = mock(WorkspaceUser.class);

        when(corrupted.getWorkspaceRole()).thenReturn(999);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(corrupted));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_DELETE")).isTrue();

        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testHasWorkspaceScopeReturnsTrueWhenScopeGranted() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkspaceScopeReturnsFalseWhenScopeMissing() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectShortCircuitsForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_DELETE")).isTrue();

        verify(projectRepository, never()).findById(PROJECT_ID);
    }

    @Test
    void testHasWorkspaceScopeForProjectResolvesOwningWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectFalseForUnknownProject() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_VIEW")).isFalse();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceRoleFalseOnUnknownRoleName() {
        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "NOT_A_ROLE")).isFalse();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceRoleFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceScopeFalseOnUnknownScopeName() {
        // Scope names are now plain strings validated by set-membership, not by an enum lookup. A name no role grants
        // (a typo, or a scope the user lacks) simply isn't in the resolved set, so the check fails closed without any
        // special-casing.
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "NOT_A_SCOPE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeFalseWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_VIEW")).isFalse();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testHasWorkspaceScopeFalseWhenAuthenticatedLoginHasNoPlatformUser() {
        // Defense-in-depth: an authenticated principal whose login resolves to no platform user (e.g. a non-platform
        // principal reaching a platform RBAC check) must fail closed — deny — rather than bubble UserNotFoundException
        // as a 500. Embedded flows that legitimately bypass automation RBAC are handled earlier via the skip flag.
        when(userService.getUser(LOGIN)).thenThrow(new UserNotFoundException());

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_VIEW")).isFalse();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceScopesEmptyWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID)).isEmpty();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceScopesReturnsAllScopesForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);
        when(permissionScopeRegistry.getAllScopeNames())
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKFLOW_DELETE", "CONNECTION_VIEW"));

        Set<String> scopes = permissionService.getMyWorkspaceScopes(WORKSPACE_ID);

        assertThat(scopes).containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_DELETE", "CONNECTION_VIEW");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGetMyWorkspaceScopesDelegatesToCache() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID))
            .containsExactly("WORKFLOW_VIEW");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentDelegatesToTheEnvironmentLookup() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.STAGING))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.STAGING))
            .containsExactly("WORKFLOW_EDIT");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentReturnsAllScopesForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);
        when(permissionScopeRegistry.getAllScopeNames())
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKSPACE_MEMBER_MANAGE"));

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION))
            .containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKSPACE_MEMBER_MANAGE");
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentEmptyWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION)).isEmpty();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testGetMyWorkspaceRoleNullWhenSecurityContextEmpty() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdminForTenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isEqualTo("ADMIN");
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testGetMyWorkspaceRoleReturnsMembershipRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isEqualTo("EDITOR");
    }

    @Test
    void testGetMyWorkspaceRoleIgnoresAnImplicitRowBesideEnvironmentRows() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(USER_ID, WORKSPACE_ID))
            .thenReturn(true);
        lenient().when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
    }

    @Test
    void testHasWorkspaceRoleIgnoresAnImplicitRowBesideEnvironmentRows() {
        when(workspaceUserRepository.existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(USER_ID, WORKSPACE_ID))
            .thenReturn(true);
        lenient().when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));

        assertThat(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).isFalse();
    }

    @Test
    void testGetMyWorkspaceRoleReturnsNullForNonMember() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
    }

    @Test
    void testGetMyWorkspaceRoleNullOnInvalidRoleOrdinal() {
        // Symmetric to hasWorkspaceRole: an out-of-range ordinal must fail closed to a null role rather than throw.
        WorkspaceUser corrupted = mock(WorkspaceUser.class);

        when(corrupted.getWorkspaceRole()).thenReturn(999);
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(corrupted));

        assertThat(permissionService.getMyWorkspaceRole(WORKSPACE_ID)).isNull();
    }

    @Test
    void testHasWorkflowScopeGrantsUnderAutomationAuthorizationSkip() throws Throwable {
        // A trusted system path under the skip flag: the gate must short-circuit to grant BEFORE any
        // project/user/cache lookup runs. Without the skip flag this same call returns false (project not stubbed → no
        // workspace).
        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_EDIT"));

        assertThat(granted).isTrue();

        verify(projectRepository, never()).findByWorkflowId(anyString());
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong());
    }

    @Test
    void testHasWorkflowScopeFalseWithoutSkipWhenWorkflowUnknown() {
        // Control for the skip test above: outside the skip context the same inputs deny (the workflow id resolves
        // to no project, so there is no owning workspace to grant a scope in).
        assertThat(permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_EDIT")).isFalse();
    }

    @Test
    void testIsCurrentUserDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticateAsConnectedUser();

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.isCurrentUser(USER_ID));

        assertThat(granted).isFalse();
    }

    @Test
    void testIsResourceOwnerDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticateAsConnectedUser();

        PermissionServiceImpl service = createService(resolver("ApiKey", ResourceOwner.ofUser(USER_ID)));

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> service.isResourceOwner("ApiKey", 1L));

        assertThat(granted).isFalse();
    }

    @Test
    void testHasResourceRoleDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticateAsConnectedUser();

        PermissionServiceImpl service = createService(
            resolver("KnowledgeBase", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> service.hasResourceRole(1L, "KnowledgeBase", "VIEWER"));

        assertThat(granted).isFalse();
    }

    @Test
    void testHasWorkspaceRoleDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticateAsConnectedUser();

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER"));

        assertThat(granted).isFalse();
    }

    @Test
    void testHasWorkspaceScopeInEveryEnvironmentDeniesAConnectedUserUnderAutomationAuthorizationSkip()
        throws Throwable {

        authenticateAsConnectedUser();

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"));

        assertThat(granted).isFalse();
        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testTenantAdminPassesThePrivilegedChecksUnderAutomationAuthorizationSkip() throws Throwable {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        PermissionServiceImpl service = createService(
            resolver("KnowledgeBase", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> service.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE") &&
                service.hasWorkspaceRole(WORKSPACE_ID, "ADMIN") &&
                service.hasResourceRole(1L, "KnowledgeBase", "ADMIN") &&
                service.isResourceOwner("KnowledgeBase", 1L) &&
                service.isCurrentUser(USER_ID));

        assertThat(granted).isTrue();
    }

    @Test
    void testCanUseConnectionInWorkflowGrantsAConnectionOfTheWorkflowsWorkspaceAndEnvironment() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("CONNECTION_VIEW"));

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID, Environment.DEVELOPMENT);

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isTrue();
    }

    @Test
    void testCanUseConnectionInWorkflowDeniesAConnectionOfAnotherWorkspace() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong(), any()))
            .thenReturn(Set.of("CONNECTION_VIEW"));

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID + 1, Environment.DEVELOPMENT);

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testCanUseConnectionInWorkflowDeniesAConnectionOfAnotherEnvironment() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong(), any()))
            .thenReturn(Set.of("CONNECTION_VIEW"));

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testCanUseConnectionInWorkflowDeniesWithoutTheConnectionViewScope() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID, Environment.DEVELOPMENT);

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testCanUseConnectionInWorkspaceGrantsAConnectionOfTheWorkspaceAndEnvironment() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("CONNECTION_VIEW"));

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID, Environment.DEVELOPMENT);

        assertThat(service.canUseConnectionInWorkspace(1L, WORKSPACE_ID, Environment.DEVELOPMENT)).isTrue();
    }

    @Test
    void testCanUseConnectionInWorkspaceDeniesAConnectionOfAnotherWorkspace() {
        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong(), any()))
            .thenReturn(Set.of("CONNECTION_VIEW"));

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID + 1, Environment.DEVELOPMENT);

        assertThat(service.canUseConnectionInWorkspace(1L, WORKSPACE_ID, Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testCanUseConnectionInWorkspaceDeniesAConnectionOfAnotherEnvironment() {
        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong(), any()))
            .thenReturn(Set.of("CONNECTION_VIEW"));

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(service.canUseConnectionInWorkspace(1L, WORKSPACE_ID, Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testCanUseConnectionInWorkflowGrantsATenantAdmin() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        PermissionServiceImpl service = createConnectionService(WORKSPACE_ID + 1, Environment.PRODUCTION);

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isTrue();
    }

    @Test
    void testHasResourceScopeUsesWorkspaceScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("CONNECTION_DELETE"));

        PermissionServiceImpl service = createService(
            resolver("Connection", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testHasResourceScopeNoWorkspaceFailsClosed() {
        PermissionServiceImpl service = createService(resolver("Connection", ResourceOwner.unknown()));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testIsResourceOwnerMatchAllows() {
        PermissionServiceImpl service = createService(resolver("ApiKey", ResourceOwner.ofUser(USER_ID)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testIsResourceOwnerMismatchDenies() {
        PermissionServiceImpl service = createService(resolver("ApiKey", ResourceOwner.ofUser(USER_ID + 1)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isFalse();
    }

    @Test
    void testHasResourceRoleChecksWorkspaceRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));

        PermissionServiceImpl service = createService(
            resolver("KnowledgeBase", ResourceOwner.ofWorkspace(WORKSPACE_ID)));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isTrue();
    }

    @Test
    void testHasResourceRoleNoWorkspaceFailsClosed() {
        PermissionServiceImpl service = createService(resolver("KnowledgeBase", ResourceOwner.unknown()));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkflowScopeResolvesProjectWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowDeniesANonAdminWhenTheWorkflowBelongsToNoProject() {
        when(projectRepository.findByWorkflowId("unknown-workflow")).thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkflowScopeIfProjectWorkflow(
            "unknown-workflow", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();

        verify(workspaceScopeCacheService, never()).getWorkspaceScopes(anyLong(), anyLong(), any());
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowGrantsATenantAdminWhenTheWorkflowBelongsToNoProject() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkflowScopeIfProjectWorkflow(
            "integration-workflow", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowDeniesAProjectWorkflowWithoutTheScope() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowGrantsAProjectWorkflowWithTheScope() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();
    }

    @Test
    void testHasWorkflowScopeDeniesAMemberWhoHoldsTheScopeOnlyInProduction() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_EDIT"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT)).isFalse();
        assertThat(permissionService.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT", Environment.PRODUCTION)).isTrue();
        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow("wf-uuid", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();
    }

    @Test
    void testEvictWorkspaceScopeCacheDelegates() {
        permissionService.evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);

        verify(workspaceScopeCacheService, times(1)).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testEvictAllWorkspaceScopeCacheDelegates() {
        permissionService.evictAllWorkspaceScopeCache();

        verify(workspaceScopeCacheService, times(1)).evictAllWorkspaceScopeCache();
    }

    private void authenticateAsConnectedUser() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(CONNECTED_USER_EXTERNAL_ID));

        when(userService.getUser(CONNECTED_USER_EXTERNAL_ID)).thenThrow(new UserNotFoundException());
    }

    private PermissionServiceImpl createConnectionService(long connectionWorkspaceId, Environment environment) {
        ResourceEnvironmentResolver connectionEnvironmentResolver = new ResourceEnvironmentResolver() {
            @Override
            public String resourceType() {
                return "Connection";
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                return Optional.of(environment);
            }
        };

        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(resolver("Connection", ResourceOwner.ofWorkspace(connectionWorkspaceId))),
            List.of(connectionEnvironmentResolver),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));
    }

    private void givenWorkflowInWorkspace(long workspaceId) {
        Project project = new Project();

        project.setWorkspaceId(workspaceId);

        when(projectRepository.findByWorkflowId("workflow-1")).thenReturn(Optional.of(project));
    }

    private PermissionServiceImpl createService(ResourceOwnershipResolver... resolvers) {
        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(resolvers), List.of(),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));
    }

    private static ResourceOwnershipResolver resolver(String type, ResourceOwner owner) {
        return new ResourceOwnershipResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return owner;
            }
        };
    }

    @Test
    void testAllowsWhenTheEnvironmentRoleGrantsTheScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isTrue();
    }

    @Test
    void testDeniesWhenTheEnvironmentRoleDoesNotGrantTheScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testDeniesWhenTheMemberHasNoRoleInThatEnvironment() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testOneMemberIsAnsweredDifferentlyPerEnvironment() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT", "WORKFLOW_VIEW"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isTrue();
        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testMyScopesInAnEnvironmentAreEmptyWhereTheMemberHoldsNoRole() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("DEPLOYMENT_CREATE", "WORKFLOW_EDIT"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testMyScopesAreAnsweredPerEnvironment() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_VIEW"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKSPACE_MEMBER_MANAGE"));

        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.DEVELOPMENT))
            .containsExactly("WORKFLOW_VIEW");
        assertThat(permissionService.getMyWorkspaceScopes(WORKSPACE_ID, Environment.PRODUCTION))
            .containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKSPACE_MEMBER_MANAGE");
    }

    @Test
    void testProjectOverloadResolvesTheWorkspaceThenChecksTheEnvironment() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_VIEW", Environment.PRODUCTION))
            .isTrue();
    }

    @Test
    void testDeniesWhenTheProjectIsUnknown() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThat(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testTenantAdminIsNotSubjectToPerEnvironmentRoles() {
        securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(true);

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isTrue();
    }

    @Test
    void testEveryEnvironmentCheckRefusesWhenOneEnvironmentLacksTheScope() {
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKSPACE_MEMBER_MANAGE"));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.STAGING))
            .thenReturn(Set.of());

        // The escalation this closes: a member who administers only Development must not be able to grant a
        // workspace-wide role, which would take effect in Production too.
        assertThat(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .isFalse();
    }

    @Test
    void testEveryEnvironmentCheckAllowsWhenAllEnvironmentsGrantTheScope() {
        for (Environment environment : Environment.values()) {
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, environment))
                .thenReturn(Set.of("WORKSPACE_MEMBER_MANAGE"));
        }

        assertThat(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, "WORKSPACE_MEMBER_MANAGE"))
            .isTrue();
    }

    @Test
    void testByIdCheckUsesTheResourcesOwnEnvironment() {
        PermissionServiceImpl permissionServiceWithResolvers = new PermissionServiceImpl(
            new CurrentUserResolver(userService), mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(deploymentOwnershipResolver()),
            List.of(deploymentEnvironmentResolver(Environment.PRODUCTION)),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("DEPLOYMENT_VIEW"));

        // The member holds DEPLOYMENT_EDIT somewhere -- the environment-unaware read unions their environments -- but
        // not in Production, where this deployment lives. Without the environment resolver the union would answer the
        // check and a Production viewer could edit a Production deployment.
        assertThat(permissionServiceWithResolvers.hasResourceScope(1L, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isFalse();
        assertThat(permissionServiceWithResolvers.hasResourceScope(1L, "ProjectDeployment", "DEPLOYMENT_VIEW"))
            .isTrue();
    }

    @Test
    void testByIdCheckFallsBackWhenNoEnvironmentCanBeResolved() {
        PermissionServiceImpl permissionServiceWithResolvers = new PermissionServiceImpl(
            new CurrentUserResolver(userService), mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(deploymentOwnershipResolver()),
            List.of(deploymentEnvironmentResolver(null)),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        // A resource with no environment keeps the environment-unaware check.
        assertThat(permissionServiceWithResolvers.hasResourceScope(1L, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isTrue();
    }

    @Test
    void testByIdCheckDeniesWhenResolvingTheEnvironmentFails() {
        ResourceEnvironmentResolver failingEnvironmentResolver = new ResourceEnvironmentResolver() {

            @Override
            public String resourceType() {
                return "ProjectDeployment";
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                throw new IllegalStateException("database unavailable");
            }
        };

        PermissionServiceImpl permissionServiceWithResolvers = new PermissionServiceImpl(
            new CurrentUserResolver(userService), mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(deploymentOwnershipResolver()), List.of(failingEnvironmentResolver),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));

        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("DEPLOYMENT_EDIT"));

        // The member holds DEPLOYMENT_EDIT in some environment. Falling back to that union when the lookup fails would
        // let them edit a deployment in an environment where they are only a viewer.
        assertThat(permissionServiceWithResolvers.hasResourceScope(1L, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isFalse();
    }

    private static ResourceOwnershipResolver deploymentOwnershipResolver() {
        return new ResourceOwnershipResolver() {

            @Override
            public String resourceType() {
                return "ProjectDeployment";
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return ResourceOwner.ofWorkspace(WORKSPACE_ID);
            }
        };
    }

    private static ResourceEnvironmentResolver deploymentEnvironmentResolver(Environment environment) {
        return new ResourceEnvironmentResolver() {

            @Override
            public String resourceType() {
                return "ProjectDeployment";
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                return Optional.ofNullable(environment);
            }
        };
    }

    @Nested
    class ConnectedUser {

        private static final long USER_ID = 42L;
        private static final String WORKFLOW_ID = "workflow-1";

        private CurrentUserResolver currentUserResolver;
        private ConnectedUserAccessDecider decider;
        private PermissionServiceImpl permissionService;
        private ProjectRepository projectRepository;
        private WorkspaceScopeCacheService workspaceScopeCacheService;
        private WorkspaceUserRepository workspaceUserRepository;

        @BeforeEach
        void setUp() {
            securityUtilsMock.close();

            currentUserResolver = mock(CurrentUserResolver.class);
            decider = mock(ConnectedUserAccessDecider.class);
            projectRepository = mock(ProjectRepository.class);
            workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
            workspaceUserRepository = mock(WorkspaceUserRepository.class);

            StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
                Map.of("connectedUserAccessDecider", decider));

            permissionService = new PermissionServiceImpl(
                currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository, workspaceScopeCacheService,
                workspaceUserRepository, List.of(), List.of(),
                beanFactory.getBeanProvider(ConnectedUserAccessDecider.class));
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();

            securityUtilsMock = mockStatic(SecurityUtils.class);
        }

        @Test
        void testDecisionStandsUnderSkippedChecks() throws Throwable {
            when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.DENY);

            boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
                () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

            assertThat(allowed).isFalse();
        }

        @Test
        void testNotGovernedFallsThroughToTodaysLogic() {
            when(decider.decideWorkspace(1L, "WORKFLOW_VIEW")).thenReturn(Decision.NOT_GOVERNED);
            when(decider.decideWorkspace(0L, "")).thenReturn(Decision.NOT_GOVERNED);
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
            when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), eq(1L))).thenReturn(Set.of("WORKFLOW_VIEW"));

            assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isTrue();
        }

        @Test
        void testNotGovernedSkipStillShortCircuits() throws Throwable {
            when(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).thenReturn(Decision.NOT_GOVERNED);

            boolean allowed = AutomationAuthorizationContext.callSkippingChecks(
                () -> permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT"));

            assertThat(allowed).isTrue();
            verifyNoInteractions(currentUserResolver, workspaceScopeCacheService);
        }

        @Test
        void testGovernedPrincipalNeverReachesWorkspaceScopes() {
            when(decider.decideWorkspace(1L, "WORKFLOW_VIEW")).thenReturn(Decision.DENY);

            assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isFalse();
            verifyNoInteractions(currentUserResolver, workspaceScopeCacheService);
        }

        @Test
        void testIsAuthorizationSkippedIsFalseForAGovernedPrincipalEvenUnderSkip() throws Throwable {
            givenGovernedPrincipal();

            assertThat(AutomationAuthorizationContext.callSkippingChecks(permissionService::isAuthorizationSkipped))
                .isFalse();
        }

        @Test
        void testIsAuthorizationSkippedFollowsSkipForANotGovernedPrincipal() throws Throwable {
            when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.NOT_GOVERNED);

            assertThat(AutomationAuthorizationContext.callSkippingChecks(permissionService::isAuthorizationSkipped))
                .isTrue();
            assertThat(permissionService.isAuthorizationSkipped()).isFalse();
        }

        @Test
        void testIsAuthorizationSkippedFollowsSkipWithoutADecider() throws Throwable {
            PermissionServiceImpl permissionServiceWithoutDecider = new PermissionServiceImpl(
                currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository, workspaceScopeCacheService,
                workspaceUserRepository, List.of(), List.of(),
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));

            assertThat(
                AutomationAuthorizationContext
                    .callSkippingChecks(permissionServiceWithoutDecider::isAuthorizationSkipped))
                        .isTrue();
        }

        @Test
        void testGovernedPrincipalIsNeverTenantAdminOrCurrentUser() {
            givenAdminAuthentication();
            givenGovernedPrincipal();
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));

            assertThat(permissionService.isTenantAdmin()).isFalse();
            assertThat(permissionService.isCurrentUser(USER_ID)).isFalse();

            verifyNoInteractions(currentUserResolver);
        }

        @Test
        void testGovernedPrincipalIsDeniedEverySkipIgnoringCheckUnderSkip() throws Throwable {
            givenAdminAuthentication();
            givenGovernedPrincipal();
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));

            AutomationAuthorizationContext.callSkippingChecks(() -> {
                assertThat(permissionService.isTenantAdmin()).isFalse();
                assertThat(permissionService.isCurrentUser(USER_ID)).isFalse();
                assertThat(permissionService.isResourceOwner("ProjectDeployment", 5L)).isFalse();
                assertThat(permissionService.hasWorkspaceRole(1L, "VIEWER")).isFalse();
                assertThat(permissionService.hasResourceRole(5L, "ProjectDeployment", "VIEWER")).isFalse();
                assertThat(permissionService.hasWorkspaceScopeInEveryEnvironment(1L, "WORKFLOW_VIEW")).isFalse();
                assertThat(permissionService.getMyWorkspaceRole(1L)).isNull();
                assertThat(permissionService.getMyWorkspaceScopes(1L)).isEmpty();
                assertThat(permissionService.getMyWorkspaceScopes(1L, Environment.PRODUCTION)).isEmpty();

                return null;
            });

            verifyNoInteractions(currentUserResolver, workspaceScopeCacheService, workspaceUserRepository);
        }

        @Test
        void testGovernedPrincipalPassesWorkflowChecksForAnotherEnvironmentWhenTheDeciderGrants() {
            when(decider.decideWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT")).thenReturn(Decision.GRANT);

            assertThat(
                permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT",
                    Environment.DEVELOPMENT))
                        .isTrue();
            assertThat(permissionService.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();
            assertThat(permissionService.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_EDIT")).isTrue();
            verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
        }

        @Test
        void testConsultsTheRightDeciderMethodForEveryCheck() {
            when(decider.decide(any(Serializable.class), anyString(), anyString())).thenReturn(Decision.GRANT);
            when(decider.decideInEnvironment(any(Serializable.class), anyString(), anyString(), any(Environment.class)))
                .thenReturn(Decision.GRANT);
            when(decider.decideWorkflow(anyString(), anyString())).thenReturn(Decision.GRANT);
            when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.GRANT);

            assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isTrue();
            assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW", Environment.PRODUCTION)).isTrue();
            assertThat(permissionService.hasWorkspaceScopeForProject(9L, "PROJECT_EDIT")).isTrue();
            assertThat(permissionService.hasWorkspaceScopeForProject(9L, "PROJECT_EDIT", Environment.PRODUCTION))
                .isTrue();
            assertThat(permissionService.hasResourceScope(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).isTrue();
            assertThat(permissionService.hasResourceScopeInEnvironment(
                5L, "ProjectDeployment", "DEPLOYMENT_EDIT", Environment.PRODUCTION)).isTrue();
            assertThat(permissionService.canUseConnectionInWorkspace(3L, 1L, Environment.PRODUCTION)).isTrue();
            assertThat(permissionService.canUseConnectionInWorkflow(3L, WORKFLOW_ID, Environment.PRODUCTION)).isTrue();

            verify(decider, times(2)).decideWorkspace(1L, "WORKFLOW_VIEW");
            verify(decider).decide(9L, "Project", "PROJECT_EDIT");
            verify(decider).decideInEnvironment(9L, "Project", "PROJECT_EDIT", Environment.PRODUCTION);
            verify(decider).decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT");
            verify(decider).decideInEnvironment(5L, "ProjectDeployment", "DEPLOYMENT_EDIT", Environment.PRODUCTION);
            verify(decider).decideWorkspace(1L, "CONNECTION_VIEW");
            verify(decider).decide(3L, "Connection", "CONNECTION_VIEW");
            verify(decider).decideWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT");
            verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
        }

        @Test
        void testDeniesADeploymentOfTheOwnProjectInAnotherEnvironment() {
            when(decider.decide(9L, "Project", "DEPLOYMENT_CREATE")).thenReturn(Decision.GRANT);
            when(decider.decideInEnvironment(9L, "Project", "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
                .thenReturn(Decision.DENY);

            assertThat(permissionService.hasWorkspaceScopeForProject(9L, "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
                .isFalse();
            verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
        }

        @Test
        void testCanUseConnectionInWorkflowRequiresBothTheConnectionAndTheWorkflow() {
            when(decider.decide(3L, "Connection", "CONNECTION_VIEW")).thenReturn(Decision.GRANT);
            when(decider.decideWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT")).thenReturn(Decision.DENY);
            when(decider.decide(4L, "Connection", "CONNECTION_VIEW")).thenReturn(Decision.DENY);
            when(decider.decideWorkflow("workflow-2", "WORKFLOW_EDIT")).thenReturn(Decision.GRANT);

            assertThat(permissionService.canUseConnectionInWorkflow(3L, WORKFLOW_ID, Environment.PRODUCTION)).isFalse();
            assertThat(permissionService.canUseConnectionInWorkflow(4L, "workflow-2", Environment.PRODUCTION))
                .isFalse();
            verifyNoInteractions(projectRepository, currentUserResolver, workspaceScopeCacheService);
        }

        private void givenAdminAuthentication() {
            SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    "admin", null, List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
        }

        private void givenGovernedPrincipal() {
            when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.DENY);
        }
    }

    @Nested
    class ConnectedUserUnderSkippedChecks {

        private static final long PROJECT_ID = 9L;
        private static final Method TO_STRING_METHOD = ReflectionUtils.findMethod(Object.class, "toString");
        private static final long RESOURCE_ID = 5L;
        private static final String RESOURCE_TYPE = "ProjectDeployment";
        private static final String SCOPE = "DEPLOYMENT_EDIT";
        private static final String WORKFLOW_ID = "workflow-1";
        private static final long WORKSPACE_ID = 1L;

        private CurrentUserResolver currentUserResolver;
        private AutomationPermissionEvaluator evaluator;
        private ProjectRepository projectRepository;
        private AutomationMethodSecurityExpressionRoot root;
        private WorkspaceScopeCacheService workspaceScopeCacheService;

        @BeforeEach
        void setUp() {
            currentUserResolver = mock(CurrentUserResolver.class);
            projectRepository = mock(ProjectRepository.class);
            workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

            ConnectedUserAccessDecider decider = mock(ConnectedUserAccessDecider.class);

            when(decider.decide(any(Serializable.class), anyString(), anyString())).thenReturn(Decision.DENY);
            when(decider.decideInEnvironment(any(Serializable.class), anyString(), anyString(), any()))
                .thenReturn(Decision.DENY);
            when(decider.decideWorkflow(anyString(), anyString())).thenReturn(Decision.DENY);
            when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.DENY);

            StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
                Map.of("connectedUserAccessDecider", decider));

            PermissionServiceImpl permissionService = new PermissionServiceImpl(
                currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository, workspaceScopeCacheService,
                mock(WorkspaceUserRepository.class), List.of(), List.of(),
                beanFactory.getBeanProvider(ConnectedUserAccessDecider.class));

            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            MethodInvocation methodInvocation = mock(MethodInvocation.class);

            when(methodInvocation.getMethod()).thenReturn(TO_STRING_METHOD);

            EvaluationContext evaluationContext = expressionHandler.createEvaluationContext(
                () -> mock(Authentication.class), methodInvocation);

            root = (AutomationMethodSecurityExpressionRoot) evaluationContext.getRootObject()
                .getValue();

            evaluator = new AutomationPermissionEvaluator(permissionService);
        }

        @Test
        void testHasWorkspaceScopeInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(
                () -> root.hasWorkspaceScopeInEnvironment(WORKSPACE_ID, SCOPE, Environment.PRODUCTION));
        }

        @Test
        void testHasWorkspaceScopeInEnvironmentIdIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 2L));
            assertDeniedUnderSkippedChecks(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, null));
            assertDeniedUnderSkippedChecks(() -> root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 99L));
        }

        @Test
        void testHasResourceScopeInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(
                () -> root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.PRODUCTION));
            assertDeniedUnderSkippedChecks(
                () -> root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null));
        }

        @Test
        void testHasResourceScopeInEnvironmentIdIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(
                () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, 2L));
            assertDeniedUnderSkippedChecks(
                () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null));
            assertDeniedUnderSkippedChecks(
                () -> root.hasResourceScopeInEnvironmentId(RESOURCE_ID, RESOURCE_TYPE, SCOPE, 99L));
        }

        @Test
        void testHasWorkflowScopeIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(() -> root.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_VIEW"));
        }

        @Test
        void testHasWorkflowScopeInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(
                () -> root.hasWorkflowScopeInEnvironment(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION));
        }

        @Test
        void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(
                () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironment(
                    WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT));
        }

        @Test
        void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(
                () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", 0L));
            assertDeniedUnderSkippedChecks(
                () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", null));
            assertDeniedUnderSkippedChecks(
                () -> root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, "WORKFLOW_EDIT", 99L));
        }

        @Test
        void testHasPermissionByIdIsDeniedUnderSkippedChecks() throws Throwable {
            assertDeniedUnderSkippedChecks(() -> evaluator.hasPermission(null, RESOURCE_ID, RESOURCE_TYPE, SCOPE));
        }

        @Test
        void testHasPermissionOnAProjectDeploymentIsDeniedUnderSkippedChecks() throws Throwable {
            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setEnvironment(Environment.PRODUCTION);
            projectDeployment.setProjectId(PROJECT_ID);

            ProjectDeploymentDTO projectDeploymentDTO = new ProjectDeploymentDTO(projectDeployment);

            assertDeniedUnderSkippedChecks(
                () -> evaluator.hasPermission(null, projectDeploymentDTO, "DEPLOYMENT_CREATE"));
        }

        private void assertDeniedUnderSkippedChecks(BooleanSupplier check) throws Throwable {
            assertThat(AutomationAuthorizationContext.callSkippingChecks(check::getAsBoolean)).isFalse();
            assertThat(check.getAsBoolean()).isFalse();

            verifyNoInteractions(currentUserResolver, projectRepository, workspaceScopeCacheService);
        }
    }

    @Nested
    class ProjectResourceScope {

        private static final long PROJECT_ID = 3L;
        private static final long USER_ID = 7L;
        private static final long WORKSPACE_ID = 42L;

        private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        private final PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        private final ProjectRepository projectRepository = mock(ProjectRepository.class);
        private final WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
        private final WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

        @Test
        void testProjectScopeIsGrantedFromTheRoleHeldInTheOwningWorkspace() {
            givenProjectOwnedByWorkspace();

            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
                .thenReturn(Set.of("WORKFLOW_VIEW"));

            assertThat(service().hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isTrue();
        }

        @Test
        void testProjectScopeIsRefusedForANonMemberOfTheOwningWorkspace() {
            givenProjectOwnedByWorkspace();

            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID)).thenReturn(Set.of());

            assertThat(service().hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isFalse();
        }

        @Test
        void testProjectScopeIsRefusedForAnUnknownProject() {
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
                .thenReturn(Set.of("WORKFLOW_VIEW"));

            // Fails closed on the resolver's unknown(): a deleted project must not inherit the caller's scopes in some
            // other workspace.
            assertThat(service().hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isFalse();
        }

        /**
         * Deleting {@code ProjectOwnershipResolver} — or mistyping its {@code resourceType()} — would break no guard
         * test. It would silently turn every {@code 'Project'} gate in the tree into a lockout for every
         * non-tenant-admin: this is the assertion that fails instead.
         */
        @Test
        void testProjectScopeIsRefusedForEveryoneWithoutTheOwnershipResolver() {
            givenProjectOwnedByWorkspace();

            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
                .thenReturn(Set.of("WORKFLOW_VIEW"));

            PermissionServiceImpl serviceWithoutResolver = service(List.of());

            assertThat(serviceWithoutResolver.hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isFalse();
        }

        private void givenProjectOwnedByWorkspace() {
            Project project = new Project();

            project.setWorkspaceId(WORKSPACE_ID);

            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        }

        private PermissionServiceImpl service() {
            return service(ownershipResolvers());
        }

        private PermissionServiceImpl service(List<ResourceOwnershipResolver> resourceOwnershipResolvers) {
            return new PermissionServiceImpl(
                currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
                workspaceUserRepository, resourceOwnershipResolvers, List.of(),
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));
        }

        private List<ResourceOwnershipResolver> ownershipResolvers() {
            return List.of(new ProjectOwnershipResolver(projectRepository));
        }
    }

    @Nested
    class ProjectDeploymentResourceScope {

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

            assertThat(service().hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT"))
                .isTrue();
        }

        @Test
        void testDeploymentScopeIsRefusedWhenTheRoleIsHeldOnlyInAnotherEnvironment() {
            givenDeploymentInProductionOwnedByWorkspace();

            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .thenReturn(Set.of());
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT))
                .thenReturn(Set.of("DEPLOYMENT_EDIT"));

            assertThat(service().hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT"))
                .isFalse();
        }

        /**
         * Deleting {@code ProjectDeploymentOwnershipResolver} would not break any guard test, because those mock
         * {@code PermissionService}. It would silently turn every deployment guard into a lockout: this is the
         * assertion that fails instead.
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
            when(projectDeploymentRepository.findById(PROJECT_DEPLOYMENT_ID))
                .thenReturn(Optional.of(projectDeployment));
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
                workspaceUserRepository, resourceOwnershipResolvers, resourceEnvironmentResolvers,
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));
        }

        private List<ResourceEnvironmentResolver> environmentResolvers() {
            return List.of(new ProjectDeploymentEnvironmentResolver(projectDeploymentRepository));
        }
    }

    @Nested
    class ProjectDeploymentWorkflowResourceScope {

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
         * Deleting {@code ProjectDeploymentWorkflowOwnershipResolver} would not break any guard test, because those
         * mock {@code PermissionService}. It would silently turn the guard into a lockout: this is the assertion that
         * fails instead.
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
         * editor would pass on a Production row. That is the fallback {@code hasResourceScope} documents, which is why
         * the resolver has to exist for this token rather than being inherited from {@code 'ProjectDeployment'}.
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
                        .as("without it the same caller passes on the environment-unaware union, which is the fallback "
                            +
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
            when(projectDeploymentRepository.findById(PROJECT_DEPLOYMENT_ID))
                .thenReturn(Optional.of(projectDeployment));
        }

        private PermissionServiceImpl service() {
            return service(ownershipResolvers(), environmentResolvers());
        }

        private PermissionServiceImpl service(
            List<ResourceOwnershipResolver> resourceOwnershipResolvers,
            List<ResourceEnvironmentResolver> resourceEnvironmentResolvers) {

            return new PermissionServiceImpl(
                currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
                workspaceUserRepository, resourceOwnershipResolvers, resourceEnvironmentResolvers,
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));
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

    @Nested
    class WorkspaceResourceScope {

        private static final long USER_ID = 7L;
        private static final long WORKSPACE_ID = 42L;

        private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        private final PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        private final ProjectRepository projectRepository = mock(ProjectRepository.class);
        private final WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
        private final WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

        @Test
        void testWorkspaceScopeIsGrantedToAMemberHoldingIt() {
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
                .thenReturn(Set.of("WORKSPACE_VIEW"));

            assertThat(service().hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).isTrue();
        }

        @Test
        void testWorkspaceScopeIsRefusedForANonMember() {
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID)).thenReturn(Set.of());

            assertThat(service().hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).isFalse();
        }

        @Test
        void testWorkspaceScopeIsRefusedForAScopeTheMemberDoesNotHold() {
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
                .thenReturn(Set.of("WORKSPACE_VIEW"));

            // The scope is compared, not merely the membership: a viewer must not pass a management gate.
            assertThat(service().hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE")).isFalse();
        }

        /**
         * The identity resolver reads as deletable boilerplate — the id already <em>is</em> the workspace. Deleting it
         * would break no guard test, because those mock {@code PermissionService}. It would silently turn every
         * workspace gate into a lockout for everybody but tenant admins: this is the assertion that fails instead.
         */
        @Test
        void testWorkspaceScopeIsRefusedForEveryoneWithoutTheOwnershipResolver() {
            when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
            when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
                .thenReturn(Set.of("WORKSPACE_VIEW"));

            PermissionServiceImpl serviceWithoutResolver = service(List.of());

            assertThat(serviceWithoutResolver.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).isFalse();
        }

        private PermissionServiceImpl service() {
            return service(ownershipResolvers());
        }

        private PermissionServiceImpl service(List<ResourceOwnershipResolver> resourceOwnershipResolvers) {
            return new PermissionServiceImpl(
                currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
                workspaceUserRepository, resourceOwnershipResolvers, List.of(),
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));
        }

        private static List<ResourceOwnershipResolver> ownershipResolvers() {
            return List.of(new WorkspaceOwnershipResolver());
        }
    }

    @Nested
    class UnattendedRun {

        private static final long EDITOR_SKILL_ID = 200L;
        private static final long OTHER_USER_SKILL_ID = 100L;
        private static final long PROJECT_DEPLOYMENT_ID = 5L;

        private final AiSkillService aiSkillService = mock(AiSkillService.class);
        private final AuthorityService authorityService = mock(AuthorityService.class);
        private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        private final UserService userService = mock(UserService.class);

        private AiSkillOperations aiSkillOperations;
        private JobPrincipalAuthenticationRunner jobPrincipalAuthenticationRunner;

        @BeforeEach
        void beforeEach() {
            securityUtilsMock.close();

            User editor = user(1L, "editor");
            User alice = user(2L, "alice");

            when(userService.fetchUserByLogin("editor")).thenReturn(Optional.of(editor));
            when(userService.fetchUserByLogin("alice")).thenReturn(Optional.of(alice));
            when(userService.getUser("editor")).thenReturn(editor);
            when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority("ROLE_USER")));
            when(aiSkillService.fetchAiSkill(OTHER_USER_SKILL_ID)).thenReturn(Optional.of(aiSkill("alice")));
            when(aiSkillService.fetchAiSkill(EDITOR_SKILL_ID)).thenReturn(Optional.of(aiSkill("editor")));

            ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

            when(projectDeployment.getCreatedBy()).thenReturn("editor");
            when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
                .thenReturn(Optional.of(projectDeployment));

            PermissionServiceImpl permissionService = new PermissionServiceImpl(
                new CurrentUserResolver(userService), mock(PermissionScopeRegistry.class),
                mock(ProjectRepository.class),
                mock(WorkspaceScopeCacheService.class), mock(WorkspaceUserRepository.class),
                List.of(new AiSkillOwnershipResolver(aiSkillService, userService)), List.of(),
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(
                new AutomationMethodSecurityExpressionHandler(permissionService));

            ProxyFactory proxyFactory = new ProxyFactory(new AiSkillOperations());

            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvisor(
                AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

            aiSkillOperations = (AiSkillOperations) proxyFactory.getProxy();

            jobPrincipalAuthenticationRunner = new JobPrincipalAuthenticationRunner(
                List.of(
                    new ProjectDeploymentJobPrincipalAuthenticationResolver(
                        authorityService, projectDeploymentService, userService)));
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();

            securityUtilsMock = mockStatic(SecurityUtils.class);
        }

        @Test
        void testScheduledRunIsDeniedDeletingAnotherUsersSkill() {
            assertThatThrownBy(() -> runUnattended(() -> aiSkillOperations.deleteAiSkill(OTHER_USER_SKILL_ID)))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testScheduledRunStartedUnderTheSchedulerSystemPrincipalIsDeniedDeletingAnotherUsersSkill() {
            assertThatThrownBy(
                () -> SecurityUtils.runAsSystem(
                    () -> runUnattended(() -> aiSkillOperations.deleteAiSkill(OTHER_USER_SKILL_ID))))
                        .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testScheduledRunMayDeleteTheDeploymentOwnersOwnSkill() {
            assertThat(runUnattended(() -> aiSkillOperations.deleteAiSkill(EDITOR_SKILL_ID))).isEqualTo("deleted");
        }

        private <T> T runUnattended(Supplier<T> supplier) {
            return jobPrincipalAuthenticationRunner.run(PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, "job 1",
                supplier);
        }

        private static AiSkill aiSkill(String createdBy) {
            AiSkill aiSkill = new AiSkill();

            aiSkill.setCreatedBy(createdBy);

            return aiSkill;
        }

        private static Authority authority(String name) {
            Authority authority = new Authority();

            authority.setName(name);

            return authority;
        }

        private static User user(long id, String login) {
            User user = new User();

            user.setActivated(true);
            user.setAuthorityIds(List.of(1L));
            user.setId(id);
            user.setLogin(login);

            return user;
        }

        static class AiSkillOperations {

            @PreAuthorize("isResourceOwner(#id, 'AiSkill')")
            @SuppressWarnings("PMD.UnusedFormalParameter")
            public String deleteAiSkill(long id) {
                return "deleted";
            }
        }
    }
}
