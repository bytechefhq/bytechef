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
 * Community Edition no-op implementation of {@link PermissionService}. RBAC (workspace roles, project scopes, custom
 * roles) is an Enterprise Edition feature — in CE every authenticated user is treated as having full access at the
 * service level, and access control is enforced solely by Spring Security's {@code ROLE_USER}/{@code ROLE_ADMIN}
 * authorities plus the client-side UI gating.
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
