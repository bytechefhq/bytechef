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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.config.WorkspacePermissionIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
@SpringBootTest(classes = WorkspacePermissionIntTestConfiguration.class, properties = "bytechef.edition=ee")
class PermissionServiceIntTest {

    private static final String LOGIN = "user";
    private static final long USER_ID = 2L;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionScopeRegistry permissionScopeRegistry;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceScopeCacheService workspaceScopeCacheService;

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
    void testEePermissionServiceMyScopeReadsRequireAuthentication() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> permissionService.getMyWorkspaceScopes(workspaceId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> permissionService.getMyWorkspaceRole(workspaceId))
            .isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> permissionService.getMyWorkspaceScopes(workspaceId))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> permissionService.getMyWorkspaceRole(workspaceId))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        authenticate(LOGIN, AuthorityConstants.USER);

        assertThatCode(() -> permissionService.getMyWorkspaceScopes(workspaceId)).doesNotThrowAnyException();
        assertThatCode(() -> permissionService.getMyWorkspaceRole(workspaceId)).doesNotThrowAnyException();
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

    private PermissionService createPermissionService(ResourceOwnershipResolver resourceOwnershipResolver) {
        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(resourceOwnershipResolver));
    }

    private static void authenticate(String login, String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
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
}
