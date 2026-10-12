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

import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.TENANT_ID_CLAIM;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.oauth2.authorizationserver.annotation.ConditionalOnOauth2AuthorizationServerEnabled;
import com.bytechef.platform.oauth2.authorizationserver.authentication.PublicClientRefreshTokenAuthenticationConverter;
import com.bytechef.platform.oauth2.authorizationserver.authentication.PublicClientRefreshTokenAuthenticationProvider;
import com.bytechef.platform.oauth2.authorizationserver.jwk.PersistentJwkSource;
import com.bytechef.platform.oauth2.authorizationserver.registration.McpAuthorizationConsentRequired;
import com.bytechef.platform.oauth2.authorizationserver.registration.McpClientRegistrationValidator;
import com.bytechef.platform.oauth2.authorizationserver.registration.McpRegisteredClientConverter;
import com.bytechef.platform.oauth2.authorizationserver.tenant.AuthorizationServerTokenRevocationChecker;
import com.bytechef.platform.oauth2.authorizationserver.tenant.SessionTenantFilter;
import com.bytechef.platform.oauth2.authorizationserver.tenant.SharedRegisteredClientRepository;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantKeyedAuthorizationCodeGenerator;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantKeyedRefreshTokenGenerator;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantRoutingOAuth2AuthorizationService;
import com.bytechef.platform.oauth2.authorizationserver.token.McpResourceAudienceTokenCustomizer;
import com.bytechef.platform.oauth2.authorizationserver.web.LoginRedirectAuthenticationEntryPoint;
import com.bytechef.tenant.TenantContext;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.McpDefaultJwtCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationConsentAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Hosts the embedded Spring Authorization Server for MCP clients: a security filter chain scoped to the authorization
 * server endpoints that reuses the application's login session, tenant-keyed authorization codes and refresh tokens,
 * tenant-aware authorization storage, and access tokens carrying the tenant and the user's authorities.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnOauth2AuthorizationServerEnabled
public class Oauth2AuthorizationServerConfiguration {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String CONSENT_PAGE_URI = "/oauth2/consent";
    private static final String FACTOR_AUTHORITY_PREFIX = "FACTOR_";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain oauth2AuthorizationServerSecurityFilterChain(
        HttpSecurity http, AuthorizationServerSettings authorizationServerSettings, JwtDecoder jwtDecoder,
        RegisteredClientRepository registeredClientRepository) throws Exception {

        http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .authenticated())
            .with(
                McpAuthorizationServerConfigurer.mcpAuthorizationServer(),
                mcpAuthorizationServer -> {
                    mcpAuthorizationServer.dynamicClientRegistration(true);
                    mcpAuthorizationServer.dynamicClientRegistrationValidator(new McpClientRegistrationValidator());

                    mcpAuthorizationServer.authorizationServer(authorizationServer -> {
                        authorizationServer.addObjectPostProcessor(McpAuthorizationConsentRequired.postProcessor());

                        authorizationServer.clientAuthentication(clientAuthentication -> clientAuthentication
                            .authenticationConverter(new PublicClientRefreshTokenAuthenticationConverter())
                            .authenticationProviders(authenticationProviders -> authenticationProviders.addFirst(
                                new PublicClientRefreshTokenAuthenticationProvider(registeredClientRepository))));

                        authorizationServer.clientRegistrationEndpoint(
                            clientRegistrationEndpoint -> clientRegistrationEndpoint.authenticationProviders(
                                authenticationProviders -> authenticationProviders.forEach(
                                    Oauth2AuthorizationServerConfiguration::setRegisteredClientConverter)));

                        http.securityMatcher(
                            new OrRequestMatcher(
                                authorizationServer.getEndpointsMatcher(),
                                PathPatternRequestMatcher.withDefaults()
                                    .matcher("/.well-known/openid-configuration")));

                        authorizationServer.authorizationEndpoint(
                            authorizationEndpoint -> authorizationEndpoint.consentPage(CONSENT_PAGE_URI));

                        TenantKeyedAuthorizationCodeGenerator authorizationCodeGenerator =
                            new TenantKeyedAuthorizationCodeGenerator();

                        authorizationServer.authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                            .authenticationProviders(authenticationProviders -> authenticationProviders.forEach(
                                authenticationProvider -> setAuthorizationCodeGenerator(
                                    authenticationProvider, authorizationCodeGenerator))));
                    });
                })
            .addFilterBefore(
                new SessionTenantFilter(
                    authorizationServerSettings.getTokenEndpoint(),
                    authorizationServerSettings.getTokenRevocationEndpoint(),
                    authorizationServerSettings.getTokenIntrospectionEndpoint(), jwtDecoder),
                SecurityContextHolderFilter.class)
            .exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(
                new LoginRedirectAuthenticationEntryPoint(), createHtmlRequestMatcher()));

        return http.build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcOperations jdbcOperations) {
        return new SharedRegisteredClientRepository(new JdbcRegisteredClientRepository(jdbcOperations));
    }

    @Bean
    TenantRoutingOAuth2AuthorizationService authorizationService(
        JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository,
        PlatformTransactionManager transactionManager) {

        return new TenantRoutingOAuth2AuthorizationService(
            new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository), jdbcOperations,
            new TransactionTemplate(transactionManager));
    }

    @Bean
    AuthorizationServerTokenRevocationChecker authorizationServerTokenRevocationChecker(
        TenantRoutingOAuth2AuthorizationService authorizationService) {

        return new AuthorizationServerTokenRevocationChecker(authorizationService);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
        JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(DataSource dataSource, Encryption encryption) {
        return new PersistentJwkSource(dataSource, encryption);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(ApplicationProperties applicationProperties) {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();

        String publicUrl = applicationProperties.getPublicUrl();

        if (publicUrl != null && !publicUrl.isBlank()) {
            builder.issuer(removeTrailingSlash(publicUrl.strip()));
        }

        return builder.build();
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> tokenGenerator(
        JWKSource<SecurityContext> jwkSource, OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer) {

        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        McpResourceAudienceTokenCustomizer audienceTokenCustomizer = new McpResourceAudienceTokenCustomizer();

        jwtGenerator.setJwtCustomizer(context -> {
            McpDefaultJwtCustomizer.DEFAULT_JWT_CUSTOMIZER.customize(context);
            audienceTokenCustomizer.customize(context);
            jwtTokenCustomizer.customize(context);
        });

        return new DelegatingOAuth2TokenGenerator(jwtGenerator, new TenantKeyedRefreshTokenGenerator());
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }

            JwtClaimsSet.Builder claims = context.getClaims();

            claims.claim(TENANT_ID_CLAIM, TenantContext.getCurrentTenantId());

            Authentication principal = context.getPrincipal();

            if (principal != null) {
                List<String> authorities = principal.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority != null && !authority.startsWith(FACTOR_AUTHORITY_PREFIX))
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));

                if (!authorities.isEmpty()) {
                    claims.claim(AUTHORITIES_CLAIM, authorities);
                }
            }
        };
    }

    private static void setAuthorizationCodeGenerator(
        AuthenticationProvider authenticationProvider,
        TenantKeyedAuthorizationCodeGenerator authorizationCodeGenerator) {

        if (authenticationProvider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider requestProvider) {
            requestProvider.setAuthorizationCodeGenerator(authorizationCodeGenerator);
        } else if (authenticationProvider instanceof OAuth2AuthorizationConsentAuthenticationProvider consentProvider) {
            consentProvider.setAuthorizationCodeGenerator(authorizationCodeGenerator);
        }
    }

    private static void setRegisteredClientConverter(AuthenticationProvider authenticationProvider) {
        if (authenticationProvider instanceof OAuth2ClientRegistrationAuthenticationProvider registrationProvider) {
            registrationProvider.setRegisteredClientConverter(new McpRegisteredClientConverter());
        }
    }

    private static String removeTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

    private static RequestMatcher createHtmlRequestMatcher() {
        MediaTypeRequestMatcher mediaTypeRequestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);

        mediaTypeRequestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));

        return mediaTypeRequestMatcher;
    }
}
