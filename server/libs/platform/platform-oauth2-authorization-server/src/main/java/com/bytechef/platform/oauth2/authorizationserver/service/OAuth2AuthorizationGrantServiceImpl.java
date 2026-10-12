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

package com.bytechef.platform.oauth2.authorizationserver.service;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.authorizationserver.annotation.ConditionalOnOauth2AuthorizationServerEnabled;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates the active {@code oauth2_authorization} rows of a registered client per principal and revokes them,
 * together with the stored consents, by deleting the rows.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnOauth2AuthorizationServerEnabled
public class OAuth2AuthorizationGrantServiceImpl implements OAuth2AuthorizationGrantService {

    private static final String SELECT_ACTIVE_AUTHORIZATIONS =
        """
            SELECT *
            FROM oauth2_authorization
            WHERE registered_client_id = ?
            AND (access_token_expires_at > CURRENT_TIMESTAMP OR refresh_token_expires_at > CURRENT_TIMESTAMP)""";

    private final JdbcOperations jdbcOperations;
    private final McpResourceNameResolver mcpResourceNameResolver;
    private final RegisteredClientRepository registeredClientRepository;

    @SuppressFBWarnings("EI2")
    public OAuth2AuthorizationGrantServiceImpl(
        JdbcOperations jdbcOperations, McpResourceNameResolver mcpResourceNameResolver,
        RegisteredClientRepository registeredClientRepository) {

        this.jdbcOperations = jdbcOperations;
        this.mcpResourceNameResolver = mcpResourceNameResolver;
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public List<OAuth2AuthorizationGrant> getGrants(String registeredClientId) {
        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);

        if (registeredClient == null) {
            return List.of();
        }

        ActiveAuthorizationReader activeAuthorizationReader = new ActiveAuthorizationReader(
            jdbcOperations, registeredClient);

        Instant now = Instant.now();

        Map<String, List<OAuth2Authorization>> authorizationsByPrincipal = activeAuthorizationReader
            .findActiveAuthorizations()
            .stream()
            .filter(authorization -> isActive(authorization, now))
            .collect(Collectors.groupingBy(OAuth2Authorization::getPrincipalName));

        return authorizationsByPrincipal.entrySet()
            .stream()
            .map(entry -> toGrant(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(
                OAuth2AuthorizationGrant::lastIssuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    @Override
    @Transactional
    public void revokeGrant(String registeredClientId, String principalName) {
        int deletedAuthorizationCount = jdbcOperations.update(
            "DELETE FROM oauth2_authorization WHERE registered_client_id = ? AND principal_name = ?",
            registeredClientId, principalName);
        int deletedConsentCount = jdbcOperations.update(
            "DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ? AND principal_name = ?",
            registeredClientId, principalName);

        if (deletedAuthorizationCount + deletedConsentCount == 0) {
            throw new ConfigurationException(
                "No grant of " + principalName + " for OAuth2 client " + registeredClientId + " to revoke",
                RegisteredClientErrorType.GRANT_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void revokeGrants(String registeredClientId) {
        int deletedAuthorizationCount = jdbcOperations.update(
            "DELETE FROM oauth2_authorization WHERE registered_client_id = ?", registeredClientId);
        int deletedConsentCount = jdbcOperations.update(
            "DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", registeredClientId);

        if (deletedAuthorizationCount + deletedConsentCount == 0) {
            throw new ConfigurationException(
                "No grants for OAuth2 client " + registeredClientId + " to revoke",
                RegisteredClientErrorType.GRANT_NOT_FOUND);
        }
    }

    private OAuth2AuthorizationGrant toGrant(String principalName, List<OAuth2Authorization> authorizations) {
        List<String> scopes = authorizations.stream()
            .flatMap(authorization -> authorization.getAuthorizedScopes()
                .stream())
            .distinct()
            .sorted()
            .toList();

        List<String> mcpServers = authorizations.stream()
            .flatMap(OAuth2AuthorizationGrantServiceImpl::getResources)
            .map(mcpResourceNameResolver::resolveName)
            .distinct()
            .sorted()
            .toList();

        List<Instant> issuedAts = authorizations.stream()
            .map(OAuth2AuthorizationGrantServiceImpl::getIssuedAt)
            .filter(Objects::nonNull)
            .toList();

        Instant authorizedAt = issuedAts.stream()
            .min(Comparator.naturalOrder())
            .orElse(null);
        Instant lastIssuedAt = issuedAts.stream()
            .max(Comparator.naturalOrder())
            .orElse(null);

        Instant expiresAt = getMaxExpiresAt(authorizations, OAuth2RefreshToken.class)
            .or(() -> getMaxExpiresAt(authorizations, OAuth2AccessToken.class))
            .orElse(null);

        return new OAuth2AuthorizationGrant(principalName, scopes, mcpServers, authorizedAt, lastIssuedAt, expiresAt);
    }

    @Nullable
    private static Instant getIssuedAt(OAuth2Authorization authorization) {
        Instant accessTokenIssuedAt = getTokenInstant(authorization, OAuth2AccessToken.class, true);

        if (accessTokenIssuedAt != null) {
            return accessTokenIssuedAt;
        }

        return getTokenInstant(authorization, OAuth2AuthorizationCode.class, true);
    }

    private static Optional<Instant> getMaxExpiresAt(
        List<OAuth2Authorization> authorizations, Class<? extends AbstractOAuth2Token> tokenType) {

        return authorizations.stream()
            .map(authorization -> getTokenInstant(authorization, tokenType, false))
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder());
    }

    @Nullable
    private static Instant getTokenInstant(
        OAuth2Authorization authorization, Class<? extends AbstractOAuth2Token> tokenType, boolean issuedAt) {

        OAuth2Authorization.Token<? extends AbstractOAuth2Token> token = authorization.getToken(tokenType);

        if (token == null) {
            return null;
        }

        AbstractOAuth2Token oAuth2Token = token.getToken();

        return issuedAt ? oAuth2Token.getIssuedAt() : oAuth2Token.getExpiresAt();
    }

    private static Stream<String> getResources(OAuth2Authorization authorization) {
        OAuth2AuthorizationRequest authorizationRequest =
            authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());

        if (authorizationRequest == null) {
            return Stream.empty();
        }

        Map<String, Object> additionalParameters = authorizationRequest.getAdditionalParameters();

        Object resource = additionalParameters.get(OAuth2ParameterNames.RESOURCE);

        if (resource instanceof Collection<?> resources) {
            return resources.stream()
                .filter(Objects::nonNull)
                .map(Object::toString);
        }

        if (resource == null) {
            return Stream.empty();
        }

        return Stream.of(resource.toString());
    }

    private static boolean isActive(OAuth2Authorization authorization, Instant now) {
        return Stream.of(OAuth2AccessToken.class, OAuth2RefreshToken.class)
            .map(tokenType -> getTokenInstant(authorization, tokenType, false))
            .anyMatch(expiresAt -> expiresAt != null && expiresAt.isAfter(now));
    }

    /**
     * Reads the active authorizations of one registered client in a single query, resolving every row against that
     * client instead of looking it up again per row.
     */
    private static final class ActiveAuthorizationReader extends JdbcOAuth2AuthorizationService {

        private final RegisteredClient registeredClient;

        private ActiveAuthorizationReader(JdbcOperations jdbcOperations, RegisteredClient registeredClient) {
            super(jdbcOperations, new SingleRegisteredClientRepository(registeredClient));

            this.registeredClient = registeredClient;
        }

        private List<OAuth2Authorization> findActiveAuthorizations() {
            JdbcOperations jdbcOperations = getJdbcOperations();

            return jdbcOperations.query(
                SELECT_ACTIVE_AUTHORIZATIONS, getAuthorizationRowMapper(), registeredClient.getId());
        }
    }

    private record SingleRegisteredClientRepository(RegisteredClient registeredClient)
        implements RegisteredClientRepository {

        @Override
        public void save(RegisteredClient registeredClient) {
            throw new UnsupportedOperationException();
        }

        @Override
        @Nullable
        public RegisteredClient findById(String id) {
            return registeredClient.getId()
                .equals(id) ? registeredClient : null;
        }

        @Override
        @Nullable
        public RegisteredClient findByClientId(String clientId) {
            return registeredClient.getClientId()
                .equals(clientId) ? registeredClient : null;
        }
    }
}
