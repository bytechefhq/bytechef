/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.web.filter;

import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates internal {@code /remote/**} microservice calls with a shared service token. Fail-closed: if no token is
 * configured, or the request token is missing or does not match, the request is rejected with 401. Runs before
 * {@link RemoteMultiTenantFilter} so an unauthenticated caller never establishes tenant context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order(1)
public class RemoteServiceAuthenticationFilter extends OncePerRequestFilter {

    private static final PathPatternRequestMatcher.Builder BUILDER = PathPatternRequestMatcher.withDefaults();

    private static final RequestMatcher REQUEST_MATCHER = new NegatedRequestMatcher(BUILDER.matcher("/remote/**"));

    private final String serviceToken;

    public RemoteServiceAuthenticationFilter(@Value("${bytechef.internal.service-token:}") String serviceToken) {
        this.serviceToken = serviceToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String providedToken = request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN);

        if (serviceToken == null || serviceToken.isBlank() || providedToken == null || providedToken.isBlank() ||
            !MessageDigest.isEqual(
                serviceToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8))) {

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return REQUEST_MATCHER.matches(request);
    }
}
