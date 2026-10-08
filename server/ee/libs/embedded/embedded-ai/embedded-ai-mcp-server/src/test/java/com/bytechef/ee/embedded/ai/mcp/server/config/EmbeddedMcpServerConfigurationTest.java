/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import io.modelcontextprotocol.common.McpTransportContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerConfigurationTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTransportContextCarriesTheConnectedUsersExternalIdAndAuthentication() {
        Authentication authentication = TestConnectedUserAuthentication.of("alice");

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        McpTransportContext mcpTransportContext = EmbeddedMcpServerConfiguration.createTransportContext(
            newServerRequest());

        assertEquals("alice", mcpTransportContext.get("externalUserId"));
        assertSame(authentication, mcpTransportContext.get("authentication"));
    }

    @Test
    void testTransportContextRefusesAPlatformSession() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "", List.of()));

        ServerRequest serverRequest = newServerRequest();

        assertThrows(
            AccessDeniedException.class, () -> EmbeddedMcpServerConfiguration.createTransportContext(serverRequest));
    }

    private static ServerRequest newServerRequest() {
        ServerRequest serverRequest = mock(ServerRequest.class);

        when(serverRequest.pathVariable("secretKey")).thenReturn("secret-key");
        when(serverRequest.servletRequest()).thenReturn(mock(HttpServletRequest.class));

        return serverRequest;
    }
}
