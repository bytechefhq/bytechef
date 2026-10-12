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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Seeds registered clients, authorizations and consents through the real authorization server persistence beans.
 *
 * @author Ivica Cardic
 */
public final class RegisteredClientIntTestFixtures {

    public static final String MANAGEMENT_URL = "http://localhost/api/management/mcp";
    public static final String SCOPE = "mcp:automation";

    private final OAuth2AuthorizationConsentService authorizationConsentService;
    private final OAuth2AuthorizationService authorizationService;
    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;

    @SuppressFBWarnings("EI2")
    public RegisteredClientIntTestFixtures(
        DataSource dataSource, RegisteredClientRepository registeredClientRepository,
        OAuth2AuthorizationConsentService authorizationConsentService) {

        this.authorizationConsentService = authorizationConsentService;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.authorizationService = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        this.registeredClientRepository = registeredClientRepository;
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM oauth2_authorization_consent");
        jdbcTemplate.update("DELETE FROM oauth2_authorization");
        jdbcTemplate.update("DELETE FROM oauth2_registered_client");
    }

    public int countAuthorizations() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_authorization", Integer.class);

        return count == null ? 0 : count;
    }

    public int countConsents() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM oauth2_authorization_consent", Integer.class);

        return count == null ? 0 : count;
    }

    public void saveClient(String id) {
        RegisteredClient registeredClient = RegisteredClient.withId(id)
            .clientId(id + "-client-id")
            .clientName(id + " name")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://127.0.0.1/callback")
            .scope(SCOPE)
            .build();

        registeredClientRepository.save(registeredClient);
    }

    public void saveAuthorization(String registeredClientId, String principalName, String resource, Instant issuedAt) {
        Instant now = Instant.now();

        saveAuthorization(
            registeredClientId, principalName, resource, issuedAt, now.plus(Duration.ofHours(1)));
    }

    public void saveExpiredAuthorization(String registeredClientId, String principalName) {
        Instant now = Instant.now();

        saveAuthorization(
            registeredClientId, principalName, MANAGEMENT_URL, now.minus(Duration.ofHours(2)),
            now.minus(Duration.ofHours(1)));
    }

    public void saveConsent(String registeredClientId, String principalName) {
        OAuth2AuthorizationConsent authorizationConsent = OAuth2AuthorizationConsent
            .withId(registeredClientId, principalName)
            .scope(SCOPE)
            .build();

        authorizationConsentService.save(authorizationConsent);
    }

    private void saveAuthorization(
        String registeredClientId, String principalName, String resource, Instant issuedAt, Instant expiresAt) {

        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);

        if (registeredClient == null) {
            throw new IllegalStateException("Unknown registered client " + registeredClientId);
        }

        String id = UUID.randomUUID()
            .toString();

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("http://localhost/oauth2/authorize")
            .clientId(registeredClient.getClientId())
            .redirectUri("http://127.0.0.1/callback")
            .additionalParameters(Map.of(OAuth2ParameterNames.RESOURCE, resource))
            .build();

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(id)
            .principalName(principalName)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of(SCOPE))
            .attribute(OAuth2AuthorizationRequest.class.getName(), authorizationRequest)
            .token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-" + id, issuedAt, expiresAt))
            .build();

        authorizationService.save(authorization);
    }
}
