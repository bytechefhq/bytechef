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
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantScope;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and deletes registered OAuth2 clients directly against the Spring Authorization Server tables:
 * {@code JdbcRegisteredClientRepository} exposes no list-all or delete operation. Clients live in the shared default
 * tenant; authorizations and consents live in the current tenant.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnOauth2AuthorizationServerEnabled
public class RegisteredClientServiceImpl implements RegisteredClientService {

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
    private final TenantService tenantService;

    @SuppressFBWarnings("EI2")
    public RegisteredClientServiceImpl(JdbcOperations jdbcOperations, TenantService tenantService) {
        this.jdbcOperations = jdbcOperations;
        this.tenantService = tenantService;
    }

    @Override
    @Transactional
    public void deleteRegisteredClient(String id) {
        if (tenantService.isMultiTenantEnabled()) {
            throw new ConfigurationException(
                "OAuth2 clients are shared across tenants and cannot be deleted",
                RegisteredClientErrorType.DELETE_SHARED_REGISTERED_CLIENT);
        }

        jdbcOperations.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id);
        jdbcOperations.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", id);

        TenantScope.run(
            TenantContext.DEFAULT_TENANT_ID,
            () -> jdbcOperations.update("DELETE FROM oauth2_registered_client WHERE id = ?", id));
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
    public List<RegisteredClientInfo> getRegisteredClients() {
        boolean multiTenantEnabled = tenantService.isMultiTenantEnabled();
        Map<String, Integer> grantCounts = getGrantCounts();

        List<RegisteredClientInfo> registeredClients = TenantScope.call(
            TenantContext.DEFAULT_TENANT_ID,
            () -> jdbcOperations.query(
                SELECT_REGISTERED_CLIENTS,
                (resultSet, rowNum) -> mapRegisteredClient(resultSet, grantCounts, !multiTenantEnabled)));

        if (!multiTenantEnabled) {
            return registeredClients;
        }

        return registeredClients.stream()
            .filter(registeredClient -> registeredClient.grantCount() > 0)
            .toList();
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
