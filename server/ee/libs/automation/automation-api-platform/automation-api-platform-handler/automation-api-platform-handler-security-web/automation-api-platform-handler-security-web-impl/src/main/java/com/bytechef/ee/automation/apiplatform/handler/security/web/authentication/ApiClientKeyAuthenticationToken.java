/*
 * Copyright 2023-present ByteChef Inc.
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
public class ApiClientKeyAuthenticationToken extends AbstractPublicApiAuthenticationToken {

    private String secretKey;

    public ApiClientKeyAuthenticationToken(String secretKey, String tenantId) {
        super(tenantId);

        this.secretKey = secretKey;
    }

    @SuppressFBWarnings("EI")
    public ApiClientKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getSecretKey() {
        return secretKey;
    }
}
