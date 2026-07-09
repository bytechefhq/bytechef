/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.mcp.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.automation.ai.mcp.server.security.web.configurer.AutomationMcpServerSecurityConfigurer;
import com.bytechef.ee.platform.security.web.config.McpTenantTrustResolutionConfigurerContributor;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.TenantIdpFederatedIssuerAuthenticator;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.McpJwtSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpOAuth2ResourceServerSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpFederatedIssuerAuthenticator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
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
 * Boots a live Tomcat + PostgreSQL context exercising the automation MCP filter chain with the OAuth2 resource server
 * enabled alongside the Phase 1 API-key filter, so the automation endpoint's JWT behavior is proven end to end (the
 * resource-server code and its other IntTests live in {@code platform-security-web-impl} against the management
 * endpoint). JWTs are validated by a static-key decoder so the test signs its own tokens.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.security")
@EnableAutoConfiguration
@EnableWebSecurity
@Import(LiquibaseConfiguration.class)
@Configuration
public class AutomationMcpOAuth2ResourceServerSecurityIntTestConfiguration {

    public static final String ISSUER_URI = "https://as.bytechef.test";
    public static final String TENANT_CLAIM = "tenant_id";

    private static final String MCP_ENDPOINT = "/api/automation/{secretKey}/mcp";
    private static final String SECRET_KEY = "secretKey";

    @Bean
    AuthorityService authorityService() {
        return mock(AuthorityService.class);
    }

    @Bean
    McpServerService mcpServerService() {
        return mock(McpServerService.class);
    }

    @Bean
    UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    KeyPair mcpTestSigningKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Bean
    McpResourceServerProperties mcpResourceServerProperties() {
        Issuer issuer = new Issuer();

        issuer.setUri(ISSUER_URI);
        issuer.setTenantClaim(TENANT_CLAIM);

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(issuer));

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
    IdentityProviderService identityProviderService() {
        IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of());

        return identityProviderService;
    }

    @Bean
    McpFederatedIssuerAuthenticator mcpFederatedIssuerAuthenticator() {
        return new TenantIdpFederatedIssuerAuthenticator(new McpAudienceValidator(), new McpJwtIdentityMapper());
    }

    @Bean
    McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        ObjectProvider<McpFederatedIssuerAuthenticator> mcpFederatedIssuerAuthenticatorProvider) {

        return new McpJwtSecurityConfigurerContributor(
            mcpResourceServerProperties, userService, mcpFederatedIssuerAuthenticatorProvider);
    }

    @Bean
    McpTenantTrustResolutionConfigurerContributor mcpTenantTrustResolutionConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, IdentityProviderService identityProviderService) {

        return new McpTenantTrustResolutionConfigurerContributor(
            mcpResourceServerProperties, new McpTenantIssuerResolver(identityProviderService));
    }

    @Bean
    WebMvcStreamableServerTransportProvider automationWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint(MCP_ENDPOINT)
            .contextExtractor(serverRequest -> {
                String secretKey = serverRequest.pathVariable(SECRET_KEY);

                return McpTransportContext.create(Map.of(SECRET_KEY, secretKey));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> automationMcpRouterFunction(
        WebMvcStreamableServerTransportProvider transportProvider) {

        return transportProvider.getRouterFunction();
    }

    @Bean
    Object automationMcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        return new FilterableMcpServerBuilder(transportProvider)
            .serverInfo("automation-mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .build())
            .toolFilter(exchange -> List.of())
            .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
        McpServerService mcpServerService, UserService userService,
        McpOAuth2ResourceServerSecurityConfigurerContributor resourceServerContributor,
        McpTenantTrustResolutionConfigurerContributor tenantTrustContributor,
        McpJwtSecurityConfigurerContributor jwtContributor) throws Exception {

        return http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .with(
                new AutomationMcpServerSecurityConfigurer(
                    apiKeyService, authorityService, mcpServerService, userService),
                withDefaults())
            .with(resourceServerContributor.getSecurityConfigurerAdapter(), withDefaults())
            .with(tenantTrustContributor.getSecurityConfigurerAdapter(), withDefaults())
            .with(jwtContributor.getSecurityConfigurerAdapter(), withDefaults())
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AutomationMcpOAuth2ResourceServerSecurityIntTestJdbcConfiguration
        extends AbstractIntTestJdbcConfiguration {
    }
}
