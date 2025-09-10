/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractPublicApiAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformKeyAuthenticationToken extends AbstractPublicApiAuthenticationToken {

    private String secretKey;

    public ApiPlatformKeyAuthenticationToken(long environmentId, String secretKey, String tenantId) {
        super(environmentId, tenantId);

        this.secretKey = secretKey;
    }

    @SuppressFBWarnings("EI")
    public ApiPlatformKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getSecretKey() {
        return secretKey;
    }
}
