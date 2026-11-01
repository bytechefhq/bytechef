/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TestEmbeddedApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private final @Nullable Long connectedUserId;

    public TestEmbeddedApiKeyAuthenticationToken(
        long environmentId, String login, @Nullable Long apiKeyId, @Nullable Long connectedUserId) {

        super(environmentId, new User(login, "", List.of()), apiKeyId);

        this.connectedUserId = connectedUserId;
    }

    @Override
    public @Nullable Long getConnectedUserId() {
        return connectedUserId;
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
