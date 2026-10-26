/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.config;

import com.bytechef.automation.configuration.service.PermissionService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("permissionService")
public class RecordingPermissionService implements PermissionService {

    public record ResourceScopeCheck(Serializable id, String resourceType, String scope) {
    }

    private final List<ResourceScopeCheck> resourceScopeChecks = new ArrayList<>();

    private boolean granted = true;

    public List<ResourceScopeCheck> getResourceScopeChecks() {
        return List.copyOf(resourceScopeChecks);
    }

    public void reset() {
        resourceScopeChecks.clear();

        granted = true;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        return Set.of();
    }

    @Override
    public @Nullable String getMyWorkspaceRole(long workspaceId) {
        return null;
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        return granted;
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        resourceScopeChecks.add(new ResourceScopeCheck(id, resourceType, scope));

        return granted;
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        return granted;
    }

    @Override
    public boolean isCurrentUser(long userId) {
        return granted;
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        return granted;
    }

    @Override
    public boolean isTenantAdmin() {
        return granted;
    }
}
