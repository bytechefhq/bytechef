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
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantTrustContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.util.UrlUtils;

/**
 * The enterprise implementation of {@link McpFederatedIssuerAuthenticator}: the relaxed path for a per-tenant external
 * identity provider trusted only for this request's tenant ({@link McpTenantTrustContext}). The endpoint surface is
 * authorized by the provider's surface flag rather than a token scope, so no scope is required; audience binding is
 * optional (the single-tenant trust prevents cross-tenant replay) unless the provider opts in; and authorities come
 * from the provider's group-to-authority map. Returns empty when the issuer is not trusted for this tenant or audience
 * validation fails, causing the filter to respond 401.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TenantIdpFederatedIssuerAuthenticator implements McpFederatedIssuerAuthenticator {

    private final McpAudienceValidator mcpAudienceValidator;
    private final McpJwtIdentityMapper mcpJwtIdentityMapper;

    @SuppressFBWarnings("EI2")
    public TenantIdpFederatedIssuerAuthenticator(
        McpAudienceValidator mcpAudienceValidator, McpJwtIdentityMapper mcpJwtIdentityMapper) {

        this.mcpAudienceValidator = mcpAudienceValidator;
        this.mcpJwtIdentityMapper = mcpJwtIdentityMapper;
    }

    @Override
    public Optional<McpJwtIdentity> authenticate(Jwt jwt, HttpServletRequest request, String urlTenantId) {
        String issuerUri = String.valueOf(jwt.getIssuer());

        McpTenantIssuer tenantIssuer = McpTenantTrustContext.getIssuer(issuerUri);

        if (tenantIssuer == null) {
            return Optional.empty();
        }

        if (!mcpAudienceValidator.isTenantAudienceValid(jwt, tenantIssuer, buildRequestUrl(request))) {
            return Optional.empty();
        }

        return Optional.of(mcpJwtIdentityMapper.mapTenantIssuer(jwt, tenantIssuer, urlTenantId));
    }

    private static String buildRequestUrl(HttpServletRequest request) {
        String url = UrlUtils.buildFullRequestUrl(request);

        int queryIndex = url.indexOf('?');

        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }
}
