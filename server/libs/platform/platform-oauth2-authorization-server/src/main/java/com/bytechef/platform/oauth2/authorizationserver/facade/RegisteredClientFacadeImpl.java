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

package com.bytechef.platform.oauth2.authorizationserver.facade;

import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and deletes registered OAuth2 clients directly against the Spring Authorization Server persistence tables:
 * {@code JdbcRegisteredClientRepository} exposes no list-all or delete operation, so this facade queries
 * {@code oauth2_registered_client} for the admin listing and, on delete, first removes the client's rows from
 * {@code oauth2_authorization} (revoking its tokens) and then the client itself.
 *
 * @author Ivica Cardic
 */
@Service
public class RegisteredClientFacadeImpl implements RegisteredClientFacade {

    private static final String SELECT_REGISTERED_CLIENTS =
        """
            SELECT id, client_id, client_name, client_id_issued_at, scopes, authorization_grant_types, redirect_uris
            FROM oauth2_registered_client
            ORDER BY client_id_issued_at DESC""";

    private final JdbcOperations jdbcOperations;

    @SuppressFBWarnings("EI2")
    public RegisteredClientFacadeImpl(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional
    public void deleteRegisteredClient(String id) {
        jdbcOperations.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id);
        jdbcOperations.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<RegisteredClientInfo> getRegisteredClients() {
        return jdbcOperations.query(SELECT_REGISTERED_CLIENTS, (resultSet, rowNum) -> mapRegisteredClient(resultSet));
    }

    private static RegisteredClientInfo mapRegisteredClient(ResultSet resultSet) throws SQLException {
        Timestamp clientIdIssuedAt = resultSet.getTimestamp("client_id_issued_at");

        return new RegisteredClientInfo(
            resultSet.getString("id"), resultSet.getString("client_id"), resultSet.getString("client_name"),
            clientIdIssuedAt == null ? null : clientIdIssuedAt.toInstant(),
            splitCommaSeparated(resultSet.getString("scopes")),
            splitCommaSeparated(resultSet.getString("authorization_grant_types")),
            splitCommaSeparated(resultSet.getString("redirect_uris")));
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
