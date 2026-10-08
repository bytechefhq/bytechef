/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
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
    private @Nullable String externalUserId;
    private @Nullable String mcpServerSecretKey;

    public EmbeddedMcpServerApiKeyAuthenticationToken(
        long environmentId, @Nullable String externalUserId, String tenantId, String mcpServerSecretKey) {

        super(environmentId, tenantId);

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
}
