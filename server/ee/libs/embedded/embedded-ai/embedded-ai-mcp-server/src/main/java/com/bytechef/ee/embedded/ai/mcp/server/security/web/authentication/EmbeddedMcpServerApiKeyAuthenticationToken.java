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
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken
    implements ConnectedUserAuthentication {

    private long connectedUserId;
    private String externalUserId;

    public EmbeddedMcpServerApiKeyAuthenticationToken(long environmentId, String externalUserId, String tenantId) {
        super(environmentId, tenantId);

        this.externalUserId = externalUserId;
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

    public String getExternalUserId() {
        return externalUserId;
    }
}
