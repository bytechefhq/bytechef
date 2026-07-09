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

package com.bytechef.platform.oauth2.authorizationserver.config;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Embedded OAuth2 authorization server configuration. Only active when the property
 * {@code bytechef.oauth2.authorization-server.enabled} is {@code true}. When active, it exposes the Spring
 * Authorization Server endpoints - including the metadata endpoint {@code /.well-known/oauth-authorization-server} and
 * Dynamic Client Registration (DCR) - backed by the Spring Authorization Server JDBC persistence beans.
 *
 * <p>
 * The authorization-server filter chain is scoped with {@link HttpSecurity#securityMatcher} to the authorization
 * server's own endpoints, so it does not shadow the surrounding application's other security chains (its
 * {@code /api/**} and MCP chains). Unauthenticated browser requests to the authorize endpoint are redirected to
 * ByteChef's login page, reusing the application's existing form-login session rather than a second user store.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.oauth2.authorization-server.enabled", havingValue = "true")
public class Oauth2AuthorizationServerConfiguration {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String CONSENT_PAGE_URI = "/oauth2/consent";
    private static final Pattern EMBEDDED_MCP_RESOURCE_PATTERN = Pattern.compile("/api/embedded/(.+)/mcp");
    private static final String TENANT_ID_CLAIM = "tenant_id";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain oauth2AuthorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .authenticated())
            .with(
                McpAuthorizationServerConfigurer.mcpAuthorizationServer(),
                mcpAuthorizationServer -> {
                    mcpAuthorizationServer.dynamicClientRegistration(true);

                    mcpAuthorizationServer.authorizationServer(authorizationServer -> {
                        http.securityMatcher(
                            new OrRequestMatcher(
                                authorizationServer.getEndpointsMatcher(),
                                PathPatternRequestMatcher.withDefaults()
                                    .matcher("/.well-known/openid-configuration")));

                        authorizationServer.authorizationEndpoint(
                            authorizationEndpoint -> authorizationEndpoint.consentPage(CONSENT_PAGE_URI));
                    });
                })
            .exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/"), createHtmlRequestMatcher()));

        return http.build();
    }

    @Bean
    JdbcRegisteredClientRepository registeredClientRepository(JdbcOperations jdbcOperations) {
        return new JdbcRegisteredClientRepository(jdbcOperations);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
        JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
        JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID()
                .toString())
            .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .build();
    }

    /**
     * Mints the claims the MCP resource server needs onto issued access-token JWTs: {@code tenant_id} (the tenant the
     * token authorizes) and {@code authorities} (the granted authorities of the resource owner who authorized the
     * token), so the embedded-issuer path carries the same identity as the API-key path.
     *
     * <p>
     * The tenant is taken from the RFC 8707 {@code resource} parameter on the token request when it targets an embedded
     * MCP endpoint ({@code /api/embedded/{secretKey}/mcp}, whose path secret is a {@link TenantKey}) - the token
     * endpoint is a back-channel request with no session, so the tenant cannot come from {@link TenantContext} there.
     * Requests without such a resource fall back to the current tenant.
     */
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }

            JwtClaimsSet.Builder claims = context.getClaims();

            claims.claim(TENANT_ID_CLAIM, resolveTenantId(context));

            Authentication principal = context.getPrincipal();

            if (principal != null) {
                List<String> authorities = principal.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .sorted()
                    .toList();

                if (!authorities.isEmpty()) {
                    claims.claim(AUTHORITIES_CLAIM, authorities);
                }
            }
        };
    }

    private static String resolveTenantId(JwtEncodingContext context) {
        String tenantId = resolveTenantIdFromResource(context);

        return tenantId != null ? tenantId : TenantContext.getCurrentTenantId();
    }

    @Nullable
    private static String resolveTenantIdFromResource(JwtEncodingContext context) {
        if (!(context.getAuthorizationGrant() instanceof OAuth2AuthorizationGrantAuthenticationToken grant)) {
            return null;
        }

        Object resource = grant.getAdditionalParameters()
            .get(OAuth2ParameterNames.RESOURCE);

        if (resource instanceof Collection<?> resources && !resources.isEmpty()) {
            resource = resources.iterator()
                .next();
        }

        if (resource == null) {
            return null;
        }

        Matcher matcher = EMBEDDED_MCP_RESOURCE_PATTERN.matcher(resource.toString());

        if (!matcher.find()) {
            return null;
        }

        try {
            TenantKey tenantKey = TenantKey.parse(matcher.group(1));

            return tenantKey.getTenantId();
        } catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private static RequestMatcher createHtmlRequestMatcher() {
        MediaTypeRequestMatcher mediaTypeRequestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);

        mediaTypeRequestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));

        return mediaTypeRequestMatcher;
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate RSA key pair", exception);
        }
    }
}
