/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.filter;

import com.bytechef.ee.embedded.security.web.authentication.ConnectedUserAuthenticationToken;
import com.bytechef.platform.security.web.filter.AbstractPublicApiAuthenticationFilter;
import com.bytechef.platform.user.service.SigningKeyService;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.tenant.util.TenantUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationFilter extends AbstractPublicApiAuthenticationFilter {

    private static final Pattern EXTERNAL_USER_ID_PATTERN = Pattern.compile(".*/v\\d+/([^/]+)/.*");
    private static final Pattern JWT_TOKEN_PATTERN =
        Pattern.compile("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*$");

    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationFilter(
        AuthenticationManager authenticationManager, SigningKeyService signingKeyService) {

        super("^/api/embedded/v[0-9]+/.+|^/api/(?:automation|embedded|platform)/internal/.+", authenticationManager);

        this.signingKeyService = signingKeyService;
    }

    @Override
    protected Authentication getAuthentication(HttpServletRequest request) {
        String token = getAuthToken(request);

        if (token == null) {
            return null;
        }

        Matcher jwtTokenMatcher = JWT_TOKEN_PATTERN.matcher(token);

        if (jwtTokenMatcher.find()) {
            Jws<Claims> jws = getJws(token);

            Claims payload = jws.getPayload();

            String externalUserId = payload.getSubject();

            JwsHeader header = jws.getHeader();

            TenantKey tenantKey = TenantKey.parse(header.getKeyId());

            return new ConnectedUserAuthenticationToken(
                externalUserId, getEnvironment(request), tenantKey.getTenantId());
        } else {
            String externalUserId;
            Matcher matcher = EXTERNAL_USER_ID_PATTERN.matcher(request.getRequestURI());

            if (matcher.matches()) {
                externalUserId = matcher.group(1);
            } else {
                throw new IllegalArgumentException("externalUserId parameter is required");
            }

            TenantKey tenantKey = TenantKey.parse(token);

            return new ConnectedUserAuthenticationToken(
                externalUserId, getEnvironment(request), tenantKey.getTenantId());
        }
    }

    @Override
    protected String getAuthToken(HttpServletRequest request) {
        String token = request.getHeader(AUTH_TOKEN_HEADER_NAME);

        if (token == null) {
            return null;
        }

        return token.replace("Bearer ", "");
    }

    private Jws<Claims> getJws(String secretKey) {
        return Jwts.parser()
            .keyLocator(new SigningKeyLocator())
            .build()
            .parseSignedClaims(secretKey);
    }

    private class SigningKeyLocator implements Locator<Key> {

        @Override
        public Key locate(Header header) {
            String keyId = (String) header.get("kid");

            TenantKey tenantKey = TenantKey.parse(keyId);

            return TenantUtils.callWithTenantId(
                tenantKey.getTenantId(), () -> signingKeyService.getPublicKey(keyId));
        }
    }
}
