/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpFederatedIssuerAuthenticator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentity;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.security.web.mcp.oauth2.McpProtectedResourceUrls;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantTrustContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class TenantIdpFederatedIssuerAuthenticator implements McpFederatedIssuerAuthenticator {

    private final McpAudienceValidator mcpAudienceValidator;
    private final McpJwtIdentityMapper mcpJwtIdentityMapper;
    private final @Nullable String publicUrl;

    @SuppressFBWarnings("EI2")
    public TenantIdpFederatedIssuerAuthenticator(
        McpAudienceValidator mcpAudienceValidator, McpJwtIdentityMapper mcpJwtIdentityMapper,
        @Nullable String publicUrl) {

        this.mcpAudienceValidator = mcpAudienceValidator;
        this.mcpJwtIdentityMapper = mcpJwtIdentityMapper;
        this.publicUrl = publicUrl;
    }

    @Override
    public Optional<McpJwtIdentity> authenticate(Jwt jwt, HttpServletRequest request, String urlTenantId) {
        String issuerUri = String.valueOf(jwt.getIssuer());

        McpTenantIssuer tenantIssuer = McpTenantTrustContext.getIssuer(issuerUri);

        if (tenantIssuer == null) {
            return Optional.empty();
        }

        if (!mcpAudienceValidator.isTenantAudienceValid(
            jwt, tenantIssuer, McpProtectedResourceUrls.resolveResourceUrl(publicUrl, request))) {

            return Optional.empty();
        }

        return Optional.of(mcpJwtIdentityMapper.mapTenantIssuer(jwt, tenantIssuer, urlTenantId));
    }
}
