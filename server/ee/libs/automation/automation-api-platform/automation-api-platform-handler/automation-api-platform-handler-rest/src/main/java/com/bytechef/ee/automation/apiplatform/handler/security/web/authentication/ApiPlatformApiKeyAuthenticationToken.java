/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.authentication;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String secretKey;

    public ApiPlatformApiKeyAuthenticationToken(long environmentId, String secretKey, String tenantId) {
        super(environmentId, tenantId);

        this.secretKey = secretKey;
    }

    @SuppressFBWarnings("EI")
    public ApiPlatformApiKeyAuthenticationToken(long environmentId, User user, Long apiKeyId) {
        super(environmentId, user, apiKeyId);
    }

    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.API_PLATFORM_API_KEY;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.AUTOMATION;
    }
}
