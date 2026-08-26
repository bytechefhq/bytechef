/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceMembershipDecider;
import com.bytechef.automation.configuration.security.ResourceMembershipDecider.Outcome;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service("permissionService")
@ConditionalOnEEVersion
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final CurrentUserResolver currentUserResolver;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final ProjectRepository projectRepository;
    private final WorkspaceScopeCacheService workspaceScopeCacheService;
    private final WorkspaceUserRepository workspaceUserRepository;
    private final Map<String, ResourceEnvironmentResolver> resourceEnvironmentResolvers;
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;
    private final Map<String, ResourceVisibilityProvider> resourceVisibilityProviders;
    private final ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public PermissionServiceImpl(
        CurrentUserResolver currentUserResolver, PermissionScopeRegistry permissionScopeRegistry,
        ProjectRepository projectRepository, WorkspaceScopeCacheService workspaceScopeCacheService,
        WorkspaceUserRepository workspaceUserRepository,
        List<ResourceOwnershipResolver> resourceOwnershipResolvers,
        List<ResourceVisibilityProvider> resourceVisibilityProviders,
        ResourceVisibilityResolver resourceVisibilityResolver,
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers,
        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider) {

        this.currentUserResolver = currentUserResolver;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.projectRepository = projectRepository;
        this.workspaceScopeCacheService = workspaceScopeCacheService;
        this.workspaceUserRepository = workspaceUserRepository;
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));
        this.resourceVisibilityProviders = resourceVisibilityProviders.stream()
            .collect(Collectors.toMap(ResourceVisibilityProvider::resourceType, Function.identity()));
        this.resourceVisibilityResolver = resourceVisibilityResolver;
        this.resourceMembershipResolverProvider = resourceMembershipResolverProvider;

        this.resourceEnvironmentResolvers = resourceEnvironmentResolvers.stream()
            .collect(Collectors.toMap(ResourceEnvironmentResolver::resourceType, Function.identity()));
    }

    @Override
    public boolean isTenantAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }

    @Override
    public boolean isCurrentUser(long userId) {
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        OptionalLong currentUserId = currentUserResolver.fetchCurrentUserId();

        return currentUserId.isPresent() && currentUserId.getAsLong() == userId;
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        WorkspaceRole minimum = parseWorkspaceRole(minimumRole);

        if (minimum == null) {
            return false;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        return workspaceUserRepository.findByUserIdAndWorkspaceId(userId.getAsLong(), workspaceId)
            .map(member -> toWorkspaceRole(member.getWorkspaceRole()))
            .map(role -> role.hasAtLeast(minimum))
            .orElse(false);
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Set<String> scopeNames = workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId);

        return scopeNames.contains(scope);
    }

    /**
     * Mirrors the environment-unaware overload, including the skip-checks and tenant-admin short circuits, and differs
     * only in resolving the member's role for {@code environment}. A tenant admin is deliberately not subject to
     * per-environment roles.
     */
    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment) {
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Set<String> scopeNames =
            workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId, environment);

        return scopeNames.contains(scope);
    }

    /**
     * Requires the scope in every environment, so that an operation whose effect is not confined to one environment
     * cannot be authorised by a role held in only one of them.
     */
    @Override
    public boolean hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope) {
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        for (Environment environment : Environment.values()) {
            if (!hasWorkspaceScope(workspaceId, scope, environment)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        // A project-keyed check is a resource-scope check on the project: routing through hasResourceScope gives it
        // the same visibility precondition every hasPermission(#id, 'Project', ...) gate has.
        return hasResourceScope(projectId, "Project", scope);
    }

    /**
     * Carries the same visibility precondition as the environment-unaware overload, but inline rather than by
     * delegating to {@link #hasResourceScope(Serializable, String, String)} — that path would discard the explicit
     * {@code environment}, which is the whole reason this overload exists (the promotion caller passes the environment
     * being deployed INTO, not the one it is acting from).
     */
    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment) {
        // Ticket 1051: like the hasWorkflowScope(..., Environment) overload, this one inlines its check rather than
        // delegating to hasResourceScope, so the decider wired there does not reach it. Its only caller today is
        // AutomationPermissionEvaluator's ProjectDeploymentDTO branch, which consults the decider first -- but nothing
        // enforces that it stays the only caller, and a skip granting a Project-keyed check to a connected user is
        // exactly what this stage exists to close. Idempotent for that caller, since it will already have returned.
        Outcome outcome = ResourceMembershipDecider.decide(
            resourceMembershipResolverProvider, projectId, "Project", scope);

        if (outcome != Outcome.NOT_GOVERNED) {
            return outcome == Outcome.GRANT;
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (!isResourceVisible(projectId, "Project")) {
            return false;
        }

        Long workspaceId = projectRepository.findById(projectId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope, environment);
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        // Ticket 1051: a principal ResourceMembershipResolver governs is answered from its own membership, ahead of
        // the skip check, the tenant-admin check and the RBAC path below -- @SkipAutomationAuthorization grants such a
        // principal nothing. This delegation point covers every caller that reaches hasResourceScope, including
        // hasWorkflowScope(...) and the two-argument hasWorkspaceScopeForProject(...) overload. See
        // ResourceMembershipDecider for the precedence rule; rules 1 and 2 there keep Community Edition and every
        // non-connected-user principal on exactly the path below.
        //
        // NOT a coverage guarantee for this class: hasWorkflowScope(String, String, Environment) and
        // hasWorkspaceScopeForProject(long, String, Environment) deliberately do not route through here (delegating
        // would discard the explicit environment), so each consults the decider on its own account. A new
        // Environment-taking overload must do the same.
        Outcome outcome = ResourceMembershipDecider.decide(
            resourceMembershipResolverProvider, id, resourceType, scope);

        if (outcome != Outcome.NOT_GOVERNED) {
            return outcome == Outcome.GRANT;
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        // Visibility is a precondition of the scope check, not a filter running beside it. Holding
        // CONNECTION_EDIT in a workspace does not entitle a member to a colleague's PRIVATE connection, even
        // though the workspace-scope check below would otherwise pass for every row in that workspace.
        if (!isResourceVisible(id, resourceType)) {
            return false;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong workspaceId = resourceOwnershipResolver.resolveOwner(id)
            .workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        // A resource that lives in an environment is checked against the role the caller holds THERE. Without this,
        // a member who is viewer in Production would still pass a by-id check on a Production deployment, because the
        // environment-unaware check unions the environments they can reach. A type with no resolver, or a resolver
        // that cannot answer, keeps the environment-unaware check it had before.
        ResourceEnvironmentResolver resourceEnvironmentResolver = resourceEnvironmentResolvers.get(resourceType);

        if (resourceEnvironmentResolver != null) {
            Optional<Environment> environment = resourceEnvironmentResolver.fetchEnvironment(id);

            if (environment.isPresent()) {
                return hasWorkspaceScope(workspaceId.getAsLong(), scope, environment.get());
            }
        }

        return hasWorkspaceScope(workspaceId.getAsLong(), scope);
    }

    /**
     * Whether the current principal may see the resource at all, delegating to the same resolver the list path uses so
     * the two cannot drift. A resource type with no registered provider has not opted into visibility and is
     * unrestricted by it; a registered type whose resource does not exist fails closed.
     */
    private boolean isResourceVisible(Serializable id, String resourceType) {
        ResourceVisibilityProvider resourceVisibilityProvider = resourceVisibilityProviders.get(resourceType);

        if (resourceVisibilityProvider == null) {
            return true;
        }

        return resourceVisibilityProvider.fetchVisibility(id)
            .map(visibilityRecord -> {
                // workspaceId is unused by both resolver implementations — they resolve against the current
                // principal, not the argument — so 0 is safe. The parameter exists for a future SQL-predicate
                // implementation. The resource type handed to the resolver is the one the record and its grants
                // are stored under, which for an inheriting resource is its parent's.
                Set<Long> visibleIds = resourceVisibilityResolver.filterVisibleIds(
                    resourceVisibilityProvider.visibilityResourceType(), 0L, List.of(visibilityRecord));

                return !visibleIds.isEmpty();
            })
            .orElse(false);
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        // Ownership gates sharing/ownership management (connection credential replacement, connection/project
        // access grants, signing-key ownership). An embedded connected user has none of it and never reaches this
        // short circuit: SkipAutomationAuthorizationAspect arms nothing for a principal the membership seam governs.
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong ownerUserId = resourceOwnershipResolver.resolveOwner(id)
            .ownerUserId();

        return ownerUserId.isPresent() && isCurrentUser(ownerUserId.getAsLong());
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        // Resolves to hasWorkspaceRole(...) below. It is the other half of the isResourceOwner(...) ||
        // hasResourceRole(..., 'ADMIN') sharing gates used across the connection/project facades, so it must be
        // exactly as hard to reach as isResourceOwner above.
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong workspaceId = resourceOwnershipResolver.resolveOwner(id)
            .workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        return hasWorkspaceRole(workspaceId.getAsLong(), minimumRole);
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        // A workflow-keyed check is a resource-scope check on the workflow: routing through hasResourceScope gives it
        // the visibility precondition, and WorkflowVisibilityProvider redirects the lookup to the owning project.
        return hasResourceScope(workflowId, "Workflow", scope);
    }

    /**
     * Mirrors the environment-unaware overload — same check kind, same short circuits, same visibility precondition —
     * and differs only in checking the caller's role for {@code environment} instead of unioning every environment they
     * can reach. A tenant admin is deliberately not subject to per-environment roles.
     */
    @Override
    public boolean hasWorkflowScope(String workflowId, String scope, Environment environment) {
        // Ticket 1051: this overload deliberately does NOT delegate to hasResourceScope (that would discard the
        // explicit environment), so the decider wired there does not cover it. Consulted here too, so the direct
        // @permissionService.hasWorkflowScope(...) bean path is governed even when
        // AutomationMethodSecurityExpressionRoot -- which consults the decider on its own account -- is not the caller.
        // Idempotent when it is: the root will already have returned for a governed principal.
        Outcome outcome = ResourceMembershipDecider.decide(
            resourceMembershipResolverProvider, workflowId, "Workflow", scope);

        if (outcome != Outcome.NOT_GOVERNED) {
            return outcome == Outcome.GRANT;
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (!isResourceVisible(workflowId, "Workflow")) {
            return false;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get("Workflow");

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong workspaceId = resourceOwnershipResolver.resolveOwner(workflowId)
            .workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        return hasWorkspaceScope(workspaceId.getAsLong(), scope, environment);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public String getMyWorkspaceRole(long workspaceId) {
        if (isTenantAdmin()) {
            return WorkspaceRole.ADMIN.name();
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return null;
        }

        return workspaceUserRepository.findByUserIdAndWorkspaceId(userId.getAsLong(), workspaceId)
            .map(member -> toWorkspaceRole(member.getWorkspaceRole()))
            .map(WorkspaceRole::name)
            .orElse(null);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        if (isTenantAdmin()) {
            return Set.copyOf(permissionScopeRegistry.getAllScopeNames());
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return Collections.emptySet();
        }

        return Set.copyOf(workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId));
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        workspaceScopeCacheService.evictWorkspaceScopeCache(userId, workspaceId);
    }

    @Override
    public void evictWorkspaceScopeCaches(Collection<UserWorkspacePair> userWorkspacePairs) {
        workspaceScopeCacheService.evictWorkspaceScopeCaches(userWorkspacePairs);
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
        workspaceScopeCacheService.evictAllWorkspaceScopeCache();
    }

    private static boolean isAutomationAuthorizationSkipped() {
        return AutomationAuthorizationContext.isSkipChecks();
    }

    private static WorkspaceRole parseWorkspaceRole(String roleName) {
        try {
            return WorkspaceRole.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            log.error("Unknown WorkspaceRole '{}' in @PreAuthorize — failing closed.", roleName);

            return null;
        }
    }

    private static WorkspaceRole toWorkspaceRole(Integer ordinal) {
        if (ordinal == null) {
            return null;
        }

        WorkspaceRole[] values = WorkspaceRole.values();

        if (ordinal < 0 || ordinal >= values.length) {
            log.error(
                "Invalid workspace_role ordinal={} — outside the range [0, {}). Failing closed. " +
                    "This indicates a corrupted or legacy workspace_role value.",
                ordinal, values.length);

            return null;
        }

        return values[ordinal];
    }
}
