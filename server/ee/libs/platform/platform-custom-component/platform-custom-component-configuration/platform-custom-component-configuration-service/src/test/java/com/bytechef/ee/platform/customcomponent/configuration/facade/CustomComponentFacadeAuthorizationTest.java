/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@link PreAuthorize} expressions on the EE custom component mutations
 * ({@link CustomComponentFacadeImpl#save}, {@link CustomComponentFacadeImpl#delete} and
 * {@link CustomComponentServiceImpl#enableCustomComponent}) through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. Each is an authority
 * check, so it must decide on the caller's {@code ROLE_ADMIN} authority alone and never consult
 * {@link PermissionService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeAuthorizationTest {

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testSaveRequiresTheAdminAuthority(boolean admin) {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, findMethod(CustomComponentFacadeImpl.class, "save"), admin))
            .as("save must %s a caller %s ROLE_ADMIN", admin ? "allow" : "deny", admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testDeleteRequiresTheAdminAuthority(boolean admin) {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, findMethod(CustomComponentFacadeImpl.class, "delete"), admin))
            .as("delete must %s a caller %s ROLE_ADMIN", admin ? "allow" : "deny", admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testEnableCustomComponentRequiresTheAdminAuthority(boolean admin) {
        PermissionService permissionService = mock(PermissionService.class);

        Method method = findMethod(CustomComponentServiceImpl.class, "enableCustomComponent");

        assertThat(evaluateGuard(permissionService, method, admin))
            .as("enableCustomComponent must %s a caller %s ROLE_ADMIN", admin ? "allow" : "deny",
                admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, boolean admin) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "alice", "credentials", List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER")));

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[method.getParameterCount()]);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(Class<?> targetClass, String methodName) {
        List<Method> matches = Arrays.stream(targetClass.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on %s", methodName, targetClass.getSimpleName())
            .hasSize(1);

        return matches.getFirst();
    }
}
