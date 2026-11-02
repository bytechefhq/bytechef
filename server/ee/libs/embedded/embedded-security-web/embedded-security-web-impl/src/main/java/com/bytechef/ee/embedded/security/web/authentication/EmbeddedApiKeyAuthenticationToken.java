/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

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
public class EmbeddedApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken
    implements ConnectedUserAuthentication {

    private boolean apiKeyAuthenticated;
    private long connectedUserId;
    private String externalUserId;
    private String secretKey;

    public EmbeddedApiKeyAuthenticationToken(
        long environmentId, String externalUserId, String secretKey, String tenantId) {

        super(environmentId, tenantId);

        this.externalUserId = externalUserId;
        this.secretKey = secretKey;
    }

    public EmbeddedApiKeyAuthenticationToken(
        long environmentId, long connectedUserId, User user, boolean apiKeyAuthenticated) {

        this(environmentId, connectedUserId, user, apiKeyAuthenticated, null);
    }

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeyAuthenticationToken(
        long environmentId, long connectedUserId, User user, boolean apiKeyAuthenticated, @Nullable Long apiKeyId) {

        super(environmentId, user, apiKeyId);

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

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.EMBEDDED_API_KEY;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.EMBEDDED;
    }
}
