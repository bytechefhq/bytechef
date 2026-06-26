/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Activates {@link AutomationAuthorizationContext} skip mode for the full synchronous execution of any type or method
 * annotated with {@code com.bytechef.automation.configuration.security.SkipAutomationAuthorization}. Ordered at
 * {@link Ordered#HIGHEST_PRECEDENCE} so the skip flag is set before any nested method-security interceptor evaluates a
 * downstream automation {@code @PreAuthorize}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SkipAutomationAuthorizationAspect {

    @Around("@within(com.bytechef.automation.configuration.security.SkipAutomationAuthorization) || " +
        "@annotation(com.bytechef.automation.configuration.security.SkipAutomationAuthorization)")
    public Object skipAutomationAuthorization(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return AutomationAuthorizationContext.callSkippingChecks(proceedingJoinPoint::proceed);
    }
}
