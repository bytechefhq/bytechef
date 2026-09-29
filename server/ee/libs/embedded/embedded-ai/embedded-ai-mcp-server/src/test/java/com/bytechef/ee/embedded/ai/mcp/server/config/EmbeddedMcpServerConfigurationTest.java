/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import io.modelcontextprotocol.common.McpTransportContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

        McpTransportContext mcpTransportContext = EmbeddedMcpServerConfiguration.createMcpTransportContext(
            "secret-key", null);

        assertEquals("alice", mcpTransportContext.get("externalUserId"));
        assertSame(authentication, mcpTransportContext.get("authentication"));
    }
}
