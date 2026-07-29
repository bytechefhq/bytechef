/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpProtectedResourceMetadataAuthenticationEntryPoint;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerOAuth2AuthenticationProvider;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpTrustedIssuerResolver;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.ApiKeyAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpDiscoveryAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Secures the embedded MCP endpoint. The base configurer registers the signing-key credential (the ByteChef-issued
 * embedded JWT); this configurer additionally installs an OAuth2 credential ahead of it, so a Bearer JWT from a trusted
 * issuer (a per-tenant external IdP, and later the embedded authorization server) is accepted, while a signing-key
 * token falls through to the base filter.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerSecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/embedded/.+/mcp";

    private static final Pattern SECRET_KEY_PATH_PATTERN =
        Pattern.compile("^/api/embedded/(.+)/(mcp|sse|message)");

    private final EmbeddedMcpServerOAuth2AuthenticationConverter embeddedMcpServerOAuth2AuthenticationConverter;
    private final EmbeddedMcpServerOAuth2AuthenticationProvider embeddedMcpServerOAuth2AuthenticationProvider;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpServerSecurityConfigurer(
        ConnectedUserService connectedUserService, McpServerService mcpServerService,
        SigningKeyService signingKeyService, McpTenantIssuerResolver mcpTenantIssuerResolver,
        McpJwtDecoderFactory mcpJwtDecoderFactory, @Nullable String embeddedAuthorizationServerIssuerUri) {

        super(
            PATH_PATTERN,
            new EmbeddedMcpServerApiKeyAuthenticationConverter(signingKeyService),
            new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService, mcpServerService));

        this.embeddedMcpServerOAuth2AuthenticationConverter = new EmbeddedMcpServerOAuth2AuthenticationConverter(
            new EmbeddedMcpTrustedIssuerResolver(mcpTenantIssuerResolver, embeddedAuthorizationServerIssuerUri),
            mcpJwtDecoderFactory);
        this.embeddedMcpServerOAuth2AuthenticationProvider =
            new EmbeddedMcpServerOAuth2AuthenticationProvider(connectedUserService, mcpServerService);
        this.mcpServerService = mcpServerService;
    }

    @Override
    public void init(HttpSecurity http) {
        super.init(http);

        http.authenticationProvider(embeddedMcpServerOAuth2AuthenticationProvider);
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

        ApiKeyAuthenticationFilter oAuth2AuthenticationFilter = new EmbeddedMcpServerOAuth2ApiKeyAuthenticationFilter(
            regexMatcher(PATH_PATTERN), embeddedMcpServerOAuth2AuthenticationConverter, authenticationManager);

        // Added before the signing-key filter (super.configure) so the OAuth2 filter runs first; a signing-key token
        // yields a null OAuth2 conversion and falls through to the base filter.
        http.addFilterBefore(oAuth2AuthenticationFilter, BasicAuthenticationFilter.class);

        // Answers an unauthenticated MCP request (no Authorization header) with the RFC 9728 discovery challenge (401 +
        // WWW-Authenticate pointing at this endpoint's protected-resource metadata) instead of a bare 401, so a generic
        // MCP client can discover the tenant's identity provider. Requests carrying a credential pass through
        // unchanged. A token-less request to a server that does not require authentication is not challenged - it falls
        // through to the credential filters, which serve it anonymously.
        McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter = new McpDiscoveryAuthenticationFilter(
            regexMatcher(PATH_PATTERN), new EmbeddedMcpProtectedResourceMetadataAuthenticationEntryPoint(),
            this::isAuthenticationRequired);

        http.addFilterBefore(mcpDiscoveryAuthenticationFilter, SecurityContextHolderFilter.class);

        super.configure(http);
    }

    /**
     * Whether the MCP server addressed by the request's path secret requires authentication. Runs before the tenant
     * context is established for the request, so it derives the tenant from the path secret (a {@link TenantKey}) to
     * resolve the server. An unresolvable path secret or server defaults to requiring authentication, so the discovery
     * challenge is only suppressed for a server that positively opts out.
     */
    private boolean isAuthenticationRequired(HttpServletRequest request) {
        String mcpServerSecretKey = extractMcpServerSecretKey(request);

        if (mcpServerSecretKey == null) {
            return true;
        }

        try {
            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            McpServer mcpServer = TenantContext.callWithTenantId(
                tenantKey.getTenantId(), () -> mcpServerService.getMcpServer(mcpServerSecretKey));

            return mcpServer.isAuthenticationRequired();
        } catch (Exception exception) {
            return true;
        }
    }

    @Nullable
    private static String extractMcpServerSecretKey(HttpServletRequest request) {
        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(1);
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }
}
