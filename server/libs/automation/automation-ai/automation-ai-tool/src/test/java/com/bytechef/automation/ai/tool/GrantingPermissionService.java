/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.configuration.service.PermissionService;
import java.io.Serializable;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
final class GrantingPermissionService implements PermissionService {

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        return true;
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        return true;
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        return true;
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        return true;
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        return true;
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        return true;
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        return Set.of();
    }

    @Override
    public String getMyWorkspaceRole(long workspaceId) {
        return null;
    }

    @Override
    public boolean isCurrentUser(long userId) {
        return true;
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        return true;
    }

    @Override
    public boolean isTenantAdmin() {
        return true;
    }
}
