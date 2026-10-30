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
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.platform.security.web.mcp.oauth2.IssuerLocationMcpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpBearerTokenResolver;
import com.bytechef.platform.security.web.mcp.oauth2.McpEndpointRequestMatcher;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.MultiIssuerJwtDecoder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(McpResourceServerProperties.class)
public class McpOAuth2ResourceServerSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private static final String RESOURCE_PATH = "/api";

    private final McpJwtDecoderFactory mcpJwtDecoderFactory;
    private final ObjectProvider<McpEndpoint> mcpEndpointProvider;
    private final McpResourceServerProperties mcpResourceServerProperties;

    @SuppressFBWarnings({
        "EI2", "CT_CONSTRUCTOR_THROW"
    })
    public McpOAuth2ResourceServerSecurityConfigurerContributor(
        ObjectProvider<McpEndpoint> mcpEndpointProvider,
        ObjectProvider<McpJwtDecoderFactory> mcpJwtDecoderFactoryProvider,
        McpResourceServerProperties mcpResourceServerProperties) {

        this.mcpJwtDecoderFactory = mcpJwtDecoderFactoryProvider.getIfAvailable(
            () -> new IssuerLocationMcpJwtDecoderFactory(trustedIssuerUris(mcpResourceServerProperties)));
        this.mcpEndpointProvider = mcpEndpointProvider;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
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
        McpBearerTokenResolver mcpBearerTokenResolver =
            new McpBearerTokenResolver(new McpEndpointRequestMatcher(mcpEndpoints));

        return (T) McpServerOAuth2Configurer.mcpServerOAuth2()
            .authorizationServer(primaryIssuer.getUri())
            .resourcePath(RESOURCE_PATH)
            .validateAudienceClaim(false)
            .jwtDecoder(multiIssuerJwtDecoder)
            .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.bearerTokenResolver(
                mcpBearerTokenResolver));
    }

    private static List<String> trustedIssuerUris(McpResourceServerProperties mcpResourceServerProperties) {
        return mcpResourceServerProperties.getIssuers()
            .stream()
            .map(Issuer::getUri)
            .filter(Objects::nonNull)
            .toList();
    }
}
