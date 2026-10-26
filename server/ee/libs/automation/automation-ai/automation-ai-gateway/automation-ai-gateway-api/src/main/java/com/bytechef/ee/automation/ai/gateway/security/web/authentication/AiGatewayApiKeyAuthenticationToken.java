/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.security.web.authentication;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiGatewayApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    @JsonIgnore
    private String secretKey;

    public AiGatewayApiKeyAuthenticationToken(long environmentId, String secretKey, String tenantId) {
        super(environmentId, tenantId);

        this.secretKey = secretKey;
    }

    public AiGatewayApiKeyAuthenticationToken(long environmentId, User user, Long apiKeyId) {
        super(environmentId, user, apiKeyId);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();

        this.secretKey = null;
    }

    @Override
    public CredentialType getCredentialType() {
        return CredentialType.AUTOMATION_API_KEY;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.AUTOMATION;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
