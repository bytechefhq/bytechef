/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpProtectedResourceMetadataAuthenticationEntryPointTest {

    private final EmbeddedMcpProtectedResourceMetadataAuthenticationEntryPoint entryPoint =
        new EmbeddedMcpProtectedResourceMetadataAuthenticationEntryPoint();

    @Test
    void testCommenceWritesDiscoveryChallengePointingAtResourceMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/embedded/acme.tenant/mcp");

        request.setScheme("https");
        request.setServerName("mcp.example.com");
        request.setServerPort(443);

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer resource_metadata=\"" +
                    "https://mcp.example.com/.well-known/oauth-protected-resource/api/embedded/acme.tenant/mcp\"");
    }

    @Test
    void testCommenceDropsQueryStringFromResourceMetadataPointer() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/embedded/acme.tenant/mcp");

        request.setScheme("https");
        request.setServerName("mcp.example.com");
        request.setServerPort(443);
        request.setQueryString("foo=bar");

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("required"));

        assertThat(response.getHeader("WWW-Authenticate")).doesNotContain("foo=bar");
    }
}
