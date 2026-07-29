/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerOAuth2AuthenticationToken;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpTrustedIssuerResolver;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.platform.security.web.mcp.oauth2.MultiIssuerJwtDecoder;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import com.nimbusds.jwt.JWTParser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.util.UrlUtils;

/**
 * Converts an OAuth2 Bearer JWT on the embedded MCP endpoint into an unauthenticated
 * {@link EmbeddedMcpServerOAuth2AuthenticationToken}. The tenant comes from the endpoint's path secret (which is a
 * {@link TenantKey}), the environment from the {@code X-Environment} header, and the user from the token subject.
 *
 * <p>
 * Because the signing-key embedded token is also a JWT, this converter claims a token only when its (unverified)
 * {@code iss} is trusted for the path-secret tenant; otherwise it returns {@code null} so the request falls through to
 * the signing-key converter. A token that claims a trusted issuer but fails validation is rejected.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerOAuth2AuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/embedded/(.+)/mcp");

    private final EmbeddedMcpTrustedIssuerResolver embeddedMcpTrustedIssuerResolver;
    private final McpJwtDecoderFactory mcpJwtDecoderFactory;
    private final McpJwtIdentityMapper mcpJwtIdentityMapper = new McpJwtIdentityMapper();

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpServerOAuth2AuthenticationConverter(
        EmbeddedMcpTrustedIssuerResolver embeddedMcpTrustedIssuerResolver, McpJwtDecoderFactory mcpJwtDecoderFactory) {

        this.embeddedMcpTrustedIssuerResolver = embeddedMcpTrustedIssuerResolver;
        this.mcpJwtDecoderFactory = mcpJwtDecoderFactory;
    }

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        String authToken = fetchAuthToken(request);

        if (authToken == null || authToken.indexOf('.') < 0) {
            return null;
        }

        String tenantId = extractTenantId(request);

        if (tenantId == null) {
            return null;
        }

        Set<String> trustedIssuerUris = TenantContext.callWithTenantId(
            tenantId, embeddedMcpTrustedIssuerResolver::resolveTrustedIssuerUris);

        String issuer = extractIssuer(authToken);

        if (issuer == null || !trustedIssuerUris.contains(issuer)) {
            return null;
        }

        Jwt jwt = decode(authToken, trustedIssuerUris);

        boolean audienceValidationRequired = TenantContext.callWithTenantId(
            tenantId, () -> embeddedMcpTrustedIssuerResolver.isAudienceValidationRequired(issuer));

        if (audienceValidationRequired && !audienceContainsEndpoint(jwt, request)) {
            throw new BadCredentialsException("Invalid OAuth2 token audience");
        }

        Environment environment = getEnvironment(request);

        List<GrantedAuthority> mappedAuthorities = mapAuthorities(jwt, issuer, tenantId);

        return new EmbeddedMcpServerOAuth2AuthenticationToken(
            environment.ordinal(), jwt.getSubject(), tenantId, extractMcpServerSecretKey(request), mappedAuthorities);
    }

    @Nullable
    private static String extractMcpServerSecretKey(HttpServletRequest request) {
        Matcher matcher = PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(1);
    }

    /**
     * Maps the token's group claim to ByteChef authorities through the tenant's identity provider (its configured group
     * claim and group-to-authority map), reusing the same mapping the automation/management resource server applies.
     * These become the connected user's authorities and gate per-component tool authorization on the embedded endpoint.
     */
    private List<GrantedAuthority> mapAuthorities(Jwt jwt, String issuer, String tenantId) {
        McpTenantIssuer mcpTenantIssuer = TenantContext.callWithTenantId(
            tenantId, () -> embeddedMcpTrustedIssuerResolver.resolveTenantIssuer(issuer));

        if (mcpTenantIssuer == null) {
            return List.of();
        }

        return mcpJwtIdentityMapper.mapTenantIssuer(jwt, mcpTenantIssuer, tenantId)
            .authorities();
    }

    private static boolean audienceContainsEndpoint(Jwt jwt, HttpServletRequest request) {
        List<String> audiences = jwt.getAudience();

        if (audiences == null) {
            return false;
        }

        String endpointUrl = stripTrailingSlash(buildRequestUrl(request));

        return audiences.stream()
            .map(EmbeddedMcpServerOAuth2AuthenticationConverter::stripTrailingSlash)
            .anyMatch(endpointUrl::equals);
    }

    private static String buildRequestUrl(HttpServletRequest request) {
        String url = UrlUtils.buildFullRequestUrl(request);

        int queryIndex = url.indexOf('?');

        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private Jwt decode(String authToken, Set<String> trustedIssuerUris) {
        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(
            issuer -> trustedIssuerUris.contains(issuer) ? mcpJwtDecoderFactory.createJwtDecoder(issuer) : null);

        try {
            return multiIssuerJwtDecoder.decode(authToken);
        } catch (JwtException jwtException) {
            throw new BadCredentialsException("Invalid OAuth2 token", jwtException);
        }
    }

    @Nullable
    private static String extractTenantId(HttpServletRequest request) {
        Matcher matcher = PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return null;
        }

        try {
            TenantKey tenantKey = TenantKey.parse(matcher.group(1));

            return tenantKey.getTenantId();
        } catch (Exception exception) {
            return null;
        }
    }

    @Nullable
    private static String extractIssuer(String authToken) {
        try {
            return JWTParser.parse(authToken)
                .getJWTClaimsSet()
                .getIssuer();
        } catch (ParseException parseException) {
            return null;
        }
    }
}
