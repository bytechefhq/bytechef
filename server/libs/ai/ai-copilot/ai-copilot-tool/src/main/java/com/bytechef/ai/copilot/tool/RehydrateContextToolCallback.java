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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
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
 * Decorator that rehydrates the request's environment, tenant, and Spring Security principal from the
 * {@link AgentToolInvocationContext} on the Spring AI {@link ToolContext} before delegating, since Spring AI runs tool
 * calls on worker threads that do not inherit the {@code EnvironmentContext}, {@code TenantContext}, or SecurityContext
 * thread-locals. Each part is independent; security is restored from a captured {@link Authentication} (preferred —
 * supports principals with no backing platform user, such as the embedded API-key principal) or, failing that, a user
 * id via {@link SecurityContextRehydrator}.
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

        return withEnvironment(
            invocationContext.environmentId(),
            () -> withTenant(
                invocationContext.tenantId(),
                () -> withSecurityContext(invocationContext, () -> delegate.call(toolInput, toolContext))));
    }

    private static String withEnvironment(@Nullable Long environmentId, Supplier<String> action) {
        if (environmentId == null || environmentId < 0 || environmentId >= Environment.values().length) {
            return action.get();
        }

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        EnvironmentContext.set(environmentId.intValue());

        try {
            return action.get();
        } finally {
            if (previousEnvironment == null) {
                EnvironmentContext.clear();
            } else {
                EnvironmentContext.set(previousEnvironment);
            }
        }
    }

    private static String withTenant(@Nullable String tenantId, Supplier<String> action) {
        if (tenantId == null) {
            return action.get();
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        try {
            TenantContext.setCurrentTenantId(tenantId);

            return action.get();
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
