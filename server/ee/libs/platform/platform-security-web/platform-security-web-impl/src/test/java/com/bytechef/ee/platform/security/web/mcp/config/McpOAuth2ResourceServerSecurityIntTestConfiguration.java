/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.ai.mcp.server.security.web.authentication.ManagementMcpAuthenticationRequiredResolver;
import com.bytechef.ai.mcp.server.security.web.configurer.ManagementMcpServerSecurityConfigurer;
import com.bytechef.ee.platform.security.web.config.McpTenantTrustResolutionConfigurerContributor;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.TenantIdpFederatedIssuerAuthenticator;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.McpDiscoverySecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpJwtSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpOAuth2ResourceServerSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpFederatedIssuerAuthenticator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.mockito.Mockito;
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
 * Boots a live Tomcat + PostgreSQL context that exercises the management MCP filter chain with the OAuth2 resource
 * server enabled alongside the Phase 1 API-key filter. A single trusted issuer is configured; its JWTs are validated by
 * a static-key {@code JwtDecoder} (supplied through {@link McpJwtDecoderFactory}) so the test signs its own tokens
 * without any issuer JWKS being fetched over HTTP.
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
public class McpOAuth2ResourceServerSecurityIntTestConfiguration {

    public static final String ISSUER_URI = "https://as.bytechef.test";
    public static final String TENANT_CLAIM = "tenant_id";
    public static final String EXTERNAL_ISSUER_URI = "https://idp.customer.test";
    public static final String EXTERNAL_TENANT_CLAIM = "org";
    public static final String EXTERNAL_AUTHORITIES_CLAIM = "groups";
    // A per-tenant external IdP, trusted only for the "public" tenant via its IdentityProvider record (T1.3).
    public static final String PER_TENANT_IDP_ISSUER_URI = "https://idp.per-tenant.test";
    public static final String PER_TENANT_IDP_TENANT_ID = "public";

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
    KeyPair mcpExternalTestSigningKeyPair() {
        return generateRsaKeyPair();
    }

    @Bean
    KeyPair mcpPerTenantIdpTestSigningKeyPair() {
        return generateRsaKeyPair();
    }

    @Bean
    IdentityProviderService identityProviderService() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setName("per-tenant-idp");
        identityProvider.setIssuerUri(PER_TENANT_IDP_ISSUER_URI);
        identityProvider.setEnabled(true);
        identityProvider.setMcpManagement(true);
        identityProvider.setAuthorityMappings(java.util.Map.of("editors", "ROLE_EDITOR"));

        IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Tenant-scoped: the provider is only visible in its own tenant, so its token is rejected on another tenant's
        // endpoint. The resolver reads it within TenantContext.callWithTenantId(...).
        Mockito.when(identityProviderService.getIdentityProviders())
            .thenAnswer(invocation -> PER_TENANT_IDP_TENANT_ID.equals(TenantContext.getCurrentTenantId())
                ? List.of(identityProvider)
                : List.of());

        return identityProviderService;
    }

    @Bean
    McpResourceServerProperties mcpResourceServerProperties() {
        Issuer embeddedIssuer = new Issuer();

        embeddedIssuer.setUri(ISSUER_URI);
        embeddedIssuer.setTenantClaim(TENANT_CLAIM);
        embeddedIssuer.setSelf(true);

        Issuer externalIssuer = new Issuer();

        externalIssuer.setUri(EXTERNAL_ISSUER_URI);
        externalIssuer.setTenantClaim(EXTERNAL_TENANT_CLAIM);
        externalIssuer.setAuthoritiesClaim(EXTERNAL_AUTHORITIES_CLAIM);
        externalIssuer.setAuthorities(List.of("ROLE_USER"));

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(embeddedIssuer, externalIssuer));

        return mcpResourceServerProperties;
    }

    @Bean
    McpJwtDecoderFactory mcpJwtDecoderFactory(
        KeyPair mcpTestSigningKeyPair, KeyPair mcpExternalTestSigningKeyPair,
        KeyPair mcpPerTenantIdpTestSigningKeyPair) {

        RSAPublicKey embeddedPublicKey = (RSAPublicKey) mcpTestSigningKeyPair.getPublic();
        RSAPublicKey externalPublicKey = (RSAPublicKey) mcpExternalTestSigningKeyPair.getPublic();
        RSAPublicKey perTenantIdpPublicKey = (RSAPublicKey) mcpPerTenantIdpTestSigningKeyPair.getPublic();

        return issuer -> {
            RSAPublicKey publicKey = switch (issuer) {
                case ISSUER_URI -> embeddedPublicKey;
                case EXTERNAL_ISSUER_URI -> externalPublicKey;
                case PER_TENANT_IDP_ISSUER_URI -> perTenantIdpPublicKey;
                default -> null;
            };

            if (publicKey == null) {
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
    McpAuthenticationRequiredResolver managementMcpAuthenticationRequiredResolver(PropertyService propertyService) {
        return new ManagementMcpAuthenticationRequiredResolver(propertyService);
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
        McpTenantTrustResolutionConfigurerContributor tenantTrustContributor,
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
            .with(tenantTrustContributor.getSecurityConfigurerAdapter(), withDefaults())
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
    public static class McpOAuth2ResourceServerSecurityIntTestJdbcConfiguration
        extends AbstractIntTestJdbcConfiguration {
    }
}
