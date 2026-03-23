/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    private final SigningKeyService signingKeyService;

    EmbeddedMcpServerApiKeyAuthenticationConverter(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        String authToken = getAuthToken(request);

        Environment environment = getEnvironment(request);

        Jws<Claims> jws = getJws(authToken, environment.ordinal());

        Claims payload = jws.getPayload();

        String externalUserId = payload.getSubject();

        JwsHeader header = jws.getHeader();

        TenantKey tenantKey = TenantKey.parse(header.getKeyId());

        return new EmbeddedMcpServerApiKeyAuthenticationToken(
            environment.ordinal(), externalUserId, tenantKey.getTenantId());
    }

    private Jws<Claims> getJws(String secretKey, long environmentId) {
        return Jwts.parser()
            .keyLocator(new SigningKeyLocator(environmentId, signingKeyService))
            .build()
            .parseSignedClaims(secretKey);
    }

    private record SigningKeyLocator(long environmentId, SigningKeyService signingKeyService)
        implements Locator<Key> {

        @Override
        public Key locate(Header header) {
            String keyId = (String) header.get("kid");

            TenantKey tenantKey = TenantKey.parse(keyId);

            return TenantContext.callWithTenantId(
                tenantKey.getTenantId(), () -> signingKeyService.getPublicKey(keyId, environmentId));
        }
    }
}
