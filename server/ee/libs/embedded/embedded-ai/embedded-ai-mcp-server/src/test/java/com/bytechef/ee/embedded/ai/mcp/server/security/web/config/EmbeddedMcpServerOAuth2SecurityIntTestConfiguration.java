/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerSecretVerifier;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer.EmbeddedMcpServerSecurityConfigurer;
import com.bytechef.ee.embedded.ai.mcp.server.web.EmbeddedMcpProtectedResourceMetadataController;
import com.bytechef.ee.embedded.ai.mcp.server.web.EmbeddedMcpProtectedResourceMetadataResolver;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration(excludeName = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
    "org.springframework.boot.data.jdbc.autoconfigure.JdbcRepositoriesAutoConfiguration",
    "org.springframework.boot.sql.init.autoconfigure.SqlInitializationAutoConfiguration"
})
@EnableWebSecurity
@Configuration
public class EmbeddedMcpServerOAuth2SecurityIntTestConfiguration {

    public static final String ISSUER_URI = "https://idp.customer.test";

    private static final String MCP_ENDPOINT = "/api/embedded/{secretKey}/mcp";
    private static final String SECRET_KEY = "secretKey";

    @Bean
    ConnectedUserService connectedUserService() {
        return mock(ConnectedUserService.class);
    }

    @Bean
    IdentityProviderService identityProviderService() {
        return mock(IdentityProviderService.class);
    }

    @Bean
    McpServerService mcpServerService() {
        return mock(McpServerService.class);
    }

    @Bean
    SigningKeyService signingKeyService() {
        return mock(SigningKeyService.class);
    }

    @Bean
    McpTenantIssuerResolver mcpTenantIssuerResolver(IdentityProviderService identityProviderService) {
        return new McpTenantIssuerResolver(identityProviderService);
    }

    @Bean
    McpTenantSecretVerifier mcpTenantSecretVerifier(McpServerService mcpServerService) {
        return new McpTenantSecretVerifier(() -> List.of(new EmbeddedMcpServerSecretVerifier(mcpServerService)));
    }

    @Bean
    EmbeddedMcpProtectedResourceMetadataResolver embeddedMcpProtectedResourceMetadataResolver(
        McpTenantIssuerResolver mcpTenantIssuerResolver, McpTenantSecretVerifier mcpTenantSecretVerifier) {

        return new EmbeddedMcpProtectedResourceMetadataResolver(mcpTenantIssuerResolver, mcpTenantSecretVerifier);
    }

    @Bean
    EmbeddedMcpProtectedResourceMetadataController embeddedMcpProtectedResourceMetadataController(
        EmbeddedMcpProtectedResourceMetadataResolver embeddedMcpProtectedResourceMetadataResolver) {

        return new EmbeddedMcpProtectedResourceMetadataController(embeddedMcpProtectedResourceMetadataResolver, null);
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
    McpJwtDecoderFactory mcpJwtDecoderFactory(KeyPair mcpTestSigningKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) mcpTestSigningKeyPair.getPublic();

        return issuer -> {
            if (!ISSUER_URI.equals(issuer)) {
                return null;
            }

            NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .build();

            nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

            return (JwtDecoder) nimbusJwtDecoder;
        };
    }

    @Bean
    WebMvcStreamableServerTransportProvider embeddedWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint(MCP_ENDPOINT)
            .contextExtractor(serverRequest -> {
                String externalUserId = SecurityUtils.getCurrentUserLogin();
                String secretKey = serverRequest.pathVariable(SECRET_KEY);
                HttpServletRequest httpServletRequest = serverRequest.servletRequest();

                return McpTransportContext.create(
                    Map.of(
                        "environment", String.valueOf(httpServletRequest.getHeader("X-Environment")),
                        "externalUserId", String.valueOf(externalUserId),
                        SECRET_KEY, secretKey));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse>
        embeddedMcpRouterFunction(WebMvcStreamableServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @Bean
    Object embeddedMcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        return new FilterableMcpServerBuilder(transportProvider)
            .serverInfo("embedded-mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .build())
            .toolFilter(exchange -> List.of())
            .build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain protectedResourceMetadataFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/.well-known/oauth-protected-resource/api/embedded/**")
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ConnectedUserService connectedUserService, McpJwtDecoderFactory mcpJwtDecoderFactory,
        McpServerService mcpServerService, McpTenantIssuerResolver mcpTenantIssuerResolver,
        McpTenantSecretVerifier mcpTenantSecretVerifier, SigningKeyService signingKeyService) throws Exception {

        return http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(RegexRequestMatcher.regexMatcher("^/api/embedded/.+/mcp"))
                .authenticated()
                .anyRequest()
                .permitAll())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .with(
                new EmbeddedMcpServerSecurityConfigurer(
                    connectedUserService, () -> List.of(Environment.values()), mcpServerService, signingKeyService,
                    mcpTenantIssuerResolver, mcpTenantSecretVerifier, mcpJwtDecoderFactory, null),
                withDefaults())
            .build();
    }
}
