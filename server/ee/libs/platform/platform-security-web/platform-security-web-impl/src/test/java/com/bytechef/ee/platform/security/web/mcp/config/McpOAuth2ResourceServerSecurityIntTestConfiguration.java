/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.ai.mcp.server.security.web.authentication.ManagementMcpServerSecretVerifier;
import com.bytechef.ai.mcp.server.security.web.configurer.ManagementMcpServerSecurityConfigurer;
import com.bytechef.ee.platform.security.web.config.McpTenantDiscoverySecurityConfiguration;
import com.bytechef.ee.platform.security.web.config.McpTenantIssuerResolverConfiguration;
import com.bytechef.ee.platform.security.web.config.McpTenantTrustResolutionConfigurerContributor;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerCacheEvictionListener;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataController;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.McpDiscoverySecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpJwtSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpOAuth2ResourceServerSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.security.web.mcp.oauth2.IssuerLocationMcpJwtDecoderFactory;
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
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.security")
@EnableAutoConfiguration
@EnableWebSecurity
@Import({
    LiquibaseConfiguration.class, McpTenantDiscoverySecurityConfiguration.class,
    McpTenantIssuerCacheEvictionListener.class, McpTenantIssuerResolverConfiguration.class,
    McpTenantProtectedResourceMetadataController.class, McpTenantTrustResolutionConfigurerContributor.class
})
@Configuration
public class McpOAuth2ResourceServerSecurityIntTestConfiguration {

    public static final String AUDIENCE_BOUND_ISSUER_URI = "https://audience-bound.idp.customer.test";
    public static final String ISSUER_URI = "https://as.bytechef.test";
    public static final String PER_TENANT_ISSUER_URI = "https://idp.customer.test";
    public static final String TENANT_CLAIM = "tenant_id";

    private static final String CANONICAL_MCP_ENDPOINT = "/api/management/mcp";
    private static final String MCP_ENDPOINT = "/api/management/{secretKey}/mcp";

    @Bean
    AuthorityService authorityService() {
        return mock(AuthorityService.class);
    }

    @Bean
    IdentityProviderService identityProviderService() {
        return mock(IdentityProviderService.class);
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
    KeyPair mcpPerTenantIdpTestSigningKeyPair() {
        return generateRsaKeyPair();
    }

    @Bean
    McpResourceServerProperties mcpResourceServerProperties() {
        Issuer selfIssuer = new Issuer();

        selfIssuer.setUri(ISSUER_URI);
        selfIssuer.setTenantClaim(TENANT_CLAIM);
        selfIssuer.setSelf(true);

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(selfIssuer));

        return mcpResourceServerProperties;
    }

    @Bean
    McpJwtDecoderFactory mcpJwtDecoderFactory(
        @Qualifier("mcpTestSigningKeyPair") KeyPair mcpTestSigningKeyPair,
        @Qualifier("mcpPerTenantIdpTestSigningKeyPair") KeyPair mcpPerTenantIdpTestSigningKeyPair) {

        RSAPublicKey selfPublicKey = (RSAPublicKey) mcpTestSigningKeyPair.getPublic();
        RSAPublicKey perTenantIdpPublicKey = (RSAPublicKey) mcpPerTenantIdpTestSigningKeyPair.getPublic();

        return new IssuerLocationMcpJwtDecoderFactory(List.of(ISSUER_URI)) {

            @Override
            public @Nullable JwtDecoder createJwtDecoder(String issuer) {
                if (!isTrustedIssuer(issuer)) {
                    return null;
                }

                NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder
                    .withPublicKey(ISSUER_URI.equals(issuer) ? selfPublicKey : perTenantIdpPublicKey)
                    .build();

                nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

                return nimbusJwtDecoder;
            }
        };
    }

    @Bean
    McpOAuth2ResourceServerSecurityConfigurerContributor mcpOAuth2ResourceServerSecurityConfigurerContributor(
        ObjectProvider<McpJwtDecoderFactory> mcpJwtDecoderFactoryProvider,
        McpResourceServerProperties mcpResourceServerProperties) {

        return new McpOAuth2ResourceServerSecurityConfigurerContributor(
            mcpJwtDecoderFactoryProvider, mcpResourceServerProperties, null);
    }

    @Bean
    McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        AuthorityService authorityService, ObjectProvider<McpServerSecretVerifier> mcpServerSecretVerifierProvider,
        ObjectProvider<SelfIssuedTokenRevocationChecker> selfIssuedTokenRevocationCheckerProvider,
        ObjectProvider<McpFederatedIssuerAuthenticator> mcpFederatedIssuerAuthenticatorProvider) {

        return new McpJwtSecurityConfigurerContributor(
            mcpResourceServerProperties, userService, authorityService, mcpServerSecretVerifierProvider,
            selfIssuedTokenRevocationCheckerProvider, null, mcpFederatedIssuerAuthenticatorProvider);
    }

    @Bean
    SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker() {
        return (tokenValue, tenantId) -> false;
    }

    @Bean
    McpServerSecretVerifier managementMcpServerSecretVerifier(PropertyService propertyService) {
        return new ManagementMcpServerSecretVerifier(propertyService);
    }

    @Bean
    McpDiscoverySecurityConfigurerContributor mcpDiscoverySecurityConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties,
        ObjectProvider<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolverProvider) {

        return new McpDiscoverySecurityConfigurerContributor(
            mcpResourceServerProperties, mcpAuthenticationRequiredResolverProvider, null);
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint(MCP_ENDPOINT)
            .build();
    }

    @Bean
    WebMvcStreamableServerTransportProvider canonicalWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint(CANONICAL_MCP_ENDPOINT)
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction() {
        return webMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    RouterFunction<ServerResponse> canonicalMcpRouterFunction() {
        return canonicalWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    McpAsyncServer mcpAsyncServer() {
        return createMcpAsyncServer(webMvcStreamableHttpServerTransportProvider());
    }

    @Bean
    McpAsyncServer canonicalMcpAsyncServer() {
        return createMcpAsyncServer(canonicalWebMvcStreamableHttpServerTransportProvider());
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
        PropertyService propertyService, UserService userService,
        McpOAuth2ResourceServerSecurityConfigurerContributor resourceServerContributor,
        McpJwtSecurityConfigurerContributor jwtContributor,
        McpDiscoverySecurityConfigurerContributor discoveryContributor,
        McpTenantTrustResolutionConfigurerContributor tenantTrustResolutionContributor) throws Exception {

        return http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .with(
                new ManagementMcpServerSecurityConfigurer(
                    apiKeyService, authorityService, () -> List.of(Environment.values()), propertyService, userService),
                withDefaults())
            .with(resourceServerContributor.getSecurityConfigurerAdapter(), withDefaults())
            .with(jwtContributor.getSecurityConfigurerAdapter(), withDefaults())
            .with(discoveryContributor.getSecurityConfigurerAdapter(), withDefaults())
            .with(tenantTrustResolutionContributor.getSecurityConfigurerAdapter(), withDefaults())
            .build();
    }

    private static McpAsyncServer createMcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        return McpServer.async(transportProvider)
            .serverInfo("mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .build())
            .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class McpOAuth2ResourceServerSecurityIntTestJdbcConfiguration
        extends AbstractIntTestJdbcConfiguration {
    }
}
