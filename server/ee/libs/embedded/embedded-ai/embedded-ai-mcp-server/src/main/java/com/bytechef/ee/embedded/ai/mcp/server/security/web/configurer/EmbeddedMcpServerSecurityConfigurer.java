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
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.ApiKeyAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpDiscoveryAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
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

    private final EmbeddedMcpServerOAuth2AuthenticationConverter embeddedMcpServerOAuth2AuthenticationConverter;
    private final EmbeddedMcpServerOAuth2AuthenticationProvider embeddedMcpServerOAuth2AuthenticationProvider;

    public EmbeddedMcpServerSecurityConfigurer(
        ConnectedUserService connectedUserService, SigningKeyService signingKeyService,
        McpTenantIssuerResolver mcpTenantIssuerResolver, McpJwtDecoderFactory mcpJwtDecoderFactory,
        @Nullable String embeddedAuthorizationServerIssuerUri) {

        super(
            PATH_PATTERN,
            new EmbeddedMcpServerApiKeyAuthenticationConverter(signingKeyService),
            new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService));

        this.embeddedMcpServerOAuth2AuthenticationConverter = new EmbeddedMcpServerOAuth2AuthenticationConverter(
            new EmbeddedMcpTrustedIssuerResolver(mcpTenantIssuerResolver, embeddedAuthorizationServerIssuerUri),
            mcpJwtDecoderFactory);
        this.embeddedMcpServerOAuth2AuthenticationProvider =
            new EmbeddedMcpServerOAuth2AuthenticationProvider(connectedUserService);
    }

    @Override
    public void init(HttpSecurity http) {
        super.init(http);

        http.authenticationProvider(embeddedMcpServerOAuth2AuthenticationProvider);
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

        ApiKeyAuthenticationFilter oAuth2AuthenticationFilter = new ApiKeyAuthenticationFilter(
            regexMatcher(PATH_PATTERN), embeddedMcpServerOAuth2AuthenticationConverter, authenticationManager);

        // Added before the signing-key filter (super.configure) so the OAuth2 filter runs first; a signing-key token
        // yields a null OAuth2 conversion and falls through to the base filter.
        http.addFilterBefore(oAuth2AuthenticationFilter, BasicAuthenticationFilter.class);

        // Answers an unauthenticated MCP request (no Authorization header) with the RFC 9728 discovery challenge (401 +
        // WWW-Authenticate pointing at this endpoint's protected-resource metadata) instead of a bare 401, so a generic
        // MCP client can discover the tenant's identity provider. Requests carrying a credential pass through
        // unchanged.
        McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter = new McpDiscoveryAuthenticationFilter(
            regexMatcher(PATH_PATTERN), new EmbeddedMcpProtectedResourceMetadataAuthenticationEntryPoint());

        http.addFilterBefore(mcpDiscoveryAuthenticationFilter, SecurityContextHolderFilter.class);

        super.configure(http);
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }
}
