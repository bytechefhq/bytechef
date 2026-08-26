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
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.configuration.domain.Environment;
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
 * authenticated (non-anonymous) caller and deny unauthenticated ones. The two
 * {@link #hasWorkspaceScopeForProject(long, String)} overloads and {@link #hasWorkflowScope(String, String)} are the
 * exception: they are keyed on a project or on a workflow that belongs to one, so they run the same resource-level
 * check as everything else rather than answering on authentication alone. Resource-level access
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
    private final Map<String, ResourceVisibilityProvider> resourceVisibilityProviders;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings("EI")
    public PermissionServiceImpl(
        UserService userService, List<ResourceOwnershipResolver> resourceOwnershipResolvers,
        List<ResourceVisibilityProvider> resourceVisibilityProviders,
        ResourceVisibilityResolver resourceVisibilityResolver) {

        this.userService = userService;
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));
        this.resourceVisibilityProviders = resourceVisibilityProviders.stream()
            .collect(Collectors.toMap(ResourceVisibilityProvider::resourceType, Function.identity()));
        this.resourceVisibilityResolver = resourceVisibilityResolver;
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

    /**
     * Community Edition has no authorization boundary between workspace members, so per-environment roles do not exist
     * here and this matches the environment-unaware overload exactly.
     */
    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment) {
        return SecurityUtils.isAuthenticated();
    }

    /**
     * See {@link #hasWorkspaceScope(long, String, Environment)} — per-environment roles are an Enterprise feature.
     */
    @Override
    public boolean hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        // A project-keyed check is a resource-scope check on the project: routing through hasResourceScope gives it
        // the same visibility precondition every hasPermission(#id, 'Project', ...) gate has.
        return hasResourceScope(projectId, "Project", scope);
    }

    /**
     * Drops the {@link Environment} and runs exactly what the environment-unaware overload runs, rather than adding the
     * visibility precondition inline as EE does. CE has no per-environment roles —
     * {@link #hasWorkspaceScope(long, String, Environment)} is itself environment-blind — so nothing is lost, and
     * routing both project-keyed overloads through the same {@code hasResourceScope} call is what keeps them from
     * disagreeing about the same project: an inline precondition would leave this one permitting a project the
     * two-argument sibling denies.
     */
    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment) {
        return hasResourceScope(projectId, "Project", scope);
    }

    /**
     * Deliberately NOT wired to {@code ResourceMembershipDecider}, unlike its EE twin. The asymmetry is intentional
     * rather than an oversight: the decider's first precedence rule is "no {@code ResourceMembershipResolver} bean
     * means no governed principal", the only implementation is {@code @ConditionalOnEEVersion}, and this implementation
     * only runs in Community Edition — so the decider could answer nothing but {@code NOT_GOVERNED} here. Should a
     * Community-Edition resolver ever exist, this method needs the same wiring the EE one has.
     */
    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        // Visibility is a precondition of every scope check, not a filter running beside it. Without this a member
        // would pass here for a resource the list correctly hides — the by-id half of the same authorization
        // question answering differently from the list half.
        if (!isResourceVisible(id, resourceType)) {
            return false;
        }

        ResourceOwnershipResolver resolver = resourceOwnershipResolvers.get(resourceType);

        if (resolver == null) {
            return false;
        }

        ResourceOwner resourceOwner = resolver.resolveOwner(id);

        if (resourceVisibilityProviders.containsKey(resourceType)) {
            // Visibility already decided this, and it is the stricter answer: CE creates every connection
            // WORKSPACE-visible, so also requiring the caller to be the owner would deny a colleague the list has
            // already shown them. All that remains is that the resource actually exists somewhere.
            return resourceOwner.workspaceId()
                .isPresent() ||
                resourceOwner.ownerUserId()
                    .isPresent();
        }

        // Resource families that have not opted into visibility keep CE owner-isolation unchanged. API keys are
        // user-owned and have no visibility concept; relaxing them here would hand every authenticated user a
        // colleague's key.
        OptionalLong ownerUserId = resourceOwner.ownerUserId();

        if (ownerUserId.isPresent()) {
            return isCurrentUser(ownerUserId.getAsLong());
        }

        return resourceOwner.workspaceId()
            .isPresent();
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
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        return SecurityUtils.isAuthenticated();
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        // A workflow-keyed check is a resource-scope check on the workflow: routing through hasResourceScope gives it
        // the visibility precondition, and WorkflowVisibilityProvider redirects the lookup to the owning project.
        return hasResourceScope(workflowId, "Workflow", scope);
    }

    /**
     * Drops the {@link Environment} and runs exactly what the environment-unaware overload runs. CE has no
     * per-environment roles — {@link #hasWorkspaceScope(long, String, Environment)} is itself environment-blind — so
     * nothing is lost, and routing both workflow-keyed overloads through the same {@code hasResourceScope} call is what
     * keeps them from disagreeing about the same workflow.
     */
    @Override
    public boolean hasWorkflowScope(String workflowId, String scope, Environment environment) {
        return hasResourceScope(workflowId, "Workflow", scope);
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
