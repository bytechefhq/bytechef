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

package com.bytechef.automation.task.config;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
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
public class ApprovalTaskMethodSecurityTestConfiguration {

    @Bean
    static AuthorizationCheck authorizationCheck() {
        return mock(AuthorizationCheck.class);
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(AuthorizationCheck authorizationCheck) {

        ApprovalTaskMethodSecurityExpressionHandler approvalTaskMethodSecurityExpressionHandler =
            new ApprovalTaskMethodSecurityExpressionHandler(authorizationCheck);

        approvalTaskMethodSecurityExpressionHandler.setPermissionEvaluator(new PermissionEvaluator() {

            @Override
            public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
                return false;
            }

            @Override
            public boolean hasPermission(
                Authentication authentication, Serializable targetId, String targetType, Object permission) {

                return authorizationCheck.hasPermission(targetId, targetType, permission);
            }
        });

        return approvalTaskMethodSecurityExpressionHandler;
    }

    public interface AuthorizationCheck {

        boolean hasPermission(Serializable targetId, String targetType, Object permission);

        boolean isCurrentUser(Long userId);

        boolean isResourceOwner(Serializable resourceId, String resourceType);

        boolean isTenantAdmin();
    }

    private static final class ApprovalTaskMethodSecurityExpressionHandler
        extends DefaultMethodSecurityExpressionHandler {

        private final AuthorizationCheck authorizationCheck;

        private ApprovalTaskMethodSecurityExpressionHandler(AuthorizationCheck authorizationCheck) {
            this.authorizationCheck = authorizationCheck;
        }

        @Override
        public EvaluationContext createEvaluationContext(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {

            StandardEvaluationContext standardEvaluationContext =
                (StandardEvaluationContext) super.createEvaluationContext(authentication, methodInvocation);

            ApprovalTaskMethodSecurityExpressionRoot approvalTaskMethodSecurityExpressionRoot =
                new ApprovalTaskMethodSecurityExpressionRoot(authentication, methodInvocation, authorizationCheck);

            approvalTaskMethodSecurityExpressionRoot.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
            approvalTaskMethodSecurityExpressionRoot.setPermissionEvaluator(getPermissionEvaluator());
            approvalTaskMethodSecurityExpressionRoot.setDefaultRolePrefix(getDefaultRolePrefix());

            standardEvaluationContext.setRootObject(approvalTaskMethodSecurityExpressionRoot);

            return standardEvaluationContext;
        }
    }

    public static final class ApprovalTaskMethodSecurityExpressionRoot
        extends SecurityExpressionRoot<MethodInvocation> implements MethodSecurityExpressionOperations {

        private final Object target;
        private final AuthorizationCheck authorizationCheck;

        private Object filterObject;
        private Object returnObject;

        private ApprovalTaskMethodSecurityExpressionRoot(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation,
            AuthorizationCheck authorizationCheck) {

            super(authentication, methodInvocation);

            this.target = methodInvocation.getThis();
            this.authorizationCheck = authorizationCheck;
        }

        public boolean isCurrentUser(Long userId) {
            return authorizationCheck.isCurrentUser(userId);
        }

        public boolean isResourceOwner(Serializable resourceId, String resourceType) {
            return authorizationCheck.isResourceOwner(resourceId, resourceType);
        }

        public boolean isTenantAdmin() {
            return authorizationCheck.isTenantAdmin();
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
