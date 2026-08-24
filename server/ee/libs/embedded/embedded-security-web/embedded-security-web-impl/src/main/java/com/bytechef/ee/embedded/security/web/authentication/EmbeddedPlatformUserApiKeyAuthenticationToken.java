/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedPlatformUserApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String secretKey;

    public EmbeddedPlatformUserApiKeyAuthenticationToken(int environment, String secretKey, String tenantId) {
        super(environment, tenantId);

        this.secretKey = secretKey;
    }

    public EmbeddedPlatformUserApiKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getSecretKey() {
        return secretKey;
    }
}
