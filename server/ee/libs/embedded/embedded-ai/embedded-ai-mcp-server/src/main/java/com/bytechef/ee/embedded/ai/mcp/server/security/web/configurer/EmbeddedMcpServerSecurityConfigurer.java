/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerOAuth2AuthenticationProvider;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerOAuth2AuthenticationToken;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpTrustedIssuerResolver;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.ApiKeyAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpAuthenticationGuardFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpDiscoveryAuthenticationFilter;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataAuthenticationEntryPoint;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerSecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/embedded/.+/mcp";
    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("^/api/embedded/(.+)/mcp");

    private final EmbeddedMcpServerOAuth2AuthenticationConverter embeddedMcpServerOAuth2AuthenticationConverter;
    private final EmbeddedMcpServerOAuth2AuthenticationProvider embeddedMcpServerOAuth2AuthenticationProvider;
    private final McpServerService mcpServerService;
    private final McpTenantProtectedResourceMetadataAuthenticationEntryPoint protectedResourceMetadataEntryPoint;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpServerSecurityConfigurer(
        ConnectedUserService connectedUserService, EnvironmentService environmentService,
        McpServerService mcpServerService, SigningKeyService signingKeyService,
        McpTenantIssuerResolver mcpTenantIssuerResolver, McpTenantSecretVerifier mcpTenantSecretVerifier,
        McpJwtDecoderFactory mcpJwtDecoderFactory, @Nullable String publicUrl) {

        super(
            PATH_PATTERN,
            new EmbeddedMcpServerApiKeyAuthenticationConverter(environmentService, signingKeyService),
            new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService, mcpServerService));

        this.embeddedMcpServerOAuth2AuthenticationConverter = new EmbeddedMcpServerOAuth2AuthenticationConverter(
            new EmbeddedMcpTrustedIssuerResolver(mcpTenantIssuerResolver), environmentService, mcpJwtDecoderFactory,
            mcpTenantSecretVerifier, publicUrl);
        this.embeddedMcpServerOAuth2AuthenticationProvider =
            new EmbeddedMcpServerOAuth2AuthenticationProvider(connectedUserService, mcpServerService);
        this.mcpServerService = mcpServerService;
        this.protectedResourceMetadataEntryPoint =
            new McpTenantProtectedResourceMetadataAuthenticationEntryPoint(publicUrl);
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
            regexMatcher(PATH_PATTERN), embeddedMcpServerOAuth2AuthenticationConverter, authenticationManager,
            this::onOAuth2AuthenticationFailure);

        http.addFilterBefore(oAuth2AuthenticationFilter, BasicAuthenticationFilter.class);

        McpDiscoveryAuthenticationFilter mcpDiscoveryAuthenticationFilter = new McpDiscoveryAuthenticationFilter(
            regexMatcher(PATH_PATTERN), protectedResourceMetadataEntryPoint, this::isAuthenticationRequired);

        http.addFilterBefore(mcpDiscoveryAuthenticationFilter, SecurityContextHolderFilter.class);

        super.configure(http);

        McpAuthenticationGuardFilter mcpAuthenticationGuardFilter = new McpAuthenticationGuardFilter(
            regexMatcher(PATH_PATTERN),
            List.of(
                EmbeddedMcpServerApiKeyAuthenticationToken.class, EmbeddedMcpServerOAuth2AuthenticationToken.class,
                McpAnonymousAuthenticationToken.class),
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

        http.addFilterBefore(mcpAuthenticationGuardFilter, AuthorizationFilter.class);
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    private boolean isAuthenticationRequired(HttpServletRequest request) {
        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return true;
        }

        String mcpServerSecretKey = matcher.group(1);

        try {
            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            McpServer mcpServer = TenantContext.callWithTenantId(
                tenantKey.getTenantId(), () -> mcpServerService.getMcpServer(mcpServerSecretKey));

            return mcpServer.isAuthenticationRequired();
        } catch (RuntimeException runtimeException) {
            return true;
        }
    }

    private void onOAuth2AuthenticationFailure(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) {

        if (authenticationException instanceof EmbeddedMcpInsufficientScopeException) {
            protectedResourceMetadataEntryPoint.sendChallenge(
                request, response, HttpServletResponse.SC_FORBIDDEN,
                McpTenantProtectedResourceMetadataAuthenticationEntryPoint.INSUFFICIENT_SCOPE);

            return;
        }

        protectedResourceMetadataEntryPoint.sendChallenge(
            request, response, HttpServletResponse.SC_UNAUTHORIZED,
            McpTenantProtectedResourceMetadataAuthenticationEntryPoint.INVALID_TOKEN);
    }
}
