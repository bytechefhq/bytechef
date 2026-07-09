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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientFacadeIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RegisteredClientFacadeIntTestConfiguration.class,
    properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
class RegisteredClientFacadeIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RegisteredClientFacade registeredClientFacade;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate().update("DELETE FROM oauth2_authorization");
        jdbcTemplate().update("DELETE FROM oauth2_registered_client");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate().update("DELETE FROM oauth2_authorization");
        jdbcTemplate().update("DELETE FROM oauth2_registered_client");
    }

    @Test
    void testGetRegisteredClientsReturnsRegisteredClients() {
        insertRegisteredClient("client-1", "acme-client", "Acme Client");
        insertRegisteredClient("client-2", "beta-client", "Beta Client");

        List<RegisteredClientInfo> registeredClients = registeredClientFacade.getRegisteredClients();

        assertThat(registeredClients).hasSize(2);
        assertThat(registeredClients)
            .extracting(RegisteredClientInfo::clientId)
            .containsExactlyInAnyOrder("acme-client", "beta-client");

        RegisteredClientInfo acmeClient = registeredClients.stream()
            .filter(registeredClient -> "acme-client".equals(registeredClient.clientId()))
            .findFirst()
            .orElseThrow();

        assertThat(acmeClient.clientName()).isEqualTo("Acme Client");
        assertThat(acmeClient.authorizationGrantTypes()).contains("authorization_code");
        assertThat(acmeClient.scopes()).contains("mcp:automation");
        assertThat(acmeClient.redirectUris()).contains("https://client.example.com/callback");
    }

    @Test
    void testDeleteRegisteredClientRemovesClientAndRevokesAuthorizations() {
        insertRegisteredClient("client-1", "acme-client", "Acme Client");
        insertAuthorization("auth-1", "client-1");

        registeredClientFacade.deleteRegisteredClient("client-1");

        assertThat(registeredClientFacade.getRegisteredClients()).isEmpty();

        Integer authorizationCount = jdbcTemplate().queryForObject(
            "SELECT COUNT(*) FROM oauth2_authorization WHERE registered_client_id = ?", Integer.class, "client-1");

        assertThat(authorizationCount).isZero();
    }

    private void insertRegisteredClient(String id, String clientId, String clientName) {
        jdbcTemplate().update(
            """
                INSERT INTO oauth2_registered_client (
                    id, client_id, client_name, client_authentication_methods, authorization_grant_types,
                    redirect_uris, scopes, client_settings, token_settings)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            id, clientId, clientName, "client_secret_basic", "authorization_code",
            "https://client.example.com/callback", "mcp:automation", "{}", "{}");
    }

    private void insertAuthorization(String id, String registeredClientId) {
        jdbcTemplate().update(
            """
                INSERT INTO oauth2_authorization (
                    id, registered_client_id, principal_name, authorization_grant_type)
                VALUES (?, ?, ?, ?)""",
            id, registeredClientId, "user@localhost.com", "authorization_code");
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
