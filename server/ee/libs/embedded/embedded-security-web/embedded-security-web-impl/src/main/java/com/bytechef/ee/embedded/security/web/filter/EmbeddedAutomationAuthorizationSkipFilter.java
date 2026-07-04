/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.filter;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bypasses automation RBAC for embedded requests. The embedded workflow builder/editor reuses the platform
 * {@code /api/.../internal/...} workflow-editor endpoints, whose facade methods are gated by
 * {@code @PreAuthorize("hasPermission(#workflowId, 'Workflow', ...)")}. An embedded request authenticates as an
 * {@link EmbeddedApiKeyAuthenticationToken} — an API-key/connected-user identity with no row in the {@code user} table
 * — so resolving it for a platform scope check throws {@code UserNotFoundException} (HTTP 4xx/5xx) instead of running.
 * Embedded enforces its own connected-user authorization, so for the duration of an embedded request the synchronous
 * call stack runs with {@link AutomationAuthorizationContext} skip mode enabled, mirroring the
 * {@code @SkipAutomationAuthorization} delegation used by embedded facades.
 *
 * <p>
 * Registered immediately after the embedded {@code ApiKeyAuthenticationFilter} so the {@link Authentication} is already
 * established. The {@code EmbeddedApiKeyAuthenticationToken} check means non-embedded requests sharing the chain are
 * untouched and keep enforcing RBAC.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedAutomationAuthorizationSkipFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (!(authentication instanceof EmbeddedApiKeyAuthenticationToken)) {
            filterChain.doFilter(request, response);

            return;
        }

        try {
            AutomationAuthorizationContext.callSkippingChecks(() -> {
                filterChain.doFilter(request, response);

                return null;
            });
        } catch (IOException | ServletException | RuntimeException exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new ServletException(throwable);
        }
    }
}
