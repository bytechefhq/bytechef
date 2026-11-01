/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken
    implements ConnectedUserAuthentication {

    private long connectedUserId;
    private boolean environmentRequested;
    private @Nullable String externalUserId;
    private @Nullable String mcpServerSecretKey;

    public EmbeddedMcpServerApiKeyAuthenticationToken(
        long environmentId, @Nullable String externalUserId, String tenantId, String mcpServerSecretKey) {

        this(environmentId, false, externalUserId, tenantId, mcpServerSecretKey);
    }

    public EmbeddedMcpServerApiKeyAuthenticationToken(
        long environmentId, boolean environmentRequested, @Nullable String externalUserId, String tenantId,
        String mcpServerSecretKey) {

        super(environmentId, tenantId);

        this.environmentRequested = environmentRequested;
        this.externalUserId = externalUserId;
        this.mcpServerSecretKey = mcpServerSecretKey;
    }

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerApiKeyAuthenticationToken(long environmentId, long connectedUserId, User user) {
        super(environmentId, user);

        this.connectedUserId = connectedUserId;
        this.externalUserId = user.getUsername();
    }

    @Override
    public long connectedUserId() {
        return connectedUserId;
    }

    @Override
    public @Nullable Long getConnectedUserId() {
        return isAuthenticated() ? connectedUserId : null;
    }

    @Override
    public String externalUserId() {
        return externalUserId;
    }

    @Override
    public long environmentId() {
        return getEnvironmentId();
    }

    public @Nullable String getExternalUserId() {
        return externalUserId;
    }

    public @Nullable String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }

    public boolean isEnvironmentRequested() {
        return environmentRequested;
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.EMBEDDED_MCP_SERVER_API_KEY;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.EMBEDDED;
    }
}
