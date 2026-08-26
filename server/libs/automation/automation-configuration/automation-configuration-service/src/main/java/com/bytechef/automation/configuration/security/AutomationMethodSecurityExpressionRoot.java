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

import com.bytechef.automation.configuration.security.ResourceMembershipDecider.Outcome;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * Custom {@code @PreAuthorize} SpEL root that adds two ByteChef-specific built-ins on top of the standard Spring
 * Security expression operations ({@code hasPermission}, {@code hasRole}, {@code isAuthenticated}, …):
 *
 * <ul>
 * <li>{@code isCurrentUser(#id)} — grants when the supplied id is the current authenticated user's id.</li>
 * <li>{@code isTenantAdmin()} — grants when the current user is a global tenant administrator.</li>
 * <li>{@code isResourceOwner(#id, 'Type')} — grants when the current user owns the identified resource.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public final class AutomationMethodSecurityExpressionRoot
    extends SecurityExpressionRoot<MethodInvocation> implements MethodSecurityExpressionOperations {

    private final PermissionService permissionService;
    private final ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;
    private final Object target;

    private Object filterObject;
    private Object returnObject;

    AutomationMethodSecurityExpressionRoot(
        Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation,
        PermissionService permissionService,
        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider) {

        super(authentication, methodInvocation);

        this.permissionService = permissionService;
        this.resourceMembershipResolverProvider = resourceMembershipResolverProvider;
        this.target = methodInvocation.getThis();
    }

    /**
     * Returns {@code true} if {@code userId} matches the current authenticated user. Bypassed (returns {@code true})
     * under skip mode; a connected user never reaches that bypass, since {@code SkipAutomationAuthorizationAspect} arms
     * nothing for a principal {@link ResourceMembershipResolver} governs — and a connected user is not any {@code user}
     * table row, having none.
     */
    public boolean isCurrentUser(long userId) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.isCurrentUser(userId);
    }

    /**
     * Returns {@code true} if the current user is a global tenant administrator. Bypassed (returns {@code true}) under
     * skip mode; a connected user never reaches that bypass — a connected user is not a tenant admin, which is what the
     * principal is rather than a policy choice.
     */
    public boolean isTenantAdmin() {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.isTenantAdmin();
    }

    /**
     * Returns {@code true} if the current user owns the resource of {@code resourceType} identified by {@code id},
     * resolved via the registered {@code ResourceOwnershipResolver}. Bypassed (returns {@code true}) under skip mode; a
     * connected user never reaches that bypass, so this still runs for real for one — it is what gates
     * sharing/ownership management (connection credential replacement, connection/project access grants, signing-key
     * ownership), none of which a connected user has.
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
     * held in a single environment would be an escalation. Bypassed (returns {@code true}) under skip mode; a connected
     * user never reaches that bypass and has no workspace membership to grant.
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
     * promotion and is lost on worker threads. Bypassed (returns {@code true}) under skip mode; a connected user never
     * reaches that bypass and has no workspace membership to grant.
     */
    public boolean hasWorkspaceScopeInEnvironment(long workspaceId, String scope, Environment environment) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.hasWorkspaceScope(workspaceId, scope, environment);
    }

    /**
     * Requires {@code scope} in the environment the operation acts on, for the workspace owning {@code workflowId}. A
     * workflow has no environment of its own, so no {@code ResourceEnvironmentResolver} can supply one and
     * {@code hasPermission(#id, 'Workflow', ...)} necessarily unions the environments the caller can reach — a member
     * who is editor in Development would pass and could then act in Production. Use this wherever the caller supplies
     * the environment to run in.
     * <p>
     * {@code environmentId} is the caller's own ordinal and is deliberately not trusted. For a principal confined to a
     * single environment (an api-key caller: embedded connected user, embedded MCP) it is ignored outright in favour of
     * the principal's own — see {@link PrincipalEnvironment#resolveEffectiveEnvironmentId(Long)} — and the callers that
     * execute afterwards resolve the same way, so no caller can be authorised for one environment and execute in
     * another. For a session principal it is honoured, since such a caller genuinely chooses per request; absent or out
     * of range then denies rather than falling back to a default, because an environment that cannot be identified
     * cannot be authorised.
     * <p>
     * For a principal {@link ResourceMembershipResolver} does not govern, the skip check still runs before the ordinal
     * is validated, matching the environment-unaware gate; validating the ordinal ahead of it would deny requests skip
     * mode permits. A governed principal is answered from its membership ahead of the skip check instead — see
     * {@link ResourceMembershipDecider}.
     */
    public boolean hasWorkflowScopeInEnvironment(String workflowId, String scope, Long environmentId) {
        // Ticket 1051: this built-in reads the skip state itself and never reaches hasResourceScope(...), so wiring
        // the decider at those two places does not cover it. It has to be consulted here as well, or the embedded
        // builder's Run button (WorkflowTestApiController.startWorkflowTest, the only production gate using this
        // built-in) would be decided by the short circuit below rather than by membership.
        Outcome outcome = ResourceMembershipDecider.decide(
            resourceMembershipResolverProvider, workflowId, "Workflow", scope);

        if (outcome == Outcome.DENY) {
            return false;
        }

        // Resolved once, and used for BOTH the range check and the scope check below, so the environment authorised
        // here is the one the caller is actually confined to. WorkflowTestApiController and AiAgentTestApiController
        // resolve the same way for the execution that follows -- authorising one environment and executing in another
        // is the whole bug.
        Long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        Environment[] environments = Environment.values();
        boolean environmentIdResolvable =
            effectiveEnvironmentId != null && effectiveEnvironmentId >= 0
                && effectiveEnvironmentId < environments.length;

        // Membership answers whose workflow this is, never which environment it may run in -- but for a principal
        // CONFINED to one environment the caller-supplied ordinal is not a choice to validate, it is a degree of
        // freedom that should not exist. resolveEffectiveEnvironmentId substitutes the principal's own, so the two
        // cannot disagree and the range check below is all that is left to do. Validating the parameter instead was
        // tried and reverted: the two ends are independently defaulted client-side and disagree in the default
        // embedded configuration, so the comparison denied callers asking for nothing unusual.
        if (outcome == Outcome.GRANT) {
            return environmentIdResolvable;
        }

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        if (!environmentIdResolvable) {
            return false;
        }

        return permissionService.hasWorkflowScope(
            workflowId, scope, environments[effectiveEnvironmentId.intValue()]);
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
