/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.ai.agent.security.AiAgentOwnershipResolver;
import com.bytechef.automation.ai.agent.security.AiAgentVisibilityProvider;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The agent half of {@link PermissionServiceVisibilityTest}, wired to the REAL {@link AiAgentVisibilityProvider} and
 * {@link AiAgentOwnershipResolver} rather than to stand-ins. That is the point of the class: an inline anonymous
 * provider would keep every assertion here green with the production provider deleted, which is precisely the failure
 * mode this branch has been shedding.
 *
 * <p>
 * What it pins is the sentence the agent facade's javadoc used to have to disclaim — that {@code AGENT_VIEW} in a
 * workspace lets a member reach every agent in it. It no longer does: a PRIVATE agent belonging to a colleague is
 * denied, and a grant on its backing project is what restores it. The grant lookup deliberately keys on
 * {@code ("Project", projectId)} and not on the agent, because an agent inherits its reach; a resolver asked about
 * {@code "AiAgent"} here would mean the provider had stopped declaring its parent type.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionServiceAgentVisibilityTest {

    private static final long AGENT_ID = 40L;
    private static final long PROJECT_ID = 41L;
    private static final long WORKSPACE_AGENT_ID = 42L;
    private static final long WORKSPACE_PROJECT_ID = 43L;
    private static final long WORKSPACE_ID = 1L;

    private static final AtomicReference<String> LAST_RESOLVED_TYPE = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPrivateAgentDeniedToNonGranteeHoldingAgentView() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope(AGENT_ID, "AiAgent", "AGENT_VIEW"))
            .as("ana holds AGENT_VIEW in the workspace, but the agent is ivica's and withheld")
            .isFalse();
    }

    @Test
    void testPrivateAgentAllowedToGrantee() {
        authenticate("ana");

        assertThat(permissionService(Set.of(PROJECT_ID)).hasResourceScope(AGENT_ID, "AiAgent", "AGENT_VIEW"))
            .as("a grant restores the access ana would have had if the agent were workspace-visible")
            .isTrue();
    }

    @Test
    void testPrivateAgentAllowedToItsOwner() {
        authenticate("ivica");

        assertThat(permissionService(Set.of()).hasResourceScope(AGENT_ID, "AiAgent", "AGENT_VIEW"))
            .as("the agent's creator is the created_by of its backing project")
            .isTrue();
    }

    @Test
    void testWorkspaceVisibleAgentAllowedToMemberHoldingAgentView() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope(WORKSPACE_AGENT_ID, "AiAgent", "AGENT_VIEW"))
            .as("nothing here narrows an agent whose owner left it shared")
            .isTrue();
    }

    /**
     * The scope is still checked. Without this the two tests above would also pass with the visibility precondition
     * removed and the scope one broken in its place.
     */
    @Test
    void testGranteeStillNeedsTheScope() {
        authenticate("ana");

        assertThat(permissionService(Set.of(PROJECT_ID)).hasResourceScope(AGENT_ID, "AiAgent", "AGENT_EDIT"))
            .as("ana holds AGENT_VIEW only; a grant is not a scope")
            .isFalse();
    }

    /**
     * The hole {@code AiAgentSharingFacadeImpl}'s second disjunct exists to close, pinned as the two halves it is made
     * of rather than as its conclusion. Ana is a workspace ADMIN here and not a tenant admin, so the scope check that
     * every other {@code 'AiAgent'} gate is built on denies her — the visibility precondition runs first and the
     * resolver's bypass is TENANT admin. {@code hasResourceRole} consults ownership and workspace role only, so it
     * allows her, and the gate's {@code ||} is what turns that into a re-share. Delete the disjunct from the facade and
     * a withheld agent is re-shareable by nobody but its creator, while the same admin could re-share a withheld
     * project — {@code ProjectSharingFacadeImpl}'s gate never consults visibility.
     *
     * <p>
     * Both halves are asserted in one test deliberately: the denial alone would stay green with {@code hasResourceRole}
     * broken to always deny, and the allowance alone would stay green with the visibility precondition deleted from
     * {@code hasResourceScope} — which is the state this whole class exists to prevent.
     */
    @Test
    void testWorkspaceAdminIsRescuedByTheResourceRoleDisjunct() {
        authenticate("ana");

        PermissionServiceImpl permissionService = permissionService(Set.of(), WorkspaceRole.ADMIN);

        assertThat(permissionService.hasResourceScope(AGENT_ID, "AiAgent", "AGENT_EDIT"))
            .as("a workspace admin who is not the creator and holds no grant cannot SEE the withheld agent")
            .isFalse();

        assertThat(permissionService.hasResourceRole(AGENT_ID, "AiAgent", "ADMIN"))
            .as("but her workspace role still reaches it, which is what lets her re-share it")
            .isTrue();
    }

    /**
     * The rescue is a workspace ADMIN's, not every member's. Without this, a disjunct weakened to any workspace role
     * would keep the test above green.
     */
    @Test
    void testWorkspaceEditorIsNotRescued() {
        authenticate("ana");

        assertThat(permissionService(Set.of(), WorkspaceRole.EDITOR).hasResourceRole(AGENT_ID, "AiAgent", "ADMIN"))
            .as("an EDITOR does not reach the ADMIN rank the sharing gate's second disjunct asks for")
            .isFalse();
    }

    @Test
    void testUnknownAgentFailsClosed() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope(999L, "AiAgent", "AGENT_VIEW"))
            .as("a registered type whose row does not exist must deny, not fall through to allow")
            .isFalse();
    }

    @Test
    void testAgentResolvesUnderItsProjectType() {
        authenticate("ana");

        permissionService(Set.of(PROJECT_ID)).hasResourceScope(AGENT_ID, "AiAgent", "AGENT_VIEW");

        assertThat(LAST_RESOLVED_TYPE.get())
            .as("grants for an agent live under its backing project, so the resolver must be asked about Project")
            .isEqualTo("Project");
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, "password", List.of(authorities)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()));
    }

    /**
     * Builds the EE service over the production agent provider and resolver, with the workspace-scope check stubbed to
     * grant {@code AGENT_VIEW} only — so a denial of {@code AGENT_VIEW} can only come from visibility, and a denial of
     * {@code AGENT_EDIT} only from the scope.
     */
    private static PermissionServiceImpl permissionService(Set<Long> grantedProjectIds) {
        return permissionService(grantedProjectIds, null);
    }

    /**
     * As above, plus the workspace role {@code workspaceUserRepository} reports for the current user — {@code null}
     * meaning "not a member", which is what every test that does not name a role wants: those assert on
     * {@code hasResourceScope}, whose workspace-scope half is stubbed on {@code WorkspaceScopeCacheService} instead.
     */
    private static PermissionServiceImpl permissionService(Set<Long> grantedProjectIds, WorkspaceRole workspaceRole) {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong())).thenReturn(Set.of("AGENT_VIEW"));

        AiAgentRepository aiAgentRepository = mock(AiAgentRepository.class);

        when(aiAgentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent(AGENT_ID, PROJECT_ID)));
        when(aiAgentRepository.findById(WORKSPACE_AGENT_ID))
            .thenReturn(Optional.of(agent(WORKSPACE_AGENT_ID, WORKSPACE_PROJECT_ID)));

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.fetchProject(PROJECT_ID))
            .thenReturn(Optional.of(project(PROJECT_ID, ResourceVisibility.PRIVATE)));
        when(projectService.fetchProject(WORKSPACE_PROJECT_ID))
            .thenReturn(Optional.of(project(WORKSPACE_PROJECT_ID, ResourceVisibility.WORKSPACE)));

        WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(7L, WORKSPACE_ID))
            .thenReturn(
                workspaceRole == null ? Optional.empty()
                    : Optional.of(new WorkspaceUser(7L, WORKSPACE_ID, workspaceRole.ordinal())));

        return new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), mock(ProjectRepository.class),
            workspaceScopeCacheService, workspaceUserRepository,
            List.of(new AiAgentOwnershipResolver(aiAgentRepository)),
            List.of(new AiAgentVisibilityProvider(aiAgentRepository, projectService)),
            capturingResolver(grantedProjectIds), List.of(), mock(ObjectProvider.class));
    }

    private static AiAgent agent(long id, long projectId) {
        AiAgent agent = new AiAgent();

        agent.setId(id);
        agent.setProjectId(projectId);
        agent.setWorkspaceId(WORKSPACE_ID);

        return agent;
    }

    private static Project project(long id, ResourceVisibility visibility) {
        Project project = new Project();

        project.setId(id);
        project.setVisibility(visibility);

        // created_by is @CreatedBy-managed; the test seeds it the way the persistence layer would
        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        return project;
    }

    /**
     * The real resolution order minus the grant table — admin, reach, ownership, then the supplied grant set — while
     * recording the resource type it was asked about, which is what {@link #testAgentResolvesUnderItsProjectType}
     * reads.
     *
     * <p>
     * The grant set is consulted only when the resolver is asked about {@code "Project"}, mirroring
     * {@code ResourceGrantService.filterGrantedResourceIds}, which is keyed on the type as well as the id. Without that
     * condition the grant tests would pass on the id alone and stay green with the provider's parent-type declaration
     * removed — the assertion would be reading a value it did not reach through the thing it pins.
     */
    private static ResourceVisibilityResolver capturingResolver(Set<Long> grantedIds) {
        return (resourceType, workspaceId, candidates) -> {
            LAST_RESOLVED_TYPE.set(resourceType);

            Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

            if (authentication == null) {
                return Set.of();
            }

            boolean admin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> AuthorityConstants.ADMIN.equals(authority.getAuthority()));
            String login = String.valueOf(authentication.getPrincipal());

            return candidates.stream()
                .filter(
                    candidate -> admin ||
                        candidate.visibility()
                            .isAtLeast(ResourceVisibility.WORKSPACE)
                        || login.equals(candidate.createdBy())
                        || ("Project".equals(resourceType) && grantedIds.contains(candidate.id())))
                .map(VisibilityRecord::id)
                .collect(Collectors.toSet());
        };
    }
}
