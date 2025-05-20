/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.web.authentication.AbstractPublicApiAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationToken extends AbstractPublicApiAuthenticationToken {

    private Environment environment;
    private String externalUserId;

    public ConnectedUserAuthenticationToken(String externalUserId, Environment environment, String tenantId) {
        super(tenantId);

        this.environment = environment;
        this.externalUserId = externalUserId;
    }

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationToken(User user) {
        super(user);
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getExternalUserId() {
        return externalUserId;
    }
}
