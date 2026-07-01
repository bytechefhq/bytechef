/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.ee.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.core.Authentication;

/**
 * Decorator that rehydrates the request's tenant and Spring Security principal from the
 * {@link AgentToolInvocationContext} carried on the Spring AI {@link ToolContext}, before delegating. Spring AI
 * dispatches tool calls on Reactor scheduler / worker threads that do not inherit the request thread's
 * {@code TenantContext} or SecurityContext thread-locals, so a tool that reads tenant-scoped data or hits a
 * {@code @PreAuthorize} method would otherwise see the {@code public} tenant and no principal. Each half is
 * independent: a missing tenant id leaves the tenant unchanged, and the security context is restored from either a
 * captured {@link Authentication} (preferred — works for principals with no backing platform user, such as the embedded
 * API-key principal) or, failing that, a user id rehydrated via {@link SecurityContextRehydrator}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class RehydrateContextToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final SecurityContextRehydrator securityContextRehydrator;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private RehydrateContextToolCallback(ToolCallback delegate, SecurityContextRehydrator securityContextRehydrator) {
        this.delegate = delegate;
        this.securityContextRehydrator = securityContextRehydrator;
    }

    /**
     * Wraps {@code delegate} so each {@link #call(String, ToolContext)} runs under the invocation's tenant + principal.
     * Idempotent — re-wrapping a callback already wrapped by this class returns it unchanged.
     */
    public static ToolCallback wrap(ToolCallback delegate, SecurityContextRehydrator securityContextRehydrator) {
        if (delegate instanceof RehydrateContextToolCallback) {
            return delegate;
        }

        return new RehydrateContextToolCallback(delegate, securityContextRehydrator);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null) {
            return delegate.call(toolInput, toolContext);
        }

        String tenantId = invocationContext.tenantId();

        Supplier<String> securedCall =
            () -> withSecurityContext(invocationContext, () -> delegate.call(toolInput, toolContext));

        if (tenantId == null) {
            return securedCall.get();
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        try {
            TenantContext.setCurrentTenantId(tenantId);

            return securedCall.get();
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    private String withSecurityContext(AgentToolInvocationContext invocationContext, Supplier<String> action) {
        Supplier<String> guardedAction = invocationContext.skipAutomationAuthorization()
            ? () -> callSkippingChecks(action)
            : action;

        Authentication authentication = invocationContext.authentication();

        if (authentication != null) {
            return SecurityUtils.runAs(authentication, guardedAction);
        }

        return securityContextRehydrator.withUserSecurityContext(invocationContext.userId(), guardedAction);
    }

    private static String callSkippingChecks(Supplier<String> action) {
        try {
            return AutomationAuthorizationContext.callSkippingChecks(action::get);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }
}
