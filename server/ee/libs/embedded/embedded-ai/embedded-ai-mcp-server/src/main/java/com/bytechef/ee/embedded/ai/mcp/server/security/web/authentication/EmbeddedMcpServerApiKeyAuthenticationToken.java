/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private final String authSecretKey;
    private String externalUserId;
    private String mcpServerSecretKey;

    public EmbeddedMcpServerApiKeyAuthenticationToken(String authSecretKey) {
        setAuthenticated(true);

        this.authSecretKey = authSecretKey;
    }

    public EmbeddedMcpServerApiKeyAuthenticationToken(
        String mcpServerSecretKey, String externalUserId, String authSecretKey, String tenantId) {

        super(-1, tenantId);

        this.authSecretKey = authSecretKey;
        this.externalUserId = externalUserId;
        this.mcpServerSecretKey = mcpServerSecretKey;
    }

    public String getAuthSecretKey() {
        return authSecretKey;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }

    @Override
    @SuppressFBWarnings("EI")
    public Object getPrincipal() {
        return authSecretKey;
    }
}
