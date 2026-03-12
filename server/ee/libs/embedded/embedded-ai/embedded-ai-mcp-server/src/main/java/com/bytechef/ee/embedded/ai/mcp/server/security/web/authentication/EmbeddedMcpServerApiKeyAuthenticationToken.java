/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String externalUserId;

    public EmbeddedMcpServerApiKeyAuthenticationToken(long environmentId, String externalUserId, String tenantId) {
        super(environmentId, tenantId);

        this.externalUserId = externalUserId;
    }

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerApiKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getExternalUserId() {
        return externalUserId;
    }
}
