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

import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Central RBAC service backing the {@code @PreAuthorize} SpEL checks used across the automation tier (both the custom
 * root built-ins {@code isCurrentUser}/{@code isTenantAdmin}/{@code isResourceOwner} and the {@code hasPermission(...)}
 * scope/role tokens). The EE implementation enforces real workspace-role, scope and ownership checks; the CE
 * implementation is a permissive pass-through (except {@link #isTenantAdmin()}). EE checks short-circuit to
 * {@code true} when {@code AutomationAuthorizationContext.isSkipChecks()} is set or the user is a tenant admin, and
 * otherwise fail closed.
 *
 * @author Ivica Cardic
 */
public interface PermissionService {

    /**
     * Evicts the cached scope grants for a single user/workspace pair, forcing the next scope check to recompute.
     *
     * @param userId      the user whose cached scopes are invalidated
     * @param workspaceId the workspace whose cached scopes are invalidated
     */
    void evictWorkspaceScopeCache(long userId, long workspaceId);

    /**
     * Evicts the cached scope grants for several user/workspace pairs.
     *
     * @param userWorkspacePairs the user/workspace pairs whose cached scopes are invalidated
     */
    default void evictWorkspaceScopeCaches(Collection<UserWorkspacePair> userWorkspacePairs) {
        for (UserWorkspacePair pair : userWorkspacePairs) {
            evictWorkspaceScopeCache(pair.userId(), pair.workspaceId());
        }
    }

    /**
     * Evicts the entire workspace scope cache for all users and workspaces.
     */
    void evictAllWorkspaceScopeCache();

    /**
     * Returns whether the current user holds at least {@code minimumRole} (by rank) in the workspace.
     *
     * <p>
     * Only a workspace-wide role answers this. A member in explicit mode — one holding a per-environment role rather
     * than a role that applies everywhere — is denied even when the role they hold in every environment would satisfy
     * {@code minimumRole}, because there is no one role of theirs to compare and picking an environment's would answer
     * a question nobody asked. Deliberately fail-closed, and a real behaviour difference at every gate that uses it:
     * prefer {@code hasWorkspaceScope(workspaceId, scope, environment)} for anything whose effect lands in one
     * environment, and {@code hasWorkspaceScopeInEveryEnvironment} for anything that does not.
     *
     * @param workspaceId the workspace to check membership in
     * @param minimumRole the minimum {@link WorkspaceRoleType} name required
     * @return {@code true} if the current user's workspace-wide role is at least {@code minimumRole}
     */
    boolean hasWorkspaceRole(long workspaceId, String minimumRole);

    /**
     * Returns whether the current user has been granted {@code scope} in the workspace.
     *
     * @param workspaceId the workspace whose scope grants are inspected
     * @param scope       the scope name the user must hold
     * @return {@code true} if the current user holds {@code scope} in the workspace
     */
    boolean hasWorkspaceScope(long workspaceId, String scope);

    /**
     * Returns whether the current user has been granted {@code scope} in the workspace, <em>in the environment being
     * acted on</em>.
     *
     * <p>
     * The environment is always passed explicitly and is never read from {@code EnvironmentContext}. That thread-local
     * holds the <em>source</em> environment during a promotion, and is already known to be lost on worker threads and
     * in agent tool calls, so an implicit read would fail open in precisely the case this overload exists for.
     *
     * @param workspaceId the workspace whose scope grants are inspected
     * @param scope       the scope name the user must hold
     * @param environment the environment the caller intends to act on
     * @return {@code true} if the current user holds {@code scope} in the workspace for that environment
     */
    boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment);

    /**
     * Returns whether the current user has been granted {@code scope} in <em>every</em> environment of the workspace.
     *
     * <p>
     * This is the check for an operation whose effect is not confined to one environment — granting a workspace-wide
     * role, for instance, takes effect everywhere at once. The environment-unaware
     * {@link #hasWorkspaceScope(long, String)} is a union across the environments a member can reach, so using it here
     * would let a member who administers only Development grant themselves Production.
     *
     * @param workspaceId the workspace whose scope grants are inspected
     * @param scope       the scope name the user must hold in every environment
     * @return {@code true} if the current user holds {@code scope} in every environment
     */
    boolean hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope);

    /**
     * Returns whether the current user has {@code scope} in the workspace that owns the project.
     *
     * @param projectId the project whose owning workspace is checked
     * @param scope     the scope name the user must hold in that workspace
     * @return {@code true} if the current user holds {@code scope} in the project's workspace
     */
    boolean hasWorkspaceScopeForProject(long projectId, String scope);

    /**
     * Returns whether the current user has {@code scope} in the workspace that owns the project, in the environment
     * being acted on. See {@link #hasWorkspaceScope(long, String, Environment)} for why the environment is a parameter.
     *
     * @param projectId   the project whose owning workspace is checked
     * @param scope       the scope name the user must hold in that workspace
     * @param environment the environment the caller intends to act on
     * @return {@code true} if the current user holds {@code scope} in the project's workspace for that environment
     */
    boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment);

    /**
     * Returns whether the current user has {@code scope} for the resource, resolved to its owning workspace via the
     * {@code ResourceOwnershipResolver} registered for {@code resourceType}.
     *
     * @param id           the resource identifier
     * @param resourceType the resource type key used to select the ownership resolver
     * @param scope        the scope name the user must hold
     * @return {@code true} if the current user holds {@code scope} for the resource
     */
    boolean hasResourceScope(Serializable id, String resourceType, String scope);

    /**
     * Returns whether the current user has {@code scope} for the resource in the environment being acted on, resolved
     * to its owning workspace the same way {@link #hasResourceScope(Serializable, String, String)} resolves it.
     *
     * <p>
     * Differs from that method in where the environment comes from: it is the caller's argument rather than whatever a
     * {@code ResourceEnvironmentResolver} can say about the resource. Use it when the resource spans environments and
     * the operation picks one — a project, for instance, has no environment of its own, so the environment-unaware
     * check unions every environment the caller can reach and a member who is viewer in Production would pass a write
     * aimed at Production.
     *
     * @param id           the resource identifier
     * @param resourceType the resource type key used to select the ownership resolver
     * @param scope        the scope name the user must hold
     * @param environment  the environment the caller intends to act on
     * @return {@code true} if the current user holds {@code scope} for the resource in that environment
     */
    boolean hasResourceScopeInEnvironment(Serializable id, String resourceType, String scope, Environment environment);

    /**
     * Returns whether the current user holds at least {@code minimumRole} in the workspace that owns the resource.
     *
     * <p>
     * Resolves the owning workspace and then defers to {@link #hasWorkspaceRole(long, String)}, so it inherits that
     * method's workspace-wide reading: a member in explicit mode is denied here too, however much they hold per
     * environment. Every {@code @PreAuthorize} using this therefore denies such a member — see the four connection
     * sharing mutations in {@code WorkspaceConnectionFacadeImpl}, where owning the connection is the remaining way in.
     *
     * @param id           the resource identifier
     * @param resourceType the resource type key used to select the ownership resolver
     * @param minimumRole  the minimum {@link WorkspaceRoleType} name required
     * @return {@code true} if the current user's workspace-wide role in the resource's workspace is at least
     *         {@code minimumRole}
     */
    boolean hasResourceRole(long id, String resourceType, String minimumRole);

    /**
     * Returns whether the current user has {@code scope} in the workspace that owns the workflow.
     *
     * @param workflowId the workflow whose owning workspace is checked
     * @param scope      the scope name the user must hold in that workspace
     * @return {@code true} if the current user holds {@code scope} in the workflow's workspace
     */
    boolean hasWorkflowScope(String workflowId, String scope);

    /**
     * Returns whether the current user has {@code scope} in the workspace that owns the workflow, in the environment
     * being acted on. See {@link #hasWorkspaceScope(long, String, Environment)} for why the environment is a parameter.
     *
     * @param workflowId  the workflow whose owning workspace is checked
     * @param scope       the scope name the user must hold in that workspace
     * @param environment the environment the caller intends to act on
     * @return {@code true} if the current user holds {@code scope} in the workflow's workspace for that environment
     */
    boolean hasWorkflowScope(String workflowId, String scope, Environment environment);

    /**
     * Returns whether the current user has {@code scope} in the workspace that owns the workflow, in the environment
     * being acted on, or the workflow belongs to no automation project. For the platform workflow-editor endpoints
     * shared with embedded, whose integration workflows have no project and are authorized by the embedded surface
     * rather than by workspace membership.
     *
     * @param workflowId  the workflow whose owning workspace is checked
     * @param scope       the scope name the user must hold in that workspace
     * @param environment the environment the caller intends to act on
     * @return {@code true} if the workflow belongs to no project, or the current user holds {@code scope} in the
     *         workspace of the project it belongs to for that environment
     */
    boolean hasWorkflowScopeIfProjectWorkflow(String workflowId, String scope, Environment environment);

    /**
     * Returns the scope names the current user holds in the workspace (all registered scopes for a tenant admin).
     *
     * @param workspaceId the workspace whose scope grants are returned
     * @return the current user's scopes, or an empty set if none / no current user
     */
    Set<String> getMyWorkspaceScopes(long workspaceId);

    /**
     * Returns the scope names the current user holds in the workspace in one environment (all registered scopes for a
     * tenant admin). A member whose only roles are in other environments holds none here.
     *
     * @param workspaceId the workspace whose scope grants are returned
     * @param environment the environment the scopes must apply to
     * @return the current user's scopes in {@code environment}, or an empty set if none / no current user
     */
    Set<String> getMyWorkspaceScopes(long workspaceId, Environment environment);

    /**
     * Returns the current user's {@link WorkspaceRoleType} name in the workspace, or {@code null} if not a member.
     *
     * @param workspaceId the workspace to look up membership in
     * @return the {@link WorkspaceRoleType} name, or {@code null} when there is no membership
     */
    String getMyWorkspaceRole(long workspaceId);

    /**
     * Returns whether {@code userId} identifies the currently authenticated user.
     *
     * @param userId the user id to compare against the current user
     * @return {@code true} if {@code userId} is the current user
     */
    boolean isCurrentUser(long userId);

    /**
     * Returns whether the current user is the owner (creator) of the resource of the given {@code resourceType}.
     *
     * @param resourceType the resource type key used to select the ownership resolver
     * @param id           the resource identifier
     * @return {@code true} if the current user owns the resource
     */
    boolean isResourceOwner(String resourceType, long id);

    /**
     * Returns whether the current user holds the tenant-admin authority. Enforced in both editions.
     *
     * @return {@code true} if the current user is a tenant admin
     */
    boolean isTenantAdmin();

    /**
     * A user/workspace pair identifying a single entry in the workspace scope cache.
     */
    record UserWorkspacePair(long userId, long workspaceId) {
    }
}
