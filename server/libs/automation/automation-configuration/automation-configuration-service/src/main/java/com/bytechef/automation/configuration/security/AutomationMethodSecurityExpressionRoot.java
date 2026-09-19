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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * Custom {@code @PreAuthorize} SpEL root that adds the ByteChef-specific built-ins on top of the standard Spring
 * Security expression operations ({@code hasPermission}, {@code hasRole}, {@code isAuthenticated}, …):
 *
 * <ul>
 * <li>{@code isCurrentUser(#id)} — grants when the supplied id is the current authenticated user's id.</li>
 * <li>{@code isTenantAdmin()} — grants when the current user is a global tenant administrator.</li>
 * <li>{@code isResourceOwner(#id, 'Type')} — grants when the current user owns the identified resource.</li>
 * <li>{@code hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'SCOPE')} — requires the scope everywhere, for an
 * operation whose effect is not confined to one environment.</li>
 * <li>{@code hasWorkspaceScopeInEnvironment(#workspaceId, 'SCOPE', #environment)} — requires it in the one environment
 * the operation acts on.</li>
 * <li>{@code hasWorkspaceScopeInEnvironmentId(#workspaceId, 'SCOPE', #environmentId)} — the raw-ordinal counterpart of
 * {@code hasWorkspaceScopeInEnvironment}, for listings that carry an environment id.</li>
 * <li>{@code hasResourceScopeInEnvironment(#id, 'Type', 'SCOPE', #environment)} — for a resource that spans
 * environments and an operation that acts on one of them.</li>
 * <li>{@code hasWorkflowScopeInEnvironment(#workflowId, 'SCOPE', #environment)} — requires it in the workspace owning
 * the workflow's project, in the environment the operation acts on.</li>
 * <li>{@code hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, 'SCOPE', #environment)} and
 * {@code hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'SCOPE', #environmentId)} — the same for the
 * workflow-editor endpoints embedded shares.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public final class AutomationMethodSecurityExpressionRoot
    extends SecurityExpressionRoot<MethodInvocation> implements MethodSecurityExpressionOperations {

    private final PermissionService permissionService;
    private final Object target;

    private Object filterObject;
    private Object returnObject;

    AutomationMethodSecurityExpressionRoot(
        Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation,
        PermissionService permissionService) {

        super(authentication, methodInvocation);

        this.permissionService = permissionService;
        this.target = methodInvocation.getThis();
    }

    /**
     * Returns {@code true} if {@code userId} matches the current authenticated user. Bypassed (returns {@code true})
     * under embedded skip-checks mode.
     */
    public boolean isCurrentUser(long userId) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.isCurrentUser(userId);
    }

    /**
     * Returns {@code true} if the current user is a global tenant administrator. Bypassed (returns {@code true}) under
     * embedded skip-checks mode.
     */
    public boolean isTenantAdmin() {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.isTenantAdmin();
    }

    /**
     * Returns {@code true} if the current user owns the resource of {@code resourceType} identified by {@code id},
     * resolved via the registered {@code ResourceOwnershipResolver}. Bypassed (returns {@code true}) under embedded
     * skip-checks mode.
     */
    public boolean isResourceOwner(long id, String resourceType) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.isResourceOwner(resourceType, id);
    }

    /**
     * Requires {@code scope} in every environment of the workspace. Use it on an operation whose effect is not confined
     * to one environment — a workspace-wide role grant takes effect everywhere at once, so authorising it from a role
     * held in a single environment would be an escalation.
     */
    public boolean hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.hasWorkspaceScopeInEveryEnvironment(workspaceId, scope);
    }

    /**
     * Requires {@code scope} in the environment the operation acts on. The environment is taken from the guarded
     * method's own arguments, never from {@code EnvironmentContext}, which holds the source environment during a
     * promotion and is lost on worker threads.
     */
    public boolean hasWorkspaceScopeInEnvironment(long workspaceId, String scope, Environment environment) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.hasWorkspaceScope(workspaceId, scope, environment);
    }

    /**
     * Requires {@code scope} in the environment the caller named, for callers that supply it as a raw ordinal rather
     * than a resolved {@link Environment} — the shape the REST and GraphQL listings use, and the one
     * {@code hasPermission(#workspaceId, 'Workspace', ...)} cannot express, because a workspace has no environment of
     * its own and the environment-unaware check therefore unions every environment the caller can reach.
     * <p>
     * <b>Named differently from {@link #hasWorkspaceScopeInEnvironment(long, String, Environment)} on purpose.</b> A
     * same-name, same-arity sibling taking {@code Long} is ambiguous in Java for a {@code null} literal, and in SpEL a
     * null argument matches both by reflection order — selecting the {@code Environment} overload and failing with an
     * NPE on {@code environment.ordinal()}. Do not merge the two.
     * <p>
     * <b>A {@code null} ordinal keeps the environment-unaware check</b> rather than denying or requiring every
     * environment. The listings that pass one use {@code null} as "no environment filter" and the clients routinely
     * send nothing, so denying would refuse ordinary pages to exactly the members per-environment roles protect. This
     * gate closes forgery — naming an environment the caller holds no role in — and a {@code null} names nothing. The
     * unfiltered listing still returns rows from every environment; that union is pre-existing and not closed here.
     */
    public boolean hasWorkspaceScopeInEnvironmentId(long workspaceId, String scope, @Nullable Long environmentId) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        if (environmentId == null) {
            return permissionService.hasWorkspaceScope(workspaceId, scope);
        }

        Environment[] environments = Environment.values();

        // An ordinal outside the enum is a forged argument, not an absent one: deny rather than fall back to the
        // environment-unaware check, which would make a bad ordinal the way around this gate.
        if (environmentId < 0 || environmentId >= environments.length) {
            return false;
        }

        return permissionService.hasWorkspaceScope(workspaceId, scope, environments[(int) (long) environmentId]);
    }

    /**
     * Requires {@code scope} for the resource in the environment the operation acts on, for a resource that spans
     * environments and an operation that picks one — the case {@code hasPermission(#id, 'Type', 'SCOPE')} cannot
     * express, since it can only take the environment from a {@code ResourceEnvironmentResolver} and a resource
     * spanning environments has none to give.
     * <p>
     * A {@code null} environment keeps the environment-unaware check, for the same reason the {@code null} ordinal does
     * above, and guarding it here rather than at the call site is what makes the expression safe to point at a
     * caller-supplied {@code Environment}: without it the path below reaches {@code environment.ordinal()} deeper in
     * and a null turns a 403 into a 500.
     */
    public boolean hasResourceScopeInEnvironment(
        Serializable id, String resourceType, String scope, @Nullable Environment environment) {

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        if (environment == null) {
            return permissionService.hasResourceScope(id, resourceType, scope);
        }

        return permissionService.hasResourceScopeInEnvironment(id, resourceType, scope, environment);
    }

    /**
     * Requires {@code scope} in the workspace that owns the workflow's project, in the environment the operation acts
     * on. A workflow id is a UUID string, which the {@code long}-keyed {@code hasPermission(#id, 'Type', 'SCOPE')}
     * resolvers cannot take, and a workflow that belongs to no project resolves to no workspace and is denied. Use it
     * where only automation traffic arrives.
     */
    public boolean hasWorkflowScopeInEnvironment(String workflowId, String scope, Environment environment) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.hasWorkflowScope(workflowId, scope, environment);
    }

    /**
     * The counterpart to {@link #hasWorkflowScopeInEnvironment(String, String, Environment)} for the platform
     * workflow-editor endpoints that embedded shares: requires {@code scope} for a workflow that belongs to an
     * automation project and leaves a workflow that belongs to none, an embedded integration workflow, to the embedded
     * surface's own authorization.
     */
    public boolean hasWorkflowScopeIfProjectWorkflowInEnvironment(
        String workflowId, String scope, Environment environment) {

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.hasWorkflowScopeIfProjectWorkflow(workflowId, scope, environment);
    }

    /**
     * The raw-ordinal counterpart of {@link #hasWorkflowScopeIfProjectWorkflowInEnvironment}, named differently for the
     * reason given on {@link #hasWorkspaceScopeInEnvironmentId}. A {@code null} ordinal is the Development environment
     * projects are edited in, and an ordinal outside the enum is denied.
     */
    public boolean hasWorkflowScopeIfProjectWorkflowInEnvironmentId(
        String workflowId, String scope, @Nullable Long environmentId) {

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        Environment[] environments = Environment.values();

        if (environmentId == null) {
            return permissionService.hasWorkflowScopeIfProjectWorkflow(workflowId, scope, Environment.DEVELOPMENT);
        }

        if (environmentId < 0 || environmentId >= environments.length) {
            return false;
        }

        return permissionService.hasWorkflowScopeIfProjectWorkflow(
            workflowId, scope, environments[(int) (long) environmentId]);
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }
}
