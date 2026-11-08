/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerOAuth2AuthenticationProvider implements AuthenticationProvider {

    private final ConnectedUserService connectedUserService;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerOAuth2AuthenticationProvider(
        ConnectedUserService connectedUserService, McpServerService mcpServerService) {

        this.connectedUserService = connectedUserService;
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmbeddedMcpServerOAuth2AuthenticationToken embeddedMcpServerOAuth2AuthenticationToken =
            (EmbeddedMcpServerOAuth2AuthenticationToken) authentication;

        String mcpServerSecretKey = embeddedMcpServerOAuth2AuthenticationToken.getMcpServerSecretKey();

        McpServer mcpServer = getMcpServer(mcpServerSecretKey);

        if (!mcpServer.isEnabled()) {
            throw new BadCredentialsException("MCP server is disabled");
        }

        if (!mcpServer.isAuthenticationRequired()) {
            return McpAnonymousAuthenticationToken.ofEmbeddedMcpServer(mcpServer.getId());
        }

        long environmentId = embeddedMcpServerOAuth2AuthenticationToken.getEnvironmentId();

        Environment mcpServerEnvironment = mcpServer.getEnvironment();

        if (environmentId != mcpServerEnvironment.ordinal()) {
            throw new BadCredentialsException("X-ENVIRONMENT does not match the MCP server environment");
        }

        String externalUserId = embeddedMcpServerOAuth2AuthenticationToken.getExternalUserId();

        ConnectedUser connectedUser = connectedUserService.fetchConnectedUser(externalUserId, environmentId)
            .orElseGet(() -> connectedUserService.createConnectedUser(externalUserId, environmentId));

        return new EmbeddedMcpServerOAuth2AuthenticationToken(
            environmentId,
            createSpringSecurityUser(
                externalUserId, connectedUser, embeddedMcpServerOAuth2AuthenticationToken.getMappedAuthorities()));
    }

    private McpServer getMcpServer(String mcpServerSecretKey) {
        McpServer mcpServer;

        try {
            mcpServer = mcpServerService.getMcpServer(mcpServerSecretKey);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("Invalid MCP server secret key", illegalArgumentException);
        }

        if (mcpServer.getType() != PlatformType.EMBEDDED) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        return mcpServer;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(EmbeddedMcpServerOAuth2AuthenticationToken.class);
    }

    private User createSpringSecurityUser(
        String externalUserId, ConnectedUser connectedUser, List<GrantedAuthority> mappedAuthorities) {

        if (!connectedUser.isEnabled()) {
            throw new UserNotActivatedException("Connected User " + externalUserId + " was not enabled");
        }

        return new User(connectedUser.getExternalId(), "", mappedAuthorities);
    }
}
