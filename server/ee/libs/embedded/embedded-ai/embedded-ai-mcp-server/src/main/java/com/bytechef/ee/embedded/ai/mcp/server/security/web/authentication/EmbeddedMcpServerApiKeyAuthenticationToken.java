/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String externalUserId;
    private String mcpServerSecretKey;

    public EmbeddedMcpServerApiKeyAuthenticationToken(
        long environmentId, String externalUserId, String tenantId, String mcpServerSecretKey) {

        super(environmentId, tenantId);

        this.externalUserId = externalUserId;
        this.mcpServerSecretKey = mcpServerSecretKey;
    }

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerApiKeyAuthenticationToken(User user) {
        super(user);
    }

    /**
     * The authenticated form. Carries the environment forward: this token lands in the {@code SecurityContext}, and
     * ticket 1051's {@code ConnectedUserResourceMembershipResolver} decides whether a principal is a connected user --
     * and in which environment -- from exactly that. Built with the {@code User}-only constructor above it would report
     * ordinal 0 (DEVELOPMENT) for every caller.
     */
    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerApiKeyAuthenticationToken(long environmentId, User user) {
        super(environmentId, user);
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }
}
