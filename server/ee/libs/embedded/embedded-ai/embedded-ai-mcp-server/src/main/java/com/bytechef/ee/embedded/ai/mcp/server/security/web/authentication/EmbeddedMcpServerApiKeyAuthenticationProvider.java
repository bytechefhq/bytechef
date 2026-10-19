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
import com.bytechef.platform.security.web.mcp.McpServerDisabledException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
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

    private final ConnectedUserService connectedUserService;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerApiKeyAuthenticationProvider(
        ConnectedUserService connectedUserService, McpServerService mcpServerService) {

        this.connectedUserService = connectedUserService;
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmbeddedMcpServerApiKeyAuthenticationToken embeddedMcpServerApiKeyAuthenticationToken =
            (EmbeddedMcpServerApiKeyAuthenticationToken) authentication;

        McpServer mcpServer = getMcpServer(embeddedMcpServerApiKeyAuthenticationToken.getMcpServerSecretKey());

        Environment mcpServerEnvironment = mcpServer.getEnvironment();

        boolean environmentMatches =
            embeddedMcpServerApiKeyAuthenticationToken.getEnvironmentId() == mcpServerEnvironment.ordinal();

        if (embeddedMcpServerApiKeyAuthenticationToken.isEnvironmentRequested() && !environmentMatches) {
            throw new BadCredentialsException("X-ENVIRONMENT does not match the MCP server environment");
        }

        if (!mcpServer.isAuthenticationRequired()) {
            checkEnabled(mcpServer);

            return McpAnonymousAuthenticationToken.ofEmbeddedMcpServer(mcpServer.getId());
        }

        String externalUserId = embeddedMcpServerApiKeyAuthenticationToken.getExternalUserId();

        if (externalUserId == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        if (!environmentMatches) {
            throw new BadCredentialsException("X-ENVIRONMENT does not match the MCP server environment");
        }

        long environmentId = embeddedMcpServerApiKeyAuthenticationToken.getEnvironmentId();

        Optional<ConnectedUser> existingConnectedUser = connectedUserService.fetchConnectedUser(
            externalUserId, environmentId);

        if (existingConnectedUser.isPresent()) {
            ConnectedUser connectedUser = existingConnectedUser.get();

            org.springframework.security.core.userdetails.User user = createSpringSecurityUser(
                externalUserId, connectedUser);

            checkEnabled(mcpServer);

            return new EmbeddedMcpServerApiKeyAuthenticationToken(environmentId, connectedUser.getId(), user);
        }

        checkEnabled(mcpServer);

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(externalUserId, environmentId);

        return new EmbeddedMcpServerApiKeyAuthenticationToken(
            environmentId, connectedUser.getId(), createSpringSecurityUser(externalUserId, connectedUser));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(EmbeddedMcpServerApiKeyAuthenticationToken.class);
    }

    private static void checkEnabled(McpServer mcpServer) {
        if (!mcpServer.isEnabled()) {
            throw new McpServerDisabledException(McpServerDisabledException.MCP_SERVER_MESSAGE);
        }
    }

    private McpServer getMcpServer(@Nullable String mcpServerSecretKey) {
        if (mcpServerSecretKey == null) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

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

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(
        String externalUserId, ConnectedUser connectedUser) {

        if (!connectedUser.isEnabled()) {
            throw new UserNotActivatedException("Connected User " + externalUserId + " was not enabled");
        }

        return new org.springframework.security.core.userdetails.User(connectedUser.getExternalId(), "", List.of());
    }
}
