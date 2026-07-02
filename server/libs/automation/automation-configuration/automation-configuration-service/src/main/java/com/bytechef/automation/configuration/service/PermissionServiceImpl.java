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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Community Edition implementation of {@link PermissionService}. Fine-grained RBAC (workspace roles, project scopes,
 * custom roles) is an Enterprise Edition feature, so in CE the workspace/role/scope checks grant access to any
 * authenticated (non-anonymous) caller and deny unauthenticated ones. Resource-level access
 * ({@link #hasResourceScope(Serializable, String, String)}) still fails closed: it denies unauthenticated callers and
 * unknown resource types, and enforces owner isolation for user-owned resources. Coarse-grained access control is
 * otherwise enforced by Spring Security's {@code ROLE_USER}/{@code ROLE_ADMIN} authorities plus the client-side UI
 * gating.
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.UnusedFormalParameter")
@Service("permissionService")
@ConditionalOnCEVersion
public class PermissionServiceImpl implements PermissionService {

    private final UserService userService;
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;

    @SuppressFBWarnings("EI")
    public PermissionServiceImpl(
        UserService userService, List<ResourceOwnershipResolver> resourceOwnershipResolvers) {

        this.userService = userService;
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));
    }

    @Override
    public boolean isTenantAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }

    @Override
    public boolean isCurrentUser(long userId) {
        return userService.fetchCurrentUser()
            .map(user -> user.getId() != null && user.getId() == userId)
            .orElse(false);
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resolver = resourceOwnershipResolvers.get(resourceType);

        if (resolver == null) {
            return false;
        }

        ResourceOwner resourceOwner = resolver.resolveOwner(id);

        OptionalLong ownerUserId = resourceOwner.ownerUserId();

        if (ownerUserId.isPresent()) {
            return isCurrentUser(ownerUserId.getAsLong());
        }

        return resourceOwner.workspaceId()
            .isPresent();
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        return Collections.emptySet();
    }

    @Override
    public String getMyWorkspaceRole(long workspaceId) {
        return "ADMIN";
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
    }
}
