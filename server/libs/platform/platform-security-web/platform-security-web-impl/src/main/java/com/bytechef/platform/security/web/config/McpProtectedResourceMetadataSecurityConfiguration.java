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

import static org.springframework.security.config.Customizer.withDefaults;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Serves the RFC 9728 protected-resource metadata that the discovery {@code WWW-Authenticate} header points at. The
 * metadata lives at {@code /.well-known/oauth-protected-resource/**}, which the application's {@code /api/**} chain
 * does not match, so it is served by this dedicated, high-precedence chain - mirroring how the embedded authorization
 * server exposes {@code /.well-known/oauth-authorization-server} on its own chain. The chain applies the
 * resource-server configurer, whose {@code mcp-server-security} setup registers the metadata endpoint; the Bearer
 * filter it also installs is inert here because its resolver only claims tokens on MCP paths. Active only in EE and
 * only when an issuer is configured.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.oauth2.resource-server.issuers[0].uri")
public class McpProtectedResourceMetadataSecurityConfiguration {

    private static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource/**";

    private final McpOAuth2ResourceServerSecurityConfigurerContributor mcpOAuth2ResourceServerSecurityConfigurerContributor;

    @SuppressFBWarnings("EI2")
    public McpProtectedResourceMetadataSecurityConfiguration(
        McpOAuth2ResourceServerSecurityConfigurerContributor mcpOAuth2ResourceServerSecurityConfigurerContributor) {

        this.mcpOAuth2ResourceServerSecurityConfigurerContributor =
            mcpOAuth2ResourceServerSecurityConfigurerContributor;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 20)
    SecurityFilterChain mcpProtectedResourceMetadataFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(PROTECTED_RESOURCE_METADATA_PATH)
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .with(
                mcpOAuth2ResourceServerSecurityConfigurerContributor.getSecurityConfigurerAdapter(), withDefaults())
            .build();
    }
}
