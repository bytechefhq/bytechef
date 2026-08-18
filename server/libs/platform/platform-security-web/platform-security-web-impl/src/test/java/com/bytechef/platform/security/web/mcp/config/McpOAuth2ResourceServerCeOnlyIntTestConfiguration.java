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

package com.bytechef.platform.security.web.mcp.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.ai.mcp.server.security.web.configurer.ManagementMcpServerSecurityConfigurer;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.McpDiscoverySecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpJwtSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpOAuth2ResourceServerSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import com.bytechef.platform.security.web.mcp.oauth2.McpFederatedIssuerAuthenticator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * CE-only context for the base MCP resource server: wires the management MCP filter chain with only the CE contributors
 * (resource server, base JWT policy, discovery) and no external-IdP federation - no
 * {@code McpFederatedIssuerAuthenticator} bean is present, so {@link McpJwtSecurityConfigurerContributor}'s
 * {@code ObjectProvider} resolves to empty. This proves the base resource server validates self-issuer JWTs
 * (signature/issuer/expiry, scope, audience, revocation) and rejects any non-statically-configured issuer, exactly as a
 * build with no {@code com.bytechef.ee.*} on the classpath would behave. A single self issuer is configured; its JWTs
 * are validated by a static-key decoder supplied through {@link McpJwtDecoderFactory} so the test signs its own tokens
 * without any issuer JWKS being fetched.
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.security")
@EnableAutoConfiguration
@EnableWebSecurity
@Import(LiquibaseConfiguration.class)
@Configuration
public class McpOAuth2ResourceServerCeOnlyIntTestConfiguration {

    public static final String ISSUER_URI = "https://as.bytechef.test";
    public static final String TENANT_CLAIM = "tenant_id";

    private static final String MCP_ENDPOINT = "/api/management/{secretKey}/mcp";

    @Bean
    AuthorityService authorityService() {
        return mock(AuthorityService.class);
    }

    @Bean
    PropertyService propertyService() {
        return mock(PropertyService.class);
    }

    @Bean
    UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    KeyPair mcpTestSigningKeyPair() {
        return generateRsaKeyPair();
    }

    @Bean
    McpResourceServerProperties mcpResourceServerProperties() {
        Issuer embeddedIssuer = new Issuer();

        embeddedIssuer.setUri(ISSUER_URI);
        embeddedIssuer.setTenantClaim(TENANT_CLAIM);
        embeddedIssuer.setSelf(true);

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(embeddedIssuer));

        return mcpResourceServerProperties;
    }

    @Bean
    McpJwtDecoderFactory mcpJwtDecoderFactory(KeyPair mcpTestSigningKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) mcpTestSigningKeyPair.getPublic();

        return issuer -> {
            if (!ISSUER_URI.equals(issuer)) {
                return null;
            }

            NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .build();

            nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

            return nimbusJwtDecoder;
        };
    }

    @Bean
    McpOAuth2ResourceServerSecurityConfigurerContributor mcpOAuth2ResourceServerSecurityConfigurerContributor(
        ObjectProvider<McpJwtDecoderFactory> mcpJwtDecoderFactoryProvider,
        McpResourceServerProperties mcpResourceServerProperties) {

        return new McpOAuth2ResourceServerSecurityConfigurerContributor(
            mcpJwtDecoderFactoryProvider, mcpResourceServerProperties);
    }

    @Bean
    McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        ObjectProvider<McpFederatedIssuerAuthenticator> mcpFederatedIssuerAuthenticatorProvider) {

        return new McpJwtSecurityConfigurerContributor(
            mcpResourceServerProperties, userService, mcpFederatedIssuerAuthenticatorProvider);
    }

    @Bean
    McpDiscoverySecurityConfigurerContributor mcpDiscoverySecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties,
        ObjectProvider<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolverProvider) {

        return new McpDiscoverySecurityConfigurerContributor(
            mcpResourceServerProperties, mcpAuthenticationRequiredResolverProvider);
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint(MCP_ENDPOINT)
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction(WebMvcStreamableServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @Bean
    McpAsyncServer mcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        return McpServer.async(transportProvider)
            .serverInfo("mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .build())
            .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
        PropertyService propertyService, UserService userService,
        McpOAuth2ResourceServerSecurityConfigurerContributor resourceServerContributor,
        McpJwtSecurityConfigurerContributor jwtContributor,
        McpDiscoverySecurityConfigurerContributor discoveryContributor) throws Exception {

        return http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .with(
                new ManagementMcpServerSecurityConfigurer(
                    apiKeyService, authorityService, propertyService, userService),
                withDefaults())
            .with(resourceServerContributor.getSecurityConfigurerAdapter(), withDefaults())
            .with(jwtContributor.getSecurityConfigurerAdapter(), withDefaults())
            .with(discoveryContributor.getSecurityConfigurerAdapter(), withDefaults())
            .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class McpOAuth2ResourceServerCeOnlyIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
