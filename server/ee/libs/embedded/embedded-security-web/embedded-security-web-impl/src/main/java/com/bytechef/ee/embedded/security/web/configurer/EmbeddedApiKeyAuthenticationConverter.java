/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;

/**
 * Authentication converter for embedded API key authentication.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    static final Pattern EXTERNAL_USER_ID_PATTERN = Pattern.compile(".*/v\\d+/([^/]+)/.*");
    static final Pattern JWT_TOKEN_PATTERN =
        Pattern.compile("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*$");

    private final SigningKeyService signingKeyService;

    EmbeddedApiKeyAuthenticationConverter(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        String authToken = fetchAuthToken(request);

        if (authToken == null || authToken.isBlank()) {
            return null;
        }

        Environment environment = getEnvironment(request);
        Matcher jwtTokenMatcher = JWT_TOKEN_PATTERN.matcher(authToken);

        if (jwtTokenMatcher.find()) {
            Jws<Claims> jws = getJws(authToken, environment.ordinal());

            Claims payload = jws.getPayload();

            String externalUserId = payload.getSubject();

            JwsHeader header = jws.getHeader();

            TenantKey tenantKey = TenantKey.parse(header.getKeyId());

            return new EmbeddedApiKeyAuthenticationToken(
                environment.ordinal(), externalUserId, tenantKey.getTenantId());
        } else {
            String externalUserId;
            Matcher matcher = EXTERNAL_USER_ID_PATTERN.matcher(request.getRequestURI());

            if (matcher.matches()) {
                externalUserId = matcher.group(1);
            } else {
                throw new IllegalArgumentException("externalUserId parameter is required");
            }

            TenantKey tenantKey = TenantKey.parse(authToken);

            return new EmbeddedApiKeyAuthenticationToken(
                environment.ordinal(), externalUserId, tenantKey.getTenantId());
        }
    }

    private Jws<Claims> getJws(String secretKey, long environmentId) {
        return Jwts.parser()
            .keyLocator(new SigningKeyLocator(environmentId, signingKeyService))
            .build()
            .parseSignedClaims(secretKey);
    }

    private record SigningKeyLocator(long environmentId, SigningKeyService signingKeyService) implements Locator<Key> {

        @Override
        public Key locate(Header header) {
            String keyId = (String) header.get("kid");

            TenantKey tenantKey = TenantKey.parse(keyId);

            return TenantContext.callWithTenantId(
                tenantKey.getTenantId(), () -> signingKeyService.getPublicKey(keyId, environmentId));
        }
    }
}
