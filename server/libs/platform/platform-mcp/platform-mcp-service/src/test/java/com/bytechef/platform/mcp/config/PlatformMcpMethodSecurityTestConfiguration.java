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

package com.bytechef.platform.mcp.config;

import static org.mockito.Mockito.mock;

import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
@EnableMethodSecurity
public class PlatformMcpMethodSecurityTestConfiguration {

    @Bean
    static PermissionEvaluator permissionEvaluator() {
        return mock(PermissionEvaluator.class);
    }

    @Bean
    static TenantAdminCheck tenantAdminCheck() {
        return mock(TenantAdminCheck.class);
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
        PermissionEvaluator permissionEvaluator, TenantAdminCheck tenantAdminCheck) {

        TenantAdminMethodSecurityExpressionHandler tenantAdminMethodSecurityExpressionHandler =
            new TenantAdminMethodSecurityExpressionHandler(tenantAdminCheck);

        tenantAdminMethodSecurityExpressionHandler.setPermissionEvaluator(permissionEvaluator);

        return tenantAdminMethodSecurityExpressionHandler;
    }

    public interface TenantAdminCheck {

        boolean isTenantAdmin();
    }

    private static final class TenantAdminMethodSecurityExpressionHandler
        extends DefaultMethodSecurityExpressionHandler {

        private final TenantAdminCheck tenantAdminCheck;

        private TenantAdminMethodSecurityExpressionHandler(TenantAdminCheck tenantAdminCheck) {
            this.tenantAdminCheck = tenantAdminCheck;
        }

        @Override
        public EvaluationContext createEvaluationContext(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {

            StandardEvaluationContext standardEvaluationContext =
                (StandardEvaluationContext) super.createEvaluationContext(authentication, methodInvocation);

            TenantAdminMethodSecurityExpressionRoot tenantAdminMethodSecurityExpressionRoot =
                new TenantAdminMethodSecurityExpressionRoot(authentication, methodInvocation, tenantAdminCheck);

            tenantAdminMethodSecurityExpressionRoot.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
            tenantAdminMethodSecurityExpressionRoot.setPermissionEvaluator(getPermissionEvaluator());
            tenantAdminMethodSecurityExpressionRoot.setDefaultRolePrefix(getDefaultRolePrefix());

            standardEvaluationContext.setRootObject(tenantAdminMethodSecurityExpressionRoot);

            return standardEvaluationContext;
        }
    }

    public static final class TenantAdminMethodSecurityExpressionRoot
        extends SecurityExpressionRoot<MethodInvocation> implements MethodSecurityExpressionOperations {

        private final Object target;
        private final TenantAdminCheck tenantAdminCheck;

        private Object filterObject;
        private Object returnObject;

        private TenantAdminMethodSecurityExpressionRoot(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation,
            TenantAdminCheck tenantAdminCheck) {

            super(authentication, methodInvocation);

            this.target = methodInvocation.getThis();
            this.tenantAdminCheck = tenantAdminCheck;
        }

        public boolean isTenantAdmin() {
            return tenantAdminCheck.isTenantAdmin();
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
}
