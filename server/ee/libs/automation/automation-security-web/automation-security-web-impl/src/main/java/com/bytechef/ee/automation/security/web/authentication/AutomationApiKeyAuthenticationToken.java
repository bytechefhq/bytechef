/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.authentication;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AutomationApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String secretKey;

    public AutomationApiKeyAuthenticationToken(long environmentId, String secretKey, String tenantId) {
        super(environmentId, tenantId);

        this.secretKey = secretKey;
    }

    public AutomationApiKeyAuthenticationToken(long environmentId, User user, @Nullable Long apiKeyId) {
        super(environmentId, user, apiKeyId);
    }

    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.AUTOMATION_API_KEY;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.AUTOMATION;
    }
}
