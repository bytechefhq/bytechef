/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.filter;

import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that blocks password-based login for users whose email domain has an enforced SSO identity provider. Only
 * intercepts {@code POST /api/authentication} (the form login endpoint).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SsoEnforcementFilter extends OncePerRequestFilter {

    private final IdentityProviderService identityProviderService;

    @SuppressFBWarnings("EI")
    public SsoEnforcementFilter(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/authentication".equals(request.getRequestURI())) {
            String username = request.getParameter("username");

            if (username != null && username.contains("@")) {
                String domain = username.substring(username.lastIndexOf('@') + 1);

                Optional<IdentityProvider> identityProvider = identityProviderService.fetchByDomain(domain);

                if (identityProvider.isPresent() && identityProvider.get()
                    .isEnforced()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter()
                        .write(
                            "{\"message\":\"Your organization requires SSO login. " +
                                "Please use the SSO button on the login page.\"}");

                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
