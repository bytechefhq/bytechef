/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.security;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
public class RemoteApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private final CredentialType credentialType;
    private final @Nullable PlatformType platformType;

    @SuppressFBWarnings("EI")
    public RemoteApiKeyAuthenticationToken(
        long environmentId, User user, CredentialType credentialType, @Nullable PlatformType platformType) {

        super(environmentId, user);

        this.credentialType = credentialType;
        this.platformType = platformType;
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
