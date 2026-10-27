/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
class GatewayProxySecurityConfigurationIntTest {

    @Autowired
    @Qualifier("proxyFilterChain")
    private SecurityFilterChain proxyFilterChain;

    @Test
    void testRoutedRequestMatches() {
        assertThat(proxyFilterChain.matches(request("/api/automation/internal/projects"))).isTrue();
    }

    @Test
    void testGatewayOwnedRequestDoesNotMatch() {
        assertThat(proxyFilterChain.matches(request("/api/account"))).isFalse();
    }

    @Test
    void testErrorDispatchOfRoutedRequestMatches() {
        assertThat(proxyFilterChain.matches(errorDispatch("/api/automation/internal/projects"))).isTrue();
    }

    @Test
    void testErrorDispatchOfGatewayOwnedRequestDoesNotMatch() {
        assertThat(proxyFilterChain.matches(errorDispatch("/api/account"))).isFalse();
    }

    @Test
    void testErrorDispatchOfUserGraphQlRequestDoesNotMatch() {
        MockHttpServletRequest mockHttpServletRequest = errorDispatch("/graphql");

        mockHttpServletRequest.addHeader(GatewayProxySecurityConfiguration.USER_GRAPHQL_SCHEMA_HEADER, "user");

        assertThat(proxyFilterChain.matches(mockHttpServletRequest)).isFalse();
    }

    private static MockHttpServletRequest errorDispatch(String originalRequestUri) {
        MockHttpServletRequest mockHttpServletRequest = request("/error");

        mockHttpServletRequest.setDispatcherType(DispatcherType.ERROR);
        mockHttpServletRequest.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, originalRequestUri);

        return mockHttpServletRequest;
    }

    private static MockHttpServletRequest request(String requestUri) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", requestUri);

        mockHttpServletRequest.setServletPath(requestUri);

        return mockHttpServletRequest;
    }
}
