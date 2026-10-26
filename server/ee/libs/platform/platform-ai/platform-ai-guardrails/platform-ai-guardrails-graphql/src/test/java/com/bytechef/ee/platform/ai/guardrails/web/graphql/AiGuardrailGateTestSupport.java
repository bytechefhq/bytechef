/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.web.graphql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AiGuardrailGateTestSupport {

    private AiGuardrailGateTestSupport() {
    }

    @SuppressFBWarnings("SPEL_INJECTION")
    static boolean isAuthorized(
        Class<?> controllerType, Object controllerInstance, String methodName, Class<?>[] parameterTypes,
        Object[] arguments, Authentication authentication, PermissionEvaluator permissionEvaluator) {

        try {
            Method method = controllerType.getDeclaredMethod(methodName, parameterTypes);

            MethodInvocation methodInvocation = mock(MethodInvocation.class);

            when(methodInvocation.getMethod()).thenReturn(method);
            when(methodInvocation.getArguments()).thenReturn(arguments);
            when(methodInvocation.getThis()).thenReturn(controllerInstance);

            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

            expressionHandler.setPermissionEvaluator(permissionEvaluator);

            EvaluationContext evaluationContext =
                expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

            Expression expression = expressionHandler.getExpressionParser()
                .parseExpression(preAuthorize.value());

            return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalStateException(noSuchMethodException);
        }
    }
}
