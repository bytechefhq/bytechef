/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerOAuth2AuthenticationToken;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpTrustedIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentity;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.security.web.mcp.oauth2.McpProtectedResourceUrls;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.platform.security.web.mcp.oauth2.MultiIssuerJwtDecoder;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import com.nimbusds.jwt.JWTParser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerOAuth2AuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/embedded/(.+)/mcp");
    private static final GrantedAuthority REQUIRED_SCOPE_AUTHORITY = new SimpleGrantedAuthority(
        "SCOPE_" + McpProtectedResourceUrls.EMBEDDED_SCOPE);

    private final EmbeddedMcpTrustedIssuerResolver embeddedMcpTrustedIssuerResolver;
    private final McpAudienceValidator mcpAudienceValidator = new McpAudienceValidator();
    private final McpJwtIdentityMapper mcpJwtIdentityMapper = new McpJwtIdentityMapper();
    private final McpTenantSecretVerifier mcpTenantSecretVerifier;
    private final MultiIssuerJwtDecoder multiIssuerJwtDecoder;
    private final @Nullable String publicUrl;
    private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpServerOAuth2AuthenticationConverter(
        EmbeddedMcpTrustedIssuerResolver embeddedMcpTrustedIssuerResolver, EnvironmentService environmentService,
        McpJwtDecoderFactory mcpJwtDecoderFactory, McpTenantSecretVerifier mcpTenantSecretVerifier,
        @Nullable String publicUrl) {

        super(environmentService);

        this.embeddedMcpTrustedIssuerResolver = embeddedMcpTrustedIssuerResolver;
        this.multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(mcpJwtDecoderFactory);
        this.mcpTenantSecretVerifier = mcpTenantSecretVerifier;
        this.publicUrl = publicUrl;
    }

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        String authToken = fetchAuthToken(request);

        if (authToken == null || authToken.indexOf('.') < 0) {
            return null;
        }

        String tenantId = extractTenantId(request);

        if (tenantId == null || !mcpTenantSecretVerifier.isVerified(request, tenantId, request.getServletPath())) {
            return null;
        }

        Set<String> trustedIssuerUris = TenantContext.callWithTenantId(
            tenantId, embeddedMcpTrustedIssuerResolver::resolveTrustedIssuerUris);

        String issuer = extractIssuer(authToken);

        if (issuer == null || !trustedIssuerUris.contains(issuer)) {
            return null;
        }

        Jwt jwt = decode(authToken);

        McpTenantIssuer mcpTenantIssuer = TenantContext.callWithTenantId(
            tenantId, () -> embeddedMcpTrustedIssuerResolver.resolveTenantIssuer(issuer));

        if (mcpTenantIssuer == null) {
            throw new BadCredentialsException("Untrusted OAuth2 token issuer");
        }

        if (!mcpAudienceValidator.isTenantAudienceValid(
            jwt, mcpTenantIssuer, McpProtectedResourceUrls.resolveResourceUrl(publicUrl, request))) {

            throw new BadCredentialsException("Invalid OAuth2 token audience");
        }

        if (!scopeAuthoritiesConverter.convert(jwt)
            .contains(REQUIRED_SCOPE_AUTHORITY)) {

            throw new EmbeddedMcpInsufficientScopeException(
                "The token lacks the " + McpProtectedResourceUrls.EMBEDDED_SCOPE + " scope");
        }

        Environment environment = getEnvironment(request);

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.mapTenantIssuer(jwt, mcpTenantIssuer, tenantId);

        return new EmbeddedMcpServerOAuth2AuthenticationToken(
            environment.ordinal(), jwt.getSubject(), tenantId, extractMcpServerSecretKey(request),
            mcpJwtIdentity.authorities());
    }

    @Nullable
    private static String extractMcpServerSecretKey(HttpServletRequest request) {
        Matcher matcher = PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(1);
    }

    private Jwt decode(String authToken) {
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
