/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/**
 * Manages short-lived tokens for MCP connection setup links. When a tool requires authentication, a token is generated
 * and embedded in a setup URL. The connected user can visit this URL to authenticate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectTokenService {

    private static final long TOKEN_TTL_MILLIS = 5 * 60 * 1000;

    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();

    public String generateToken(
        long mcpServerId, String componentName, String externalUserId, String tenantId) {

        String token = UUID.randomUUID()
            .toString();

        tokenStore.put(
            token,
            new TokenEntry(
                new ConnectTokenData(mcpServerId, componentName, externalUserId, tenantId),
                System.currentTimeMillis() + TOKEN_TTL_MILLIS));

        return token;
    }

    public @Nullable ConnectTokenData resolveToken(String token) {
        TokenEntry tokenEntry = tokenStore.get(token);

        if (tokenEntry == null) {
            return null;
        }

        if (System.currentTimeMillis() > tokenEntry.expiresAt()) {
            tokenStore.remove(token);

            return null;
        }

        return tokenEntry.connectTokenData();
    }

    public void removeToken(String token) {
        tokenStore.remove(token);
    }

    public record ConnectTokenData(long mcpServerId, String componentName, String externalUserId, String tenantId) {
    }

    private record TokenEntry(ConnectTokenData connectTokenData, long expiresAt) {
    }
}
