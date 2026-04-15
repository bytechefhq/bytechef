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

import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Community Edition no-op implementation of {@link PermissionService}. RBAC (workspace roles, project scopes, custom
 * roles) is an Enterprise Edition feature — in CE every authenticated user is treated as having full access at the
 * service level, and access control is enforced solely by Spring Security's {@code ROLE_USER}/{@code ROLE_ADMIN}
 * authorities plus the client-side UI gating.
 *
 * <p>
 * As a result every {@code hasXxx} method returns {@code true} unconditionally so that {@code @PreAuthorize} SpEL
 * expressions like {@code @permissionService.hasProjectScope(#id, 'WORKFLOW_VIEW')} stay green on CE deployments. The
 * EE counterpart in {@code com.bytechef.ee.automation.configuration.service.PermissionServiceImpl} provides real
 * checks. Both beans are declared with {@code @ConditionalOnXEVersion} so exactly one is loaded per edition.
 *
 * <p>
 * The {@code isTenantAdmin()} check is the one method that DOES enforce a real authority — it gates tenant-admin-only
 * mutations (custom-role management, cross-workspace project listings) which must remain admin-only even on CE.
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.UnusedFormalParameter")
@Service("permissionService")
@ConditionalOnCEVersion
public class PermissionServiceImpl implements PermissionService {

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public PermissionServiceImpl(UserService userService) {
        this.userService = userService;
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
    public boolean hasProjectScope(long projectId, String scope) {
        return true;
    }

    @Override
    public boolean hasProjectRole(long projectId, String minimumRole) {
        return true;
    }

    @Override
    public Set<String> getMyProjectScopes(long projectId) {
        return Collections.emptySet();
    }

    @Override
    public String getMyWorkspaceRole(long workspaceId) {
        // CE is a single-tenant edition with permissive workspace access — every authenticated user is effectively
        // an ADMIN. Returning the EE-defined "ADMIN" role string keeps EE-only callers that compare role ordinals
        // (e.g. AiGatewayFacade.validateWorkspaceAccess, WorkspaceAuthorization) consistent with the "permissive
        // pass-through" promise in this class's Javadoc — a null response was being treated as deny, which silently
        // blocked non-admin CE users from any EE-branched feature.
        return "ADMIN";
    }

    @Override
    public void evictProjectScopeCache(long userId, long projectId) {
    }

    @Override
    public void evictAllProjectScopeCache() {
    }
}
