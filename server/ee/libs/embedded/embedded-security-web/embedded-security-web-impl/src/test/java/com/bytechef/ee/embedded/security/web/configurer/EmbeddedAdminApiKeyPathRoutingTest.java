/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The embedded admin paths are claimed by {@link EmbeddedAdminApiKeySecurityConfigurer} and must be excluded from
 * {@link EmbeddedApiKeySecurityConfigurer}'s connected-user auth, which issues a principal with zero authorities that
 * no {@code ROLE_ADMIN} facade guard can accept.
 *
 * <p>
 * Pinned because the failure is silent in both directions: were the carve-out dropped, the admin endpoints would answer
 * 403 forever; were the admin pattern widened, a tenant's end-user credential could reach them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedAdminApiKeyPathRoutingTest {

    private static final RequestMatcher ADMIN_MATCHER =
        regexMatcher(EmbeddedAdminApiKeySecurityConfigurer.PATH_PATTERN);
    private static final RequestMatcher CONNECTED_USER_MATCHER = regexMatcher("^/api/embedded/v[0-9]+/.+");

    @Test
    void testAdminPathsAreClaimedByTheAdminConfigurer() {
        for (String path : new String[] {
            "/api/embedded/v1/automation-project-code-workflows",
            "/api/embedded/v1/automation-project-code-workflows/deploy",
            "/api/embedded/v2/automation-project-code-workflows"
        }) {
            assertThat(ADMIN_MATCHER.matches(request(path)))
                .as("admin configurer must claim %s", path)
                .isTrue();
        }
    }

    @Test
    void testAdminPathsAreExcludedFromConnectedUserAuth() {
        String path = "/api/embedded/v1/automation-project-code-workflows/deploy";

        assertThat(CONNECTED_USER_MATCHER.matches(request(path)))
            .as("the broad embedded pattern still matches, which is why the carve-out is needed")
            .isTrue();
        assertThat(carvedOut(path))
            .as("connected-user auth must not claim %s", path)
            .isFalse();
    }

    @Test
    void testOrdinaryEmbeddedPathsKeepConnectedUserAuth() {
        for (String path : new String[] {
            "/api/embedded/v1/user-1/integrations",
            "/api/embedded/v1/app-events",
            "/api/embedded/v1/workflows/some-uuid"
        }) {
            assertThat(ADMIN_MATCHER.matches(request(path)))
                .as("admin configurer must not claim %s", path)
                .isFalse();
            assertThat(carvedOut(path))
                .as("connected-user auth must still claim %s", path)
                .isTrue();
        }
    }

    /**
     * Mirrors the first clause of {@link EmbeddedApiKeySecurityConfigurer}'s matcher.
     */
    private static boolean carvedOut(String path) {
        HttpServletRequest request = request(path);

        return CONNECTED_USER_MATCHER.matches(request) && !ADMIN_MATCHER.matches(request);
    }

    private static HttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        // RegexRequestMatcher matches on servletPath + pathInfo, which MockHttpServletRequest leaves empty when only
        // the request URI is given.
        request.setServletPath(path);

        return request;
    }
}
