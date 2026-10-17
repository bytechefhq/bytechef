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

import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.oauth2.IssuerLocationMcpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpBearerTokenResolver;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpProtectedResourceUrls;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataAuthenticationEntryPoint;
import com.bytechef.platform.security.web.mcp.oauth2.MultiIssuerJwtDecoder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(McpResourceServerProperties.class)
public class McpOAuth2ResourceServerSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private static final String MCP_PATH_REGEX = "^/api/(automation/.+|management(/[^/]+)?)/mcp$";
    private static final String RESOURCE_NAME = "ByteChef";
    private static final String RESOURCE_PATH = "/api";

    private final McpJwtDecoderFactory mcpJwtDecoderFactory;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final @Nullable String publicUrl;
    private @Nullable AuthenticationEventPublisher authenticationEventPublisher;

    @SuppressFBWarnings({
        "EI2", "CT_CONSTRUCTOR_THROW"
    })
    public McpOAuth2ResourceServerSecurityConfigurerContributor(
        ObjectProvider<McpJwtDecoderFactory> mcpJwtDecoderFactoryProvider,
        McpResourceServerProperties mcpResourceServerProperties,
        @Value("${bytechef.public-url:}") @Nullable String publicUrl) {

        this.mcpJwtDecoderFactory = mcpJwtDecoderFactoryProvider.getIfAvailable(
            () -> new IssuerLocationMcpJwtDecoderFactory(trustedIssuerUris(mcpResourceServerProperties)));
        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.publicUrl = publicUrl;
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

        if (issuers.isEmpty()) {
            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        Issuer primaryIssuer = issuers.get(0);

        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(mcpJwtDecoderFactory);
        McpBearerTokenResolver mcpBearerTokenResolver =
            new McpBearerTokenResolver(RegexRequestMatcher.regexMatcher(MCP_PATH_REGEX));

        return (T) McpServerOAuth2Configurer.mcpServerOAuth2()
            .authorizationServer(primaryIssuer.getUri())
            .resourcePath(RESOURCE_PATH)
            .protectedResourceMetadataCustomizer(protectedResourceMetadata -> protectedResourceMetadata
                .authorizationServer(primaryIssuer.getUri())
                .resourceName(RESOURCE_NAME)
                .tlsClientCertificateBoundAccessTokens(false)
                .claims(this::customizeResourceClaims))
            .validateAudienceClaim(false)
            .jwtDecoder(multiIssuerJwtDecoder)
            .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                .bearerTokenResolver(mcpBearerTokenResolver)
                .authenticationEntryPoint(new McpTenantProtectedResourceMetadataAuthenticationEntryPoint(publicUrl))
                .jwt(jwt -> jwt.authenticationManager(createJwtAuthenticationManager(multiIssuerJwtDecoder))));
    }

    private AuthenticationManager createJwtAuthenticationManager(JwtDecoder jwtDecoder) {
        ProviderManager providerManager = new ProviderManager(new JwtAuthenticationProvider(jwtDecoder));

        if (authenticationEventPublisher != null) {
            providerManager.setAuthenticationEventPublisher(authenticationEventPublisher);
        }

        return providerManager;
    }

    private void customizeResourceClaims(Map<String, Object> claims) {
        Object resource = claims.get("resource");

        if (resource == null) {
            return;
        }

        String resourceUrl = McpProtectedResourceUrls.toPublicResourceUrl(publicUrl, resource.toString());

        claims.put("resource", resourceUrl);

        URI resourceUri = URI.create(resourceUrl);

        String requiredScope = McpProtectedResourceUrls.getRequiredScope(resourceUri.getRawPath());

        if (requiredScope != null) {
            claims.put("scopes_supported", List.of(requiredScope));
        }
    }

    private static List<String> trustedIssuerUris(McpResourceServerProperties mcpResourceServerProperties) {
        return mcpResourceServerProperties.getIssuers()
            .stream()
            .map(Issuer::getUri)
            .filter(Objects::nonNull)
            .toList();
    }
}
