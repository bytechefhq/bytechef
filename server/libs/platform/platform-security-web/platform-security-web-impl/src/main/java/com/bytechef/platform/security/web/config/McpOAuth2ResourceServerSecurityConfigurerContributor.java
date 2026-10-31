/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.web.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.platform.security.web.mcp.oauth2.IssuerLocationMcpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpBearerTokenResolver;
import com.bytechef.platform.security.web.mcp.oauth2.McpEndpointRequestMatcher;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpProtectedResourceUrls;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataAuthenticationEntryPoint;
import com.bytechef.platform.security.web.mcp.oauth2.MultiIssuerJwtDecoder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(McpResourceServerProperties.class)
public class McpOAuth2ResourceServerSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private static final String RESOURCE_NAME = "ByteChef";
    private static final String RESOURCE_PATH = "/api";

    private final McpJwtDecoderFactory mcpJwtDecoderFactory;
    private final ObjectProvider<McpEndpoint> mcpEndpointProvider;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final @Nullable String publicUrl;
    private @Nullable AuthenticationEventPublisher authenticationEventPublisher;

    @SuppressFBWarnings({
        "EI2", "CT_CONSTRUCTOR_THROW"
    })
    public McpOAuth2ResourceServerSecurityConfigurerContributor(
        ObjectProvider<McpEndpoint> mcpEndpointProvider,
        ObjectProvider<McpJwtDecoderFactory> mcpJwtDecoderFactoryProvider,
        McpResourceServerProperties mcpResourceServerProperties, ApplicationProperties applicationProperties) {

        this.mcpJwtDecoderFactory = mcpJwtDecoderFactoryProvider.getIfAvailable(
            () -> new IssuerLocationMcpJwtDecoderFactory(trustedIssuerUris(mcpResourceServerProperties)));
        this.mcpEndpointProvider = mcpEndpointProvider;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @Autowired(required = false)
    @SuppressFBWarnings("EI2")
    public void setAuthenticationEventPublisher(AuthenticationEventPublisher authenticationEventPublisher) {
        this.authenticationEventPublisher = authenticationEventPublisher;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        List<Issuer> issuers = mcpResourceServerProperties.getIssuers();
        List<McpEndpoint> mcpEndpoints = mcpEndpointProvider.orderedStream()
            .toList();

        if (issuers.isEmpty() || mcpEndpoints.isEmpty()) {
            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        Issuer primaryIssuer = issuers.get(0);

        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(mcpJwtDecoderFactory);
        McpEndpointRequestMatcher mcpEndpointRequestMatcher = new McpEndpointRequestMatcher(mcpEndpoints);
        McpBearerTokenResolver mcpBearerTokenResolver = new McpBearerTokenResolver(mcpEndpointRequestMatcher);

        return (T) McpServerOAuth2Configurer.mcpServerOAuth2()
            .authorizationServer(primaryIssuer.getUri())
            .resourcePath(RESOURCE_PATH)
            .protectedResourceMetadataCustomizer(protectedResourceMetadata -> protectedResourceMetadata
                .authorizationServer(primaryIssuer.getUri())
                .resourceName(RESOURCE_NAME)
                .tlsClientCertificateBoundAccessTokens(false)
                .claims(claims -> customizeResourceClaims(claims, mcpEndpointRequestMatcher)))
            .validateAudienceClaim(false)
            .jwtDecoder(multiIssuerJwtDecoder)
            .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                .bearerTokenResolver(mcpBearerTokenResolver)
                .authenticationEntryPoint(new McpTenantProtectedResourceMetadataAuthenticationEntryPoint(
                    mcpEndpointRequestMatcher, publicUrl))
                .jwt(jwt -> jwt.authenticationManager(createJwtAuthenticationManager(multiIssuerJwtDecoder))));
    }

    private AuthenticationManager createJwtAuthenticationManager(JwtDecoder jwtDecoder) {
        ProviderManager providerManager = new ProviderManager(new JwtAuthenticationProvider(jwtDecoder));

        if (authenticationEventPublisher != null) {
            providerManager.setAuthenticationEventPublisher(
                new FailureAuthenticationEventPublisher(authenticationEventPublisher));
        }

        return providerManager;
    }

    private void customizeResourceClaims(
        Map<String, Object> claims, McpEndpointRequestMatcher mcpEndpointRequestMatcher) {

        Object resource = claims.get("resource");

        if (resource == null) {
            return;
        }

        String resourceUrl = McpProtectedResourceUrls.toPublicResourceUrl(publicUrl, resource.toString());

        claims.put("resource", resourceUrl);

        URI resourceUri = URI.create(resourceUrl);

        Optional<McpEndpoint> mcpEndpoint = mcpEndpointRequestMatcher.findMcpEndpoint(
            StringUtils.removeEnd(resourceUri.getRawPath(), "/"));

        mcpEndpoint.ifPresent(endpoint -> claims.put("scopes_supported", List.of(endpoint.getScope())));
    }

    private static List<String> trustedIssuerUris(McpResourceServerProperties mcpResourceServerProperties) {
        return mcpResourceServerProperties.getIssuers()
            .stream()
            .map(Issuer::getUri)
            .filter(Objects::nonNull)
            .toList();
    }

    private record FailureAuthenticationEventPublisher(AuthenticationEventPublisher authenticationEventPublisher)
        implements AuthenticationEventPublisher {

        @Override
        public void publishAuthenticationSuccess(Authentication authentication) {
        }

        @Override
        public void publishAuthenticationFailure(
            AuthenticationException authenticationException, Authentication authentication) {

            authenticationEventPublisher.publishAuthenticationFailure(authenticationException, authentication);
        }
    }
}
