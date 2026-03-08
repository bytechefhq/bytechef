/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerApiKeyAuthenticationProvider(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmbeddedMcpServerApiKeyAuthenticationToken embeddedMcpServerApiKeyAuthenticationToken =
            (EmbeddedMcpServerApiKeyAuthenticationToken) authentication;

        McpServer mcpServer = mcpServerService.getMcpServer(
            embeddedMcpServerApiKeyAuthenticationToken.getMcpServerSecretKey());

        if (!Objects.equals(
            mcpServer.getSecretKey(), embeddedMcpServerApiKeyAuthenticationToken.getMcpServerSecretKey())) {

            throw new BadCredentialsException("Invalid secret key");
        }

        return new EmbeddedMcpServerApiKeyAuthenticationToken(
            embeddedMcpServerApiKeyAuthenticationToken.getAuthSecretKey());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(EmbeddedMcpServerApiKeyAuthenticationToken.class);
    }
}
