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
import com.bytechef.platform.oauth2.authorizationserver.domain.ClientRegistrationRequest;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.oauth2.authorizationserver.registration.AppClients;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantScope;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnOauth2AuthorizationServerEnabled
public class RegisteredClientServiceImpl implements RegisteredClientService {

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");
    private static final String MCP_SCOPE_PREFIX = "mcp:";
    private static final String PROGRAMMATIC_REGISTRATION_SETTING =
        "settings.client.bytechef.programmatic-registration";

    private static final String SELECT_CONSENT_CLIENT =
        "SELECT client_id, client_name, redirect_uris FROM oauth2_registered_client WHERE client_id = ?";

    private static final String SELECT_GRANT_COUNTS =
        """
            SELECT registered_client_id, COUNT(DISTINCT principal_name) AS grant_count
            FROM oauth2_authorization
            WHERE access_token_expires_at > CURRENT_TIMESTAMP OR refresh_token_expires_at > CURRENT_TIMESTAMP
            GROUP BY registered_client_id""";

    private static final String SELECT_REGISTERED_CLIENTS =
        """
            SELECT id, client_id, client_name, client_id_issued_at, scopes, authorization_grant_types, redirect_uris
            FROM oauth2_registered_client
            ORDER BY client_id_issued_at DESC""";

    private final JdbcOperations jdbcOperations;
    private final ObjectProvider<McpEndpoint> mcpEndpointProvider;
    private final RegisteredClientRepository registeredClientRepository;

    @SuppressFBWarnings("EI2")
    public RegisteredClientServiceImpl(
        JdbcOperations jdbcOperations, ObjectProvider<McpEndpoint> mcpEndpointProvider,
        RegisteredClientRepository registeredClientRepository) {

        this.jdbcOperations = jdbcOperations;
        this.mcpEndpointProvider = mcpEndpointProvider;
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    @Transactional
    public boolean deleteRegisteredClient(String id) {
        RegisteredClient registeredClient = TenantScope.call(
            TenantContext.DEFAULT_TENANT_ID, () -> registeredClientRepository.findById(id));

        if (registeredClient != null && isProgrammaticallyRegistered(registeredClient)) {
            throw new ConfigurationException(
                "OAuth2 client " + registeredClient.getClientId() + " belongs to an app instance; change the " +
                    "instance's sign-in or URL instead of deleting the client",
                RegisteredClientErrorType.REGISTERED_CLIENT_MANAGED_BY_SERVER);
        }

        jdbcOperations.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id);
        jdbcOperations.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", id);

        int deletedCount = TenantScope.call(
            TenantContext.DEFAULT_TENANT_ID,
            () -> jdbcOperations.update("DELETE FROM oauth2_registered_client WHERE id = ?", id));

        return deletedCount > 0;
    }

    @Override
    @Transactional
    public boolean deleteClient(String clientId) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        if (registeredClient == null) {
            return false;
        }

        if (!isProgrammaticallyRegistered(registeredClient)) {
            throw new ConfigurationException(
                "OAuth2 client " + clientId + " was not registered by the server and cannot be deleted this way",
                RegisteredClientErrorType.REGISTERED_CLIENT_NOT_DELETABLE);
        }

        String id = registeredClient.getId();

        jdbcOperations.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id);
        jdbcOperations.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", id);

        int deletedCount = TenantScope.call(
            TenantContext.DEFAULT_TENANT_ID,
            () -> jdbcOperations.update("DELETE FROM oauth2_registered_client WHERE id = ?", id));

        return deletedCount > 0;
    }

    @Override
    public Optional<String> fetchRegisteredClientId(String clientId) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        if (registeredClient == null) {
            return Optional.empty();
        }

        return Optional.of(registeredClient.getId());
    }

    @Override
    public Optional<OAuth2ConsentClient> fetchConsentClient(String clientId) {
        List<OAuth2ConsentClient> consentClients = TenantScope.call(
            TenantContext.DEFAULT_TENANT_ID,
            () -> jdbcOperations.query(
                SELECT_CONSENT_CLIENT, (resultSet, rowNum) -> mapConsentClient(resultSet), clientId));

        return consentClients.stream()
            .findFirst();
    }

    @Override
    public List<RegisteredClientInfo> getRegisteredClients(boolean deletable) {
        Map<String, Integer> grantCounts = getGrantCounts();

        return TenantScope.call(
            TenantContext.DEFAULT_TENANT_ID,
            () -> jdbcOperations.query(
                SELECT_REGISTERED_CLIENTS,
                (resultSet, rowNum) -> mapRegisteredClient(resultSet, grantCounts, deletable)));
    }

    @Override
    @Transactional
    public String registerClient(ClientRegistrationRequest clientRegistrationRequest) {
        validate(clientRegistrationRequest);

        String clientId = clientRegistrationRequest.clientId();

        if (registeredClientRepository.findByClientId(clientId) != null) {
            throw new ConfigurationException(
                "OAuth2 client id " + clientId + " is already registered",
                RegisteredClientErrorType.CLIENT_ID_ALREADY_REGISTERED);
        }

        String id = UUID.randomUUID()
            .toString();

        RegisteredClient registeredClient = RegisteredClient.withId(id)
            .clientId(clientId)
            .clientIdIssuedAt(Instant.now())
            .clientName(clientRegistrationRequest.clientName())
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUris(redirectUris -> redirectUris.addAll(clientRegistrationRequest.redirectUris()))
            .scopes(scopes -> scopes.addAll(clientRegistrationRequest.scopes()))
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(clientRegistrationRequest.requireAuthorizationConsent())
                .requireProofKey(true)
                .setting(PROGRAMMATIC_REGISTRATION_SETTING, true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .reuseRefreshTokens(false)
                .build())
            .build();

        registeredClientRepository.save(registeredClient);

        return id;
    }

    @Override
    @Transactional
    public void updateClient(ClientRegistrationRequest clientRegistrationRequest) {
        validate(clientRegistrationRequest);

        String clientId = clientRegistrationRequest.clientId();

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        if (registeredClient == null) {
            throw new ConfigurationException(
                "OAuth2 client " + clientId + " does not exist", RegisteredClientErrorType.REGISTERED_CLIENT_NOT_FOUND);
        }

        if (!isProgrammaticallyRegistered(registeredClient)) {
            throw new ConfigurationException(
                "OAuth2 client " + clientId + " was not registered by the server and cannot be updated",
                RegisteredClientErrorType.REGISTERED_CLIENT_NOT_UPDATABLE);
        }

        Set<String> registeredScopes = registeredClient.getScopes();
        Set<String> requestedScopes = clientRegistrationRequest.scopes();

        if (!registeredScopes.equals(requestedScopes)) {
            throw new ConfigurationException(
                "The scopes of OAuth2 client " + clientId + " cannot be changed: registered " +
                    toSortedList(registeredScopes) + ", requested " + toSortedList(requestedScopes),
                RegisteredClientErrorType.SCOPES_NOT_UPDATABLE);
        }

        ClientSettings clientSettings = registeredClient.getClientSettings();

        RegisteredClient updatedRegisteredClient = RegisteredClient.from(registeredClient)
            .clientName(clientRegistrationRequest.clientName())
            .redirectUris(redirectUris -> {
                redirectUris.clear();
                redirectUris.addAll(clientRegistrationRequest.redirectUris());
            })
            .clientSettings(ClientSettings.withSettings(clientSettings.getSettings())
                .requireAuthorizationConsent(clientRegistrationRequest.requireAuthorizationConsent())
                .build())
            .build();

        registeredClientRepository.save(updatedRegisteredClient);
    }

    private Set<String> getMcpScopes() {
        return mcpEndpointProvider.orderedStream()
            .map(McpEndpoint::getScope)
            .collect(Collectors.toSet());
    }

    private boolean isMcpScope(String scope, Set<String> mcpScopes) {
        return scope.startsWith(MCP_SCOPE_PREFIX) || mcpScopes.contains(scope);
    }

    private void validate(ClientRegistrationRequest clientRegistrationRequest) {
        List<String> redirectUris = clientRegistrationRequest.redirectUris();

        if (redirectUris.isEmpty()) {
            throw new ConfigurationException(
                "An OAuth2 client needs at least one redirect URI", RegisteredClientErrorType.INVALID_REDIRECT_URI);
        }

        for (String redirectUri : redirectUris) {
            validateRedirectUri(redirectUri);
        }

        Set<String> scopes = clientRegistrationRequest.scopes();

        if (AppClients.isAppScopeSet(scopes)) {
            Set<String> mcpScopes = getMcpScopes();

            List<String> mixedMcpScopes = scopes.stream()
                .filter(scope -> isMcpScope(scope, mcpScopes))
                .sorted()
                .toList();

            if (!mixedMcpScopes.isEmpty()) {
                throw new ConfigurationException(
                    "An OAuth2 client with the app scope cannot also hold the MCP scopes " + mixedMcpScopes,
                    RegisteredClientErrorType.INVALID_SCOPE);
            }
        }
    }

    private Map<String, Integer> getGrantCounts() {
        Map<String, Integer> grantCounts = new HashMap<>();

        jdbcOperations.query(
            SELECT_GRANT_COUNTS,
            resultSet -> {
                grantCounts.put(resultSet.getString("registered_client_id"), resultSet.getInt("grant_count"));
            });

        return grantCounts;
    }

    private static boolean isProgrammaticallyRegistered(RegisteredClient registeredClient) {
        ClientSettings clientSettings = registeredClient.getClientSettings();

        return Boolean.TRUE.equals(clientSettings.getSetting(PROGRAMMATIC_REGISTRATION_SETTING));
    }

    private static List<String> toSortedList(Set<String> values) {
        return values.stream()
            .sorted()
            .toList();
    }

    private static void validateRedirectUri(String redirectUri) {
        if (redirectUri.contains("*")) {
            throw createInvalidRedirectUriException(redirectUri);
        }

        URI uri;

        try {
            uri = new URI(redirectUri);
        } catch (URISyntaxException uriSyntaxException) {
            throw createInvalidRedirectUriException(redirectUri);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (!uri.isAbsolute() || scheme == null || host == null || host.isBlank() || uri.getRawFragment() != null) {
            throw createInvalidRedirectUriException(redirectUri);
        }

        String lowerCaseScheme = scheme.toLowerCase(Locale.ROOT);

        if (HTTPS_SCHEME.equals(lowerCaseScheme)) {
            return;
        }

        if (!HTTP_SCHEME.equals(lowerCaseScheme) || !LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            throw createInvalidRedirectUriException(redirectUri);
        }
    }

    private static ConfigurationException createInvalidRedirectUriException(String redirectUri) {
        return new ConfigurationException(
            "OAuth2 redirect URI " + redirectUri + " must be an absolute https URI, or http on a loopback host, " +
                "without a fragment or wildcard",
            RegisteredClientErrorType.INVALID_REDIRECT_URI);
    }

    private static OAuth2ConsentClient mapConsentClient(ResultSet resultSet) throws SQLException {
        List<String> redirectHosts = splitCommaSeparated(resultSet.getString("redirect_uris"))
            .stream()
            .map(RegisteredClientServiceImpl::getRedirectHost)
            .distinct()
            .toList();

        return new OAuth2ConsentClient(
            resultSet.getString("client_id"), resultSet.getString("client_name"), redirectHosts);
    }

    private static String getRedirectHost(String redirectUri) {
        try {
            URI uri = new URI(redirectUri);

            String host = uri.getHost();

            if (host != null) {
                return host;
            }

            String scheme = uri.getScheme();

            if (scheme != null) {
                return scheme + ":";
            }
        } catch (URISyntaxException uriSyntaxException) {
            return redirectUri;
        }

        return redirectUri;
    }

    private static RegisteredClientInfo mapRegisteredClient(
        ResultSet resultSet, Map<String, Integer> grantCounts, boolean deletable) throws SQLException {

        String id = resultSet.getString("id");
        Timestamp clientIdIssuedAt = resultSet.getTimestamp("client_id_issued_at");

        return new RegisteredClientInfo(
            id, resultSet.getString("client_id"), resultSet.getString("client_name"),
            clientIdIssuedAt == null ? null : clientIdIssuedAt.toInstant(),
            splitCommaSeparated(resultSet.getString("scopes")),
            splitCommaSeparated(resultSet.getString("authorization_grant_types")),
            splitCommaSeparated(resultSet.getString("redirect_uris")), grantCounts.getOrDefault(id, 0), deletable);
    }

    private static List<String> splitCommaSeparated(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }
}
