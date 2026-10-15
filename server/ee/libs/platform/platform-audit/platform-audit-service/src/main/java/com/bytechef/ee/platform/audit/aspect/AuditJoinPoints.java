/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.audit.Audited;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AuditJoinPoints {

    private AuditJoinPoints() {
    }

    // AnnotationUtils.getAnnotation, not AnnotatedElementUtils.findMergedAnnotation: the latter also searches the
    // declaring class's interfaces, which the pointcuts and AuditedMethodValidator do not — both see only the resolved
    // implementation method below. A single lookup strategy avoids one seeing @Audited (via an interface) that the
    // other misses.
    static @Nullable Audited findAudited(ProceedingJoinPoint proceedingJoinPoint) {
        return AnnotationUtils.getAnnotation(resolveMethod(proceedingJoinPoint), Audited.class);
    }

    static AuditInvocation createInvocation(
        ProceedingJoinPoint proceedingJoinPoint, String event, @Nullable Object result, @Nullable Object captured,
        AuditOutcome auditOutcome, @Nullable Class<? extends Throwable> errorClass) {

        return new AuditInvocation(
            event, getArguments(proceedingJoinPoint), result, captured, auditOutcome, errorClass);
    }

    // The signature's method is the interface method when the call came through a JDK proxy; the annotation sits on the
    // implementation, so resolve the most specific method on the target class first.
    private static Method resolveMethod(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Object target = proceedingJoinPoint.getTarget();

        if (target == null) {
            return methodSignature.getMethod();
        }

        return AopUtils.getMostSpecificMethod(methodSignature.getMethod(), AopUtils.getTargetClass(target));
    }

    private static Map<String, @Nullable Object> getArguments(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

        String[] parameterNames = methodSignature.getParameterNames();
        Object[] argumentValues = proceedingJoinPoint.getArgs();

        Map<String, @Nullable Object> arguments = new LinkedHashMap<>();

        for (int index = 0; index < argumentValues.length; index++) {
            String parameterName = parameterNames != null && index < parameterNames.length
                ? parameterNames[index] : "arg" + index;

            arguments.put(parameterName, argumentValues[index]);
        }

        return arguments;
    }
}
