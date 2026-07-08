/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer.EmbeddedMcpServerSecurityConfigurer;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Boots a live Tomcat context (no database - the JDBC auto-configurations are excluded) that exercises the real
 * embedded MCP security chain with a lean MCP server. {@code ConnectedUserService}, {@code SigningKeyService} and
 * {@code IdentityProviderService} are mocked so the test controls the connected user and the tenant's trusted issuers;
 * OAuth2 JWTs are validated by a static-key decoder so the test signs its own tokens.
 *
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
    SigningKeyService signingKeyService() {
        return mock(SigningKeyService.class);
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
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ConnectedUserService connectedUserService, IdentityProviderService identityProviderService,
        McpJwtDecoderFactory mcpJwtDecoderFactory, SigningKeyService signingKeyService) throws Exception {

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
                    connectedUserService, signingKeyService, new McpTenantIssuerResolver(identityProviderService),
                    mcpJwtDecoderFactory, null),
                withDefaults())
            .build();
    }
}
