/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedAutomationAuthorizationSkipFilterTest {

    private final EmbeddedAutomationAuthorizationSkipFilter filter = new EmbeddedAutomationAuthorizationSkipFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEmbeddedRequestEnablesSkipDuringChain() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new EmbeddedApiKeyAuthenticationToken(1L, "external-user", "secret", "tenant"));

        AtomicBoolean skipDuringChain = new AtomicBoolean(false);

        FilterChain filterChain =
            (request, response) -> skipDuringChain.set(AutomationAuthorizationContext.isSkipChecks());

        filter.doFilterInternal(mock(HttpServletRequest.class), mock(HttpServletResponse.class), filterChain);

        assertThat(skipDuringChain).isTrue();

        // Cleared after the request so it cannot leak to the next one.
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testNonEmbeddedRequestDoesNotEnableSkip() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        AtomicBoolean skipDuringChain = new AtomicBoolean(true);

        FilterChain filterChain =
            (request, response) -> skipDuringChain.set(AutomationAuthorizationContext.isSkipChecks());

        filter.doFilterInternal(mock(HttpServletRequest.class), mock(HttpServletResponse.class), filterChain);

        assertThat(skipDuringChain).isFalse();
    }

    @Test
    void testNoAuthenticationDoesNotEnableSkip() throws Exception {
        AtomicBoolean skipDuringChain = new AtomicBoolean(true);

        FilterChain filterChain =
            (request, response) -> skipDuringChain.set(AutomationAuthorizationContext.isSkipChecks());

        filter.doFilterInternal(mock(HttpServletRequest.class), mock(HttpServletResponse.class), filterChain);

        assertThat(skipDuringChain).isFalse();
    }
}
