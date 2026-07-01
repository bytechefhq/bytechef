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
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;

/**
 * {@link DefaultMethodSecurityExpressionHandler} that installs an {@link AutomationMethodSecurityExpressionRoot} as the
 * {@code @PreAuthorize} SpEL root, so {@code isCurrentUser(...)} and {@code isTenantAdmin()} are available as
 * first-class built-ins alongside the standard operations.
 *
 * @author Ivica Cardic
 */
public class AutomationMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private final PermissionService permissionService;

    public AutomationMethodSecurityExpressionHandler(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public EvaluationContext createEvaluationContext(
        Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {

        StandardEvaluationContext context =
            (StandardEvaluationContext) super.createEvaluationContext(authentication, methodInvocation);

        AutomationMethodSecurityExpressionRoot root =
            new AutomationMethodSecurityExpressionRoot(authentication, methodInvocation, permissionService);

        root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setDefaultRolePrefix(getDefaultRolePrefix());

        context.setRootObject(root);

        return context;
    }
}
