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
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers) {

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
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        Long workspaceId = projectRepository.findById(projectId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope);
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment) {
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
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

        if (!(id instanceof Number number)) {
            return false;
        }

        return resourceVisibilityProvider.fetchVisibility(number.longValue())
            .map(visibilityRecord -> {
                // workspaceId is unused by both resolver implementations — they resolve against the current
                // principal, not the argument — so 0 is safe. The parameter exists for a future SQL-predicate
                // implementation.
                Set<Long> visibleIds = resourceVisibilityResolver.filterVisibleIds(
                    resourceType, 0L, List.of(visibilityRecord));

                return !visibleIds.isEmpty();
            })
            .orElse(false);
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
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
        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        Long workspaceId = projectRepository.findByWorkflowId(workflowId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope);
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
