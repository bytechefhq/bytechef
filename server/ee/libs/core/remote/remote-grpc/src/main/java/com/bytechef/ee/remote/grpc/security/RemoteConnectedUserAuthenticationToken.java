/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.security;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
public class RemoteConnectedUserAuthenticationToken extends AbstractApiKeyAuthenticationToken
    implements ConnectedUserAuthentication {

    private final boolean apiKeyAuthenticated;
    private final long connectedUserId;
    private final String externalUserId;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserAuthenticationToken(
        long environmentId, long connectedUserId, User user, boolean apiKeyAuthenticated) {

        super(environmentId, user);

        this.apiKeyAuthenticated = apiKeyAuthenticated;
        this.connectedUserId = connectedUserId;
        this.externalUserId = user.getUsername();
    }

    @Override
    public boolean apiKeyAuthenticated() {
        return apiKeyAuthenticated;
    }

    @Override
    public long connectedUserId() {
        return connectedUserId;
    }

    @Override
    public long environmentId() {
        return getEnvironmentId();
    }

    @Override
    public String externalUserId() {
        return externalUserId;
    }
}
