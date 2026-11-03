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

package com.bytechef.platform.ai.skill.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.ai.skill.security.AiSkillOwnershipResolver;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
@EnableMethodSecurity
public class AiSkillMethodSecurityTestConfiguration {

    public static final Map<String, Long> USER_IDS = Map.of("alice", 1L, "bob", 2L, "root", 3L);

    @Bean
    static UserService userService() {
        UserService userService = mock(UserService.class);

        when(userService.fetchUserByLogin(anyString())).thenAnswer(invocation -> {
            String login = invocation.getArgument(0);

            return Optional.ofNullable(USER_IDS.get(login))
                .map(id -> {
                    User user = new User();

                    user.setId(id);
                    user.setLogin(login);

                    return user;
                });
        });

        return userService;
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
        @Lazy AiSkillService aiSkillService, UserService userService) {

        return new AiSkillMethodSecurityExpressionHandler(new AiSkillOwnershipResolver(aiSkillService, userService));
    }

    private static final class AiSkillMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

        private final ResourceOwnershipResolver resourceOwnershipResolver;

        private AiSkillMethodSecurityExpressionHandler(ResourceOwnershipResolver resourceOwnershipResolver) {
            this.resourceOwnershipResolver = resourceOwnershipResolver;
        }

        @Override
        public EvaluationContext createEvaluationContext(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {

            StandardEvaluationContext standardEvaluationContext =
                (StandardEvaluationContext) super.createEvaluationContext(authentication, methodInvocation);

            AiSkillMethodSecurityExpressionRoot aiSkillMethodSecurityExpressionRoot =
                new AiSkillMethodSecurityExpressionRoot(authentication, methodInvocation, resourceOwnershipResolver);

            aiSkillMethodSecurityExpressionRoot.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
            aiSkillMethodSecurityExpressionRoot.setPermissionEvaluator(getPermissionEvaluator());
            aiSkillMethodSecurityExpressionRoot.setDefaultRolePrefix(getDefaultRolePrefix());

            standardEvaluationContext.setRootObject(aiSkillMethodSecurityExpressionRoot);

            return standardEvaluationContext;
        }
    }

    public static final class AiSkillMethodSecurityExpressionRoot
        extends SecurityExpressionRoot<MethodInvocation> implements MethodSecurityExpressionOperations {

        private final Supplier<? extends Authentication> authenticationSupplier;
        private final ResourceOwnershipResolver resourceOwnershipResolver;
        private final Object target;

        private Object filterObject;
        private Object returnObject;

        private AiSkillMethodSecurityExpressionRoot(
            Supplier<? extends Authentication> authenticationSupplier, MethodInvocation methodInvocation,
            ResourceOwnershipResolver resourceOwnershipResolver) {

            super(authenticationSupplier, methodInvocation);

            this.authenticationSupplier = authenticationSupplier;
            this.resourceOwnershipResolver = resourceOwnershipResolver;
            this.target = methodInvocation.getThis();
        }

        public boolean isResourceOwner(long id, String resourceType) {
            if (NonPlatformUserAuthentication.isCurrent()) {
                return false;
            }

            if (isTenantAdmin()) {
                return true;
            }

            if (!resourceType.equals(resourceOwnershipResolver.resourceType())) {
                return false;
            }

            ResourceOwnershipResolver.ResourceOwner resourceOwner = resourceOwnershipResolver.resolveOwner(id);

            OptionalLong ownerUserId = resourceOwner.ownerUserId();

            Authentication authentication = authenticationSupplier.get();

            Long currentUserId = authentication == null ? null : USER_IDS.get(authentication.getName());

            return ownerUserId.isPresent() && currentUserId != null && ownerUserId.getAsLong() == currentUserId;
        }

        public boolean isTenantAdmin() {
            if (NonPlatformUserAuthentication.isCurrent()) {
                return false;
            }

            Authentication authentication = authenticationSupplier.get();

            if (authentication == null) {
                return false;
            }

            return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(AuthorityConstants.ADMIN::equals);
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
