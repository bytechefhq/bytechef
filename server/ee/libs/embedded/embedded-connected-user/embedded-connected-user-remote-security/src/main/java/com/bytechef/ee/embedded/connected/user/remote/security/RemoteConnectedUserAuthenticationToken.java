/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.security;

import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
public class RemoteConnectedUserAuthenticationToken extends AbstractApiKeyAuthenticationToken
    implements ConnectedUserAuthentication {

    private final boolean apiKeyAuthenticated;
    private final long connectedUserId;
    private final CredentialType credentialType;
    private final String externalUserId;
    private final @Nullable PlatformType platformType;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserAuthenticationToken(
        long environmentId, long connectedUserId, User user, boolean apiKeyAuthenticated,
        CredentialType credentialType, @Nullable PlatformType platformType) {

        super(environmentId, user);

        this.apiKeyAuthenticated = apiKeyAuthenticated;
        this.connectedUserId = connectedUserId;
        this.credentialType = credentialType;
        this.externalUserId = user.getUsername();
        this.platformType = platformType;
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

    @Override
    public CredentialType getCredentialType() {
        return credentialType;
    }

    @Override
    public @Nullable PlatformType getPlatformType() {
        return platformType;
    }
}
