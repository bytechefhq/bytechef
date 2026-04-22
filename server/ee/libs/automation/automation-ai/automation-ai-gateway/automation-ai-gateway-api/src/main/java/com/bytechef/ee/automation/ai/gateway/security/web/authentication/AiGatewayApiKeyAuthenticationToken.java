/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
public class AiGatewayApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    @JsonIgnore
    private String secretKey;

    private Long apiKeyId;

    public AiGatewayApiKeyAuthenticationToken(long environmentId, String secretKey, String tenantId) {
        super(environmentId, tenantId);

        this.secretKey = secretKey;
    }

    public AiGatewayApiKeyAuthenticationToken(User user) {
        super(user);
    }

    public AiGatewayApiKeyAuthenticationToken(User user, Long apiKeyId) {
        super(user);

        this.apiKeyId = apiKeyId;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();

        this.secretKey = null;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public String getSecretKey() {
        return secretKey;
    }

}
