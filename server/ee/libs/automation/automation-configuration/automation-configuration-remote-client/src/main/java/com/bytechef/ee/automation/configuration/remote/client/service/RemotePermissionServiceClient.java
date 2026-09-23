/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemotePermissionServiceClient implements PermissionService {

    @Override
    public boolean isTenantAdmin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCurrentUser(long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasResourceScopeInEnvironment(
        Serializable id, String resourceType, String scope, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkflowScopeIfProjectWorkflow(String workflowId, String scope, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable String getMyWorkspaceRole(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
        throw new UnsupportedOperationException();
    }
}
