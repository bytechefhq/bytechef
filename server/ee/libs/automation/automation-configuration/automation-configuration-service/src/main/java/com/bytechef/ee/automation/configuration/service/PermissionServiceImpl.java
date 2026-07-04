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
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public PermissionServiceImpl(
        CurrentUserResolver currentUserResolver, PermissionScopeRegistry permissionScopeRegistry,
        ProjectRepository projectRepository, WorkspaceScopeCacheService workspaceScopeCacheService,
        WorkspaceUserRepository workspaceUserRepository,
        List<ResourceOwnershipResolver> resourceOwnershipResolvers) {

        this.currentUserResolver = currentUserResolver;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.projectRepository = projectRepository;
        this.workspaceScopeCacheService = workspaceScopeCacheService;
        this.workspaceUserRepository = workspaceUserRepository;
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));
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
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
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

        return hasWorkspaceScope(workspaceId.getAsLong(), scope);
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

        return values[ordinal];
    }
}
