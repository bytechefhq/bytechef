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
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link DefaultMethodSecurityExpressionHandler} that installs an {@link AutomationMethodSecurityExpressionRoot} as the
 * {@code @PreAuthorize} SpEL root, so {@code isCurrentUser(...)} and {@code isTenantAdmin()} are available as
 * first-class built-ins alongside the standard operations.
 *
 * @author Ivica Cardic
 */
public class AutomationMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler
    implements SmartInitializingSingleton {

    private final SingletonSupplier<List<MethodResolver>> contributorMethodResolversSupplier;
    private final PermissionService permissionService;

    public AutomationMethodSecurityExpressionHandler(
        PermissionService permissionService,
        ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

        this.contributorMethodResolversSupplier = SingletonSupplier.of(
            () -> createContributorMethodResolvers(methodSecurityExpressionContributorProvider));
        this.permissionService = permissionService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        contributorMethodResolversSupplier.obtain();
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

        List<MethodResolver> methodResolvers = new ArrayList<>(context.getMethodResolvers());

        methodResolvers.addAll(contributorMethodResolversSupplier.obtain());

        context.setMethodResolvers(methodResolvers);

        return context;
    }

    private static List<MethodResolver> createContributorMethodResolvers(
        ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

        Map<String, String> methodOwners = new HashMap<>();

        for (Method method : AutomationMethodSecurityExpressionRoot.class.getMethods()) {
            methodOwners.put(method.getName(), AutomationMethodSecurityExpressionRoot.class.getName());
        }

        List<MethodResolver> methodResolvers = new ArrayList<>();

        methodSecurityExpressionContributorProvider.orderedStream()
            .forEach(contributor -> {
                Class<?> contributorClass = ClassUtils.getUserClass(contributor);

                for (Method method : contributorClass.getMethods()) {
                    if (method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers())) {
                        continue;
                    }

                    String owner = methodOwners.putIfAbsent(method.getName(), contributorClass.getName());

                    if (owner != null && !owner.equals(contributorClass.getName())) {
                        throw new IllegalStateException(
                            "Method security expression '%s' of %s is already defined by %s".formatted(
                                method.getName(), contributorClass.getName(), owner));
                    }
                }

                methodResolvers.add(new MethodSecurityExpressionContributorMethodResolver(contributor));
            });

        return List.copyOf(methodResolvers);
    }
}
