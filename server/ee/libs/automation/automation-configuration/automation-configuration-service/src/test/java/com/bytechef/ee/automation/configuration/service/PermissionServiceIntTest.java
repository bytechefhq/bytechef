/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ProjectDeploymentEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentJobPrincipalAuthenticationResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.ee.automation.configuration.config.WorkspacePermissionIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.security.AiSkillOwnershipResolver;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.AuthorityServiceImpl;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.worker.security.JobPrincipalAuthenticationRunner;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.AopTestUtils;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Runs the EE {@link PermissionService} with method security, the real {@link CurrentUserResolver} over the seeded
 * platform users, the real scope cache and real workspace, membership, project and project-workflow rows.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        WorkspacePermissionIntTestConfiguration.class, PermissionServiceIntTest.ProjectDeploymentConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=mono,dev"
    })
class PermissionServiceIntTest {

    private static final String LOGIN = "user";
    private static final String CONNECTED_USER_EXTERNAL_ID = "external-user-1";
    private static final long DEPLOYMENT_OWNER_SKILL_ID = 200L;
    private static final long OTHER_USER_SKILL_ID = 100L;
    private static final String EMBEDDED_VIEW = "EMBEDDED_VIEW";
    private static final long USER_ID = 2L;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionScopeRegistry permissionScopeRegistry;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceScopeCacheService workspaceScopeCacheService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        RequestContextHolder.resetRequestAttributes();

        Workspace workspace = workspaceRepository.save(new Workspace("Permission Service " + UUID.randomUUID()));

        workspaceId = workspace.getId();

        authenticate(LOGIN, AuthorityConstants.USER);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDevelopmentViewerIsDeniedADeploymentWriteInDevelopment() {
        long developmentDeploymentId = saveDeployment(createProject(), Environment.DEVELOPMENT);

        authenticate(createMember(workspaceId, WorkspaceRole.VIEWER, Environment.DEVELOPMENT), AuthorityConstants.USER);

        ProjectDeployment projectDeployment = renamedDeployment(developmentDeploymentId);

        assertThatThrownBy(
            () -> projectDeploymentFacade.updateProjectDeployment(projectDeployment, List.of(), List.of()))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(storedDeploymentName(developmentDeploymentId)).isNotEqualTo(projectDeployment.getName());
    }

    @Test
    void testDevelopmentEditorIsAllowedTheDeploymentWriteInDevelopment() {
        long developmentDeploymentId = saveDeployment(createProject(), Environment.DEVELOPMENT);

        authenticate(createMember(workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT), AuthorityConstants.USER);

        ProjectDeployment projectDeployment = renamedDeployment(developmentDeploymentId);

        projectDeploymentFacade.updateProjectDeployment(projectDeployment, List.of(), List.of());

        assertThat(storedDeploymentName(developmentDeploymentId)).isEqualTo(projectDeployment.getName());
    }

    @Test
    void testDevelopmentViewerIsDeniedReadingAProductionDeploymentById() {
        long productionDeploymentId = saveDeployment(createProject(), Environment.PRODUCTION);

        authenticate(createMember(workspaceId, WorkspaceRole.VIEWER, Environment.DEVELOPMENT), AuthorityConstants.USER);

        assertThatThrownBy(() -> projectDeploymentFacade.getProjectDeployment(productionDeploymentId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testDevelopmentViewerIsAllowedReadingADevelopmentDeploymentById() {
        long developmentDeploymentId = saveDeployment(createProject(), Environment.DEVELOPMENT);

        authenticate(createMember(workspaceId, WorkspaceRole.VIEWER, Environment.DEVELOPMENT), AuthorityConstants.USER);

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacade.getProjectDeployment(
            developmentDeploymentId);

        assertThat(projectDeploymentDTO.id()).isEqualTo(developmentDeploymentId);
    }

    @Test
    void testDevelopmentViewerSeesRunsButNotTheirDetails() {
        long developmentDeploymentId = saveDeployment(createProject(), Environment.DEVELOPMENT);

        authenticate(createMember(workspaceId, WorkspaceRole.VIEWER, Environment.DEVELOPMENT), AuthorityConstants.USER);

        assertThat(permissionService.hasResourceScope(developmentDeploymentId, "ProjectDeployment", "EXECUTION_VIEW"))
            .isTrue();
        assertThat(
            permissionService.hasResourceScope(developmentDeploymentId, "ProjectDeployment", "EXECUTION_DETAILS_VIEW"))
                .isFalse();
    }

    @Test
    void testDevelopmentEditorSeesRunDetailsOnlyInDevelopment() {
        Project project = createProject();

        long developmentDeploymentId = saveDeployment(project, Environment.DEVELOPMENT);
        long productionDeploymentId = saveDeployment(project, Environment.PRODUCTION);

        authenticate(createMember(workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT), AuthorityConstants.USER);

        assertThat(
            permissionService.hasResourceScope(developmentDeploymentId, "ProjectDeployment", "EXECUTION_DETAILS_VIEW"))
                .isTrue();
        assertThat(
            permissionService.hasResourceScope(productionDeploymentId, "ProjectDeployment", "EXECUTION_DETAILS_VIEW"))
                .isFalse();
    }

    @Test
    void testMemberOfAnotherWorkspaceIsDeniedTheDeploymentById() {
        long developmentDeploymentId = saveDeployment(createProject(), Environment.DEVELOPMENT);

        Workspace otherWorkspace = workspaceRepository.save(new Workspace("Other " + UUID.randomUUID()));

        authenticate(createMember(otherWorkspace.getId(), WorkspaceRole.EDITOR, null), AuthorityConstants.USER);

        ProjectDeployment projectDeployment = renamedDeployment(developmentDeploymentId);

        assertThatThrownBy(
            () -> projectDeploymentFacade.updateProjectDeployment(projectDeployment, List.of(), List.of()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> projectDeploymentFacade.getProjectDeployment(developmentDeploymentId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCanUseConnectionInWorkflowGrantsAConnectionOfTheWorkflowsWorkspaceAndEnvironment() {
        String workflowId = createProjectWorkflow();

        addMember(WorkspaceRole.VIEWER, Environment.DEVELOPMENT);

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId, Environment.DEVELOPMENT);

        assertThat(connectionPermissionService.canUseConnectionInWorkflow(1L, workflowId, Environment.DEVELOPMENT))
            .isTrue();
    }

    @Test
    void testCanUseConnectionInWorkflowDeniesAConnectionOfAnotherWorkspace() {
        String workflowId = createProjectWorkflow();

        addMember(WorkspaceRole.ADMIN);

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId + 1, Environment.DEVELOPMENT);

        assertThat(connectionPermissionService.canUseConnectionInWorkflow(1L, workflowId, Environment.DEVELOPMENT))
            .isFalse();
    }

    @Test
    void testCanUseConnectionInWorkflowDeniesAConnectionOfAnotherEnvironment() {
        String workflowId = createProjectWorkflow();

        addMember(WorkspaceRole.ADMIN);

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId, Environment.PRODUCTION);

        assertThat(connectionPermissionService.canUseConnectionInWorkflow(1L, workflowId, Environment.DEVELOPMENT))
            .isFalse();
    }

    @Test
    void testCanUseConnectionInWorkflowDeniesWithoutTheConnectionViewScope() {
        String workflowId = createProjectWorkflow();

        createCustomRole(WorkspacePermissionIntTestConfiguration.CUSTOM_ROLE_ID);

        workspaceUserRepository.save(
            WorkspaceUser.forCustomRole(
                USER_ID, workspaceId, WorkspacePermissionIntTestConfiguration.CUSTOM_ROLE_ID,
                Environment.DEVELOPMENT));

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId, Environment.DEVELOPMENT);

        assertThat(connectionPermissionService.canUseConnectionInWorkflow(1L, workflowId, Environment.DEVELOPMENT))
            .isFalse();
    }

    @Test
    void testCanUseConnectionInWorkspaceGrantsAConnectionOfTheWorkspaceAndEnvironment() {
        addMember(WorkspaceRole.VIEWER, Environment.DEVELOPMENT);

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId, Environment.DEVELOPMENT);

        assertThat(connectionPermissionService.canUseConnectionInWorkspace(1L, workspaceId, Environment.DEVELOPMENT))
            .isTrue();
    }

    @Test
    void testCanUseConnectionInWorkspaceDeniesAConnectionOfAnotherWorkspace() {
        addMember(WorkspaceRole.ADMIN);

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId + 1, Environment.DEVELOPMENT);

        assertThat(connectionPermissionService.canUseConnectionInWorkspace(1L, workspaceId, Environment.DEVELOPMENT))
            .isFalse();
    }

    @Test
    void testCanUseConnectionInWorkspaceDeniesAConnectionOfAnotherEnvironment() {
        addMember(WorkspaceRole.ADMIN);

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId, Environment.PRODUCTION);

        assertThat(connectionPermissionService.canUseConnectionInWorkspace(1L, workspaceId, Environment.DEVELOPMENT))
            .isFalse();
    }

    @Test
    void testCanUseConnectionInWorkflowGrantsATenantAdmin() {
        String workflowId = createProjectWorkflow();

        authenticate(LOGIN, AuthorityConstants.ADMIN);

        PermissionService connectionPermissionService = createConnectionPermissionService(
            workspaceId + 1, Environment.PRODUCTION);

        assertThat(connectionPermissionService.canUseConnectionInWorkflow(1L, workflowId, Environment.DEVELOPMENT))
            .isTrue();
    }

    @Test
    void testEePermissionServiceMyScopeReadsRequireAuthentication() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> permissionService.getMyWorkspaceScopes(workspaceId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> permissionService.getMyWorkspaceRole(workspaceId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> permissionService.getMyWorkspaceScopes(workspaceId, Environment.PRODUCTION))
            .isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> permissionService.getMyWorkspaceScopes(workspaceId))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> permissionService.getMyWorkspaceRole(workspaceId))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        authenticate(LOGIN, AuthorityConstants.USER);

        assertThatCode(() -> permissionService.getMyWorkspaceScopes(workspaceId)).doesNotThrowAnyException();
        assertThatCode(() -> permissionService.getMyWorkspaceRole(workspaceId)).doesNotThrowAnyException();
        assertThatCode(() -> permissionService.getMyWorkspaceScopes(workspaceId, Environment.PRODUCTION))
            .doesNotThrowAnyException();
    }

    @Test
    void testScheduledRunIsDeniedDeletingAnotherUsersSkill() {
        UnattendedRun unattendedRun = createUnattendedRun();

        assertThatThrownBy(() -> unattendedRun.run(() -> unattendedRun.deleteAiSkill(OTHER_USER_SKILL_ID)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testScheduledRunStartedUnderTheSchedulerSystemPrincipalIsDeniedDeletingAnotherUsersSkill() {
        UnattendedRun unattendedRun = createUnattendedRun();

        assertThatThrownBy(
            () -> SecurityUtils.runAsSystem(
                () -> unattendedRun.run(() -> unattendedRun.deleteAiSkill(OTHER_USER_SKILL_ID))))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testScheduledRunMayDeleteTheDeploymentOwnersOwnSkill() {
        UnattendedRun unattendedRun = createUnattendedRun();

        assertThat(unattendedRun.run(() -> unattendedRun.deleteAiSkill(DEPLOYMENT_OWNER_SKILL_ID)))
            .isEqualTo("deleted");
    }

    @Test
    void testIsTenantAdminTrueWhenAuthorityPresent() {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

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
        SecurityContextHolder.clearContext();

        assertThat(permissionService.isCurrentUser(USER_ID)).isFalse();
    }

    @Test
    void testHasWorkspaceRoleShortCircuitsForTenantAdmin() {
        // No membership row exists, so only the tenant-admin short-circuit can grant.
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isTrue();
    }

    @Test
    void testHasWorkspaceRoleAdminUserSatisfiesAllMinimums() {
        addMember(WorkspaceRole.ADMIN);

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(workspaceId, "EDITOR")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(workspaceId, "ADMIN")).isTrue();
    }

    @Test
    void testHasWorkspaceRoleViewerCannotEdit() {
        addMember(WorkspaceRole.VIEWER);

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(workspaceId, "EDITOR")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(workspaceId, "ADMIN")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleFalseForNonMember() {
        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleDeniesAMemberInExplicitMode() {
        addMember(WorkspaceRole.ADMIN, Environment.DEVELOPMENT);
        addMember(WorkspaceRole.ADMIN, Environment.STAGING);
        addMember(WorkspaceRole.ADMIN, Environment.PRODUCTION);

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "ADMIN")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isFalse();
    }

    @Test
    void testHasResourceRoleDeniesAMemberInExplicitMode() {
        addMember(WorkspaceRole.ADMIN, Environment.PRODUCTION);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("Connection", ResourceOwner.ofWorkspace(workspaceId)));

        assertThat(resourcePermissionService.hasResourceRole(1L, "Connection", "ADMIN")).isFalse();
    }

    @Test
    void testGetMyWorkspaceRoleNullForAMemberInExplicitMode() {
        addMember(WorkspaceRole.ADMIN, Environment.PRODUCTION);

        assertThat(permissionService.getMyWorkspaceRole(workspaceId)).isNull();
    }

    @Test
    void testHasWorkspaceRoleFailsClosedOnInvalidRoleOrdinal() {
        // A corrupted or legacy workspace_role ordinal hydrated via Spring Data JDBC (which bypasses the constructor's
        // range validation) must fail closed - deny - rather than throw ArrayIndexOutOfBoundsException as a 500.
        corruptRoleOrdinal(addMember(WorkspaceRole.ADMIN));

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleFalseOnUnknownRoleName() {
        addMember(WorkspaceRole.ADMIN);

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "NOT_A_ROLE")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleFalseWhenSecurityContextEmpty() {
        addMember(WorkspaceRole.ADMIN);

        SecurityContextHolder.clearContext();

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeShortCircuitsForTenantAdmin() {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_DELETE")).isTrue();
    }

    @Test
    void testHasWorkspaceScopeReturnsTrueWhenScopeGranted() {
        addMember(WorkspaceRole.EDITOR);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkspaceScopeReturnsFalseWhenScopeMissing() {
        addMember(WorkspaceRole.VIEWER);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeFalseOnUnknownScopeName() {
        // Scope names are plain strings validated by set-membership, so a name no role grants fails closed.
        addMember(WorkspaceRole.ADMIN);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "NOT_A_SCOPE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeFalseWhenSecurityContextEmpty() {
        addMember(WorkspaceRole.ADMIN);

        SecurityContextHolder.clearContext();

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeFalseWhenAuthenticatedLoginHasNoPlatformUser() {
        // Defense-in-depth: an authenticated principal whose login resolves to no platform user must fail closed -
        // deny - rather than bubble UserNotFoundException as a 500.
        addMember(WorkspaceRole.ADMIN);

        authenticate("ghost", AuthorityConstants.USER);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectShortCircuitsForTenantAdmin() {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        assertThat(permissionService.hasWorkspaceScopeForProject(Long.MAX_VALUE, "WORKFLOW_DELETE")).isTrue();
    }

    @Test
    void testHasWorkspaceScopeForProjectResolvesOwningWorkspace() {
        addMember(WorkspaceRole.VIEWER);

        Project project = createProject();

        assertThat(permissionService.hasWorkspaceScopeForProject(project.getId(), "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(project.getId(), "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectFalseForUnknownProject() {
        addMember(WorkspaceRole.ADMIN);

        assertThat(permissionService.hasWorkspaceScopeForProject(Long.MAX_VALUE, "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testGetMyWorkspaceScopesEmptyWhenSecurityContextEmpty() {
        // @PreAuthorize("isAuthenticated()") rejects an empty context at the proxy, so the fail-closed branch of the
        // implementation is exercised on the target bean behind the security proxy.
        addMember(WorkspaceRole.ADMIN);

        SecurityContextHolder.clearContext();

        PermissionService targetPermissionService = AopTestUtils.getUltimateTargetObject(permissionService);

        assertThat(targetPermissionService.getMyWorkspaceScopes(workspaceId)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceScopesReturnsAllScopesForTenantAdmin() {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        Set<String> scopes = permissionService.getMyWorkspaceScopes(workspaceId);

        assertThat(scopes).isNotEmpty()
            .isEqualTo(permissionScopeRegistry.getAllScopeNames());
    }

    @Test
    void testGetMyWorkspaceScopesDelegatesToCache() {
        addMember(WorkspaceRole.VIEWER);

        assertThat(permissionService.getMyWorkspaceScopes(workspaceId))
            .isEqualTo(permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER))
            .isEqualTo(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId));
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentResolvesThatEnvironmentsRole() {
        addMember(WorkspaceRole.EDITOR, Environment.STAGING);

        assertThat(permissionService.getMyWorkspaceScopes(workspaceId, Environment.STAGING))
            .isEqualTo(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR));
        assertThat(permissionService.getMyWorkspaceScopes(workspaceId, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentReturnsAllScopesForTenantAdmin() {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        assertThat(permissionService.getMyWorkspaceScopes(workspaceId, Environment.PRODUCTION))
            .isEqualTo(permissionScopeRegistry.getAllScopeNames());
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentEmptyWhenSecurityContextEmpty() {
        addMember(WorkspaceRole.ADMIN, Environment.PRODUCTION);

        SecurityContextHolder.clearContext();

        PermissionService targetPermissionService = AopTestUtils.getUltimateTargetObject(permissionService);

        assertThat(targetPermissionService.getMyWorkspaceScopes(workspaceId, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceRoleNullWhenSecurityContextEmpty() {
        addMember(WorkspaceRole.ADMIN);

        SecurityContextHolder.clearContext();

        PermissionService targetPermissionService = AopTestUtils.getUltimateTargetObject(permissionService);

        assertThat(targetPermissionService.getMyWorkspaceRole(workspaceId)).isNull();
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdminForTenantAdmin() {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        assertThat(permissionService.getMyWorkspaceRole(workspaceId)).isEqualTo("ADMIN");
    }

    @Test
    void testGetMyWorkspaceRoleReturnsMembershipRole() {
        addMember(WorkspaceRole.EDITOR);

        assertThat(permissionService.getMyWorkspaceRole(workspaceId)).isEqualTo("EDITOR");
    }

    @Test
    void testGetMyWorkspaceRoleIgnoresAnImplicitRowBesideEnvironmentRows() {
        addMember(WorkspaceRole.ADMIN);
        addMember(WorkspaceRole.VIEWER, Environment.DEVELOPMENT);

        assertThat(permissionService.getMyWorkspaceRole(workspaceId)).isNull();
    }

    @Test
    void testHasWorkspaceRoleIgnoresAnImplicitRowBesideEnvironmentRows() {
        addMember(WorkspaceRole.ADMIN);
        addMember(WorkspaceRole.VIEWER, Environment.DEVELOPMENT);

        assertThat(permissionService.hasWorkspaceRole(workspaceId, "VIEWER")).isFalse();
    }

    @Test
    void testGetMyWorkspaceRoleReturnsNullForNonMember() {
        assertThat(permissionService.getMyWorkspaceRole(workspaceId)).isNull();
    }

    @Test
    void testGetMyWorkspaceRoleNullOnInvalidRoleOrdinal() {
        corruptRoleOrdinal(addMember(WorkspaceRole.EDITOR));

        assertThat(permissionService.getMyWorkspaceRole(workspaceId)).isNull();
    }

    @Test
    void testHasWorkflowScopeGrantsUnderAutomationAuthorizationSkip() throws Throwable {
        // Embedded -> automation delegation: the principal has no platform user and the workflow resolves to no
        // project, so only the skip flag short-circuit can grant.
        authenticate("api-key-principal", AuthorityConstants.USER);

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkflowScope("workflow-" + UUID.randomUUID(), "WORKFLOW_EDIT"));

        assertThat(granted).isTrue();
    }

    @Test
    void testIsCurrentUserDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticate(CONNECTED_USER_EXTERNAL_ID, AuthorityConstants.USER);

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.isCurrentUser(USER_ID));

        assertThat(granted).isFalse();
    }

    @Test
    void testIsResourceOwnerDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticate(CONNECTED_USER_EXTERNAL_ID, AuthorityConstants.USER);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("ApiKey", ResourceOwner.ofUser(USER_ID)));

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> resourcePermissionService.isResourceOwner("ApiKey", 1L));

        assertThat(granted).isFalse();
    }

    @Test
    void testHasResourceRoleDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticate(CONNECTED_USER_EXTERNAL_ID, AuthorityConstants.USER);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("KnowledgeBase", ResourceOwner.ofWorkspace(workspaceId)));

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> resourcePermissionService.hasResourceRole(1L, "KnowledgeBase", "VIEWER"));

        assertThat(granted).isFalse();
    }

    @Test
    void testHasWorkspaceRoleDeniesAConnectedUserUnderAutomationAuthorizationSkip() throws Throwable {
        authenticate(CONNECTED_USER_EXTERNAL_ID, AuthorityConstants.USER);

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkspaceRole(workspaceId, "VIEWER"));

        assertThat(granted).isFalse();
    }

    @Test
    void testHasWorkspaceScopeInEveryEnvironmentDeniesAConnectedUserUnderAutomationAuthorizationSkip()
        throws Throwable {

        authenticate(CONNECTED_USER_EXTERNAL_ID, AuthorityConstants.USER);

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasWorkspaceScopeInEveryEnvironment(workspaceId, "WORKSPACE_MEMBER_MANAGE"));

        assertThat(granted).isFalse();
    }

    @Test
    void testTenantAdminPassesThePrivilegedChecksUnderAutomationAuthorizationSkip() throws Throwable {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("KnowledgeBase", ResourceOwner.ofWorkspace(workspaceId)));

        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> resourcePermissionService.hasWorkspaceScopeInEveryEnvironment(
                workspaceId, "WORKSPACE_MEMBER_MANAGE") &&
                resourcePermissionService.hasWorkspaceRole(workspaceId, "ADMIN") &&
                resourcePermissionService.hasResourceRole(1L, "KnowledgeBase", "ADMIN") &&
                resourcePermissionService.isResourceOwner("KnowledgeBase", 1L) &&
                resourcePermissionService.isCurrentUser(USER_ID));

        assertThat(granted).isTrue();
    }

    @Test
    void testHasWorkflowScopeFalseWithoutSkipWhenWorkflowUnknown() {
        addMember(WorkspaceRole.ADMIN);

        assertThat(permissionService.hasWorkflowScope("workflow-" + UUID.randomUUID(), "WORKFLOW_EDIT")).isFalse();
    }

    @Test
    void testHasWorkflowScopeResolvesProjectWorkspace() {
        addMember(WorkspaceRole.EDITOR);

        Project project = createProject();

        String workflowId = "wf-" + UUID.randomUUID();

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId, UUID.randomUUID()));

        assertThat(permissionService.hasWorkflowScope(workflowId, "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowDeniesANonAdminWhenTheWorkflowBelongsToNoProject() {
        addMember(WorkspaceRole.ADMIN, Environment.DEVELOPMENT);

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow(
                "unknown-workflow-" + UUID.randomUUID(), "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                    .isFalse();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowGrantsATenantAdminWhenTheWorkflowBelongsToNoProject() {
        authenticate(LOGIN, AuthorityConstants.ADMIN);

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow(
                "integration-workflow-" + UUID.randomUUID(), "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                    .isTrue();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowDeniesAProjectWorkflowWithoutTheScope() {
        addMember(WorkspaceRole.VIEWER, Environment.DEVELOPMENT);

        String workflowId = createProjectWorkflow();

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow(workflowId, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowGrantsAProjectWorkflowWithTheScope() {
        addMember(WorkspaceRole.EDITOR, Environment.DEVELOPMENT);

        String workflowId = createProjectWorkflow();

        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow(workflowId, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();
    }

    @Test
    void testHasWorkflowScopeDeniesAMemberWhoHoldsTheScopeOnlyInProduction() {
        addMember(WorkspaceRole.EDITOR, Environment.PRODUCTION);
        addMember(WorkspaceRole.VIEWER, Environment.DEVELOPMENT);

        String workflowId = createProjectWorkflow();

        assertThat(permissionService.hasWorkflowScope(workflowId, "WORKFLOW_EDIT", Environment.DEVELOPMENT)).isFalse();
        assertThat(permissionService.hasWorkflowScope(workflowId, "WORKFLOW_EDIT", Environment.PRODUCTION)).isTrue();
        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow(workflowId, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();
    }

    @Test
    void testHasResourceScopeUsesWorkspaceScope() {
        addMember(WorkspaceRole.EDITOR);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("Connection", ResourceOwner.ofWorkspace(workspaceId)));

        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", "WORKFLOW_DELETE")).isTrue();
        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", "NOT_A_SCOPE")).isFalse();
    }

    @Test
    void testHasResourceScopeNoWorkspaceFailsClosed() {
        addMember(WorkspaceRole.ADMIN);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("Connection", ResourceOwner.unknown()));

        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testTwoResolversForOneTypeChainUntilOneKnowsTheOwner() {
        EmbeddedScopeCacheService embeddedScopeCacheService = mock(EmbeddedScopeCacheService.class);

        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID)).thenReturn(Set.of(EMBEDDED_VIEW));

        PermissionService resourcePermissionService = createPermissionService(
            List.of(resolver("Connection", ResourceOwner.unknown()),
                resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(), embeddedScopeCacheService);

        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", EMBEDDED_VIEW)).isTrue();
    }

    @Test
    void testAnEmbeddedOwnerWithAnEnvironmentIsCheckedInThatEnvironment() {
        EmbeddedScopeCacheService embeddedScopeCacheService = mock(EmbeddedScopeCacheService.class);

        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of(EMBEDDED_VIEW));
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION)).thenReturn(Set.of());

        PermissionService resourcePermissionService = createPermissionService(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.of(Environment.PRODUCTION))), embeddedScopeCacheService);

        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", EMBEDDED_VIEW)).isFalse();
    }

    @Test
    void testAnEmbeddedOwnerWithoutAnEnvironmentUsesTheUnawareAnswer() {
        EmbeddedScopeCacheService embeddedScopeCacheService = mock(EmbeddedScopeCacheService.class);

        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID)).thenReturn(Set.of(EMBEDDED_VIEW));

        PermissionService resourcePermissionService = createPermissionService(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.empty())), embeddedScopeCacheService);

        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", EMBEDDED_VIEW)).isTrue();
    }

    @Test
    void testEveryResolverUnknownDeniesWithoutThrowing() {
        addMember(WorkspaceRole.ADMIN);

        PermissionService resourcePermissionService = createPermissionService(
            List.of(resolver("Connection", ResourceOwner.unknown()), resolver("Connection", ResourceOwner.unknown())),
            List.of(), mock(EmbeddedScopeCacheService.class));

        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", EMBEDDED_VIEW)).isFalse();
    }

    @Test
    void testAnEnvironmentResolverThatThrowsDenies() {
        addMember(WorkspaceRole.EDITOR);

        PermissionService resourcePermissionService = createPermissionService(
            List.of(
                resolver("Connection", ResourceOwner.unknown()),
                resolver("Connection", ResourceOwner.ofWorkspace(workspaceId))),
            List.of(environmentResolver("Connection", Optional.empty()), throwingEnvironmentResolver("Connection")),
            mock(EmbeddedScopeCacheService.class));

        assertThat(resourcePermissionService.hasResourceScope(1L, "Connection", "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testAnEmbeddedOwnerIsCheckedInTheNamedEnvironment() {
        EmbeddedScopeCacheService embeddedScopeCacheService = mock(EmbeddedScopeCacheService.class);

        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.STAGING))
            .thenReturn(Set.of(EMBEDDED_VIEW));

        PermissionService resourcePermissionService = createPermissionService(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())), List.of(), embeddedScopeCacheService);

        assertThat(
            resourcePermissionService.hasResourceScopeInEnvironment(
                1L, "Connection", EMBEDDED_VIEW, Environment.STAGING))
                    .isTrue();
    }

    @Test
    void testIsResourceOwnerComparesTheOwnerOfAnEmbeddedResource() {
        PermissionService resourcePermissionService = createPermissionService(
            resolver("ApiKey", ResourceOwner.ofEmbedded(USER_ID)));

        assertThat(resourcePermissionService.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testAnEmbeddedOwnerIsDeniedByResourceRole() {
        addMember(WorkspaceRole.ADMIN);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("KnowledgeBase", ResourceOwner.ofEmbedded()));

        assertThat(resourcePermissionService.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isFalse();
    }

    @Test
    void testAnEmbeddedConnectionIsDeniedInAWorkspace() {
        addMember(WorkspaceRole.ADMIN);

        PermissionService resourcePermissionService = createPermissionService(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.of(Environment.PRODUCTION))),
            mock(EmbeddedScopeCacheService.class));

        assertThat(resourcePermissionService.canUseConnectionInWorkspace(1L, workspaceId, Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testIsResourceOwnerMatchAllows() {
        PermissionService resourcePermissionService = createPermissionService(
            resolver("ApiKey", ResourceOwner.ofUser(USER_ID)));

        assertThat(resourcePermissionService.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testIsResourceOwnerMismatchDenies() {
        PermissionService resourcePermissionService = createPermissionService(
            resolver("ApiKey", ResourceOwner.ofUser(USER_ID + 1)));

        assertThat(resourcePermissionService.isResourceOwner("ApiKey", 1L)).isFalse();
    }

    @Test
    void testHasResourceRoleChecksWorkspaceRole() {
        addMember(WorkspaceRole.EDITOR);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("KnowledgeBase", ResourceOwner.ofWorkspace(workspaceId)));

        assertThat(resourcePermissionService.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isTrue();
        assertThat(resourcePermissionService.hasResourceRole(1L, "KnowledgeBase", "ADMIN")).isFalse();
    }

    @Test
    void testHasResourceRoleNoWorkspaceFailsClosed() {
        addMember(WorkspaceRole.ADMIN);

        PermissionService resourcePermissionService = createPermissionService(
            resolver("KnowledgeBase", ResourceOwner.unknown()));

        assertThat(resourcePermissionService.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isFalse();
    }

    @Test
    void testEvictWorkspaceScopeCacheDelegates() {
        WorkspaceUser workspaceUser = addMember(WorkspaceRole.VIEWER);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_EDIT")).isFalse();

        changeRole(workspaceUser, WorkspaceRole.EDITOR);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_EDIT")).isFalse();

        permissionService.evictWorkspaceScopeCache(USER_ID, workspaceId);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testEvictAllWorkspaceScopeCacheDelegates() {
        WorkspaceUser workspaceUser = addMember(WorkspaceRole.VIEWER);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_EDIT")).isFalse();

        changeRole(workspaceUser, WorkspaceRole.EDITOR);

        permissionService.evictAllWorkspaceScopeCache();

        assertThat(permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_EDIT")).isTrue();
    }

    private WorkspaceUser addMember(WorkspaceRole workspaceRole) {
        return workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, workspaceRole));
    }

    private WorkspaceUser addMember(WorkspaceRole workspaceRole, Environment environment) {
        return workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, workspaceRole, environment));
    }

    private void changeRole(WorkspaceUser workspaceUser, WorkspaceRole workspaceRole) {
        WorkspaceUser persistedWorkspaceUser = workspaceUserRepository.findById(workspaceUser.getId())
            .orElseThrow();

        persistedWorkspaceUser.assignRole(workspaceRole);

        workspaceUserRepository.save(persistedWorkspaceUser);
    }

    private void corruptRoleOrdinal(WorkspaceUser workspaceUser) {
        jdbcTemplate.update("UPDATE workspace_user SET workspace_role = 999 WHERE id = ?", workspaceUser.getId());
    }

    private Project createProject() {
        Project project = new Project();

        project.setName("Permission Service Project " + UUID.randomUUID());
        project.setWorkspaceId(workspaceId);

        return projectRepository.save(project);
    }

    private String createProjectWorkflow() {
        Project project = createProject();

        String workflowId = "wf-" + UUID.randomUUID();

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId, UUID.randomUUID()));

        return workflowId;
    }

    private String createMember(long memberWorkspaceId, WorkspaceRole workspaceRole, Environment environment) {
        String login = "member-" + UUID.randomUUID();

        User user = new User();

        user.setActivated(true);
        user.setEmail(login + "@localhost.com");
        user.setLogin(login);
        user.setPassword(String.valueOf(UUID.randomUUID()));
        user.setUuid(String.valueOf(UUID.randomUUID()));

        user = userRepository.save(user);

        workspaceUserRepository.save(
            WorkspaceUser.forRole(user.getId(), memberWorkspaceId, workspaceRole, environment));

        return login;
    }

    private PermissionService createConnectionPermissionService(
        long connectionWorkspaceId, Environment connectionEnvironment) {

        ResourceEnvironmentResolver connectionEnvironmentResolver = new ResourceEnvironmentResolver() {

            @Override
            public String resourceType() {
                return "Connection";
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                return Optional.of(connectionEnvironment);
            }
        };

        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(resolver("Connection", ResourceOwner.ofWorkspace(connectionWorkspaceId))),
            List.of(connectionEnvironmentResolver),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class),
            mock(EmbeddedScopeCacheService.class), mock(EmbeddedUserRepository.class));
    }

    private void createCustomRole(long customRoleId) {
        jdbcTemplate.update(
            "INSERT INTO custom_role (id, name, created_date, created_by, last_modified_date, last_modified_by, " +
                "version) VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0) " +
                "ON CONFLICT (id) DO NOTHING",
            customRoleId, "Custom Role " + customRoleId);
    }

    private PermissionService createPermissionService(
        List<ResourceOwnershipResolver> resourceOwnershipResolvers,
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers,
        EmbeddedScopeCacheService embeddedScopeCacheService) {

        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, resourceOwnershipResolvers, resourceEnvironmentResolvers,
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class),
            embeddedScopeCacheService, mock(EmbeddedUserRepository.class));
    }

    private PermissionService createPermissionService(ResourceOwnershipResolver resourceOwnershipResolver) {
        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(resourceOwnershipResolver), List.of(),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class),
            mock(EmbeddedScopeCacheService.class), mock(EmbeddedUserRepository.class));
    }

    private UnattendedRun createUnattendedRun() {
        long projectDeploymentId = saveDeployment(createProject(), Environment.DEVELOPMENT);

        jdbcTemplate.update("UPDATE project_deployment SET created_by = ? WHERE id = ?", LOGIN, projectDeploymentId);

        SecurityContextHolder.clearContext();

        AiSkillService aiSkillService = mock(AiSkillService.class);

        when(aiSkillService.fetchAiSkill(OTHER_USER_SKILL_ID)).thenReturn(Optional.of(aiSkill("admin")));
        when(aiSkillService.fetchAiSkill(DEPLOYMENT_OWNER_SKILL_ID)).thenReturn(Optional.of(aiSkill(LOGIN)));

        PermissionService aiSkillPermissionService = createPermissionService(
            new AiSkillOwnershipResolver(aiSkillService, userService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        preAuthorizeAuthorizationManager.setExpressionHandler(
            new AutomationMethodSecurityExpressionHandler(
                aiSkillPermissionService, beanFactory.getBeanProvider(MethodSecurityExpressionContributor.class)));

        ProxyFactory proxyFactory = new ProxyFactory(new AiSkillOperations());

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        JobPrincipalAuthenticationRunner jobPrincipalAuthenticationRunner = new JobPrincipalAuthenticationRunner(
            List.of(
                new ProjectDeploymentJobPrincipalAuthenticationResolver(
                    authorityService, projectDeploymentService, userService)));

        return new UnattendedRun(
            (AiSkillOperations) proxyFactory.getProxy(), jobPrincipalAuthenticationRunner, projectDeploymentId);
    }

    private ProjectDeployment renamedDeployment(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        projectDeployment.setName("renamed-" + UUID.randomUUID());

        return projectDeployment;
    }

    private long saveDeployment(Project project, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(environment.name() + "-" + UUID.randomUUID());
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        return projectDeployment.getId();
    }

    private String storedDeploymentName(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        return projectDeployment.getName();
    }

    private static void authenticate(String login, String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private static ResourceEnvironmentResolver throwingEnvironmentResolver(String resourceType) {
        return new ResourceEnvironmentResolver() {

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                throw new IllegalStateException("boom");
            }
        };
    }

    private static AiSkill aiSkill(String createdBy) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setCreatedBy(createdBy);

        return aiSkill;
    }

    private static ResourceEnvironmentResolver environmentResolver(
        String resourceType, Optional<Environment> environment) {

        return new ResourceEnvironmentResolver() {

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                return environment;
            }
        };
    }

    private static ResourceOwnershipResolver resolver(String resourceType, ResourceOwner resourceOwner) {
        return new ResourceOwnershipResolver() {

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return resourceOwner;
            }
        };
    }

    static class AiSkillOperations {

        @PreAuthorize("isResourceOwner(#id, 'AiSkill')")
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public String deleteAiSkill(long id) {
            return "deleted";
        }
    }

    private record UnattendedRun(
        AiSkillOperations aiSkillOperations, JobPrincipalAuthenticationRunner jobPrincipalAuthenticationRunner,
        long projectDeploymentId) {

        String deleteAiSkill(long id) {
            return aiSkillOperations.deleteAiSkill(id);
        }

        <T> T run(Supplier<T> supplier) {
            return jobPrincipalAuthenticationRunner.run(
                PlatformType.AUTOMATION, projectDeploymentId, "job 1", supplier);
        }
    }

    @TestConfiguration
    @Import({
        AuthorityServiceImpl.class, ProjectDeploymentEnvironmentResolver.class,
        ProjectDeploymentOwnershipResolver.class, ProjectDeploymentServiceImpl.class, ProjectOwnershipResolver.class
    })
    static class ProjectDeploymentConfiguration {

        @Bean
        ProjectDeploymentFacade projectDeploymentFacade(
            PermissionService permissionService, ProjectDeploymentService projectDeploymentService)
            throws ReflectiveOperationException {

            Constructor<?>[] constructors = ProjectDeploymentFacadeImpl.class.getConstructors();

            Constructor<?> constructor = constructors[0];

            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];

            for (int index = 0; index < parameterTypes.length; index++) {
                Class<?> parameterType = parameterTypes[index];

                if (parameterType == PermissionService.class) {
                    arguments[index] = permissionService;
                } else if (parameterType == ProjectDeploymentService.class) {
                    arguments[index] = projectDeploymentService;
                } else {
                    arguments[index] = mock(parameterType);
                }
            }

            return (ProjectDeploymentFacade) constructor.newInstance(arguments);
        }
    }
}
