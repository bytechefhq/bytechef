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
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
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
