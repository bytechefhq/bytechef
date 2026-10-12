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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.ACME_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.GLOBEX_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.TENANT_IDS;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.getDatabaseSchema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RegisteredClientIntTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee", "spring.liquibase.contexts=mono"
    })
@Import({
    MultiTenantSchemaIntTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
class SharedRegisteredClientRepositoryIntTest {

    private static final String CLIENT_ID = "shared-repository-client";
    private static final String PUBLIC_SCHEMA = TenantContext.DEFAULT_TENANT_ID;
    private static final String SHADOWED_CLIENT_ID = "tenant-shadowed-client";

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private McpServerService mcpServerService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @MockitoBean
    private TenantService tenantService;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        deleteAllRows();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        for (String tenantId : TENANT_IDS) {
            jdbcTemplate.execute(
                "CREATE TABLE " + getDatabaseSchema(tenantId) + ".oauth2_registered_client (LIKE " + PUBLIC_SCHEMA +
                    ".oauth2_registered_client INCLUDING ALL)");
        }
    }

    @AfterEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    void afterEach() {
        for (String tenantId : TENANT_IDS) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + getDatabaseSchema(tenantId) + ".oauth2_registered_client");
        }

        deleteAllRows();

        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testAllOperationsRunInDefaultTenant() {
        TenantContext.setCurrentTenantId(ACME_TENANT_ID);

        registeredClientRepository.save(createRegisteredClient(CLIENT_ID));

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(ACME_TENANT_ID);
        assertThat(countRegisteredClients(PUBLIC_SCHEMA)).isEqualTo(1);
        assertThat(countRegisteredClients(getDatabaseSchema(ACME_TENANT_ID))).isZero();

        TenantScope.run(TenantContext.DEFAULT_TENANT_ID, () -> registeredClientRepository.save(
            createRegisteredClient(SHADOWED_CLIENT_ID)));

        copyRegisteredClientIntoTenantSchema(SHADOWED_CLIENT_ID, ACME_TENANT_ID);

        jdbcTemplate.update(
            "DELETE FROM " + PUBLIC_SCHEMA + ".oauth2_registered_client WHERE id = ?", SHADOWED_CLIENT_ID);

        for (String tenantId : TENANT_IDS) {
            TenantContext.setCurrentTenantId(tenantId);

            RegisteredClient foundById = registeredClientRepository.findById(CLIENT_ID);
            RegisteredClient foundByClientId = registeredClientRepository.findByClientId(CLIENT_ID + "-client-id");

            assertThat(foundById).isNotNull();
            assertThat(foundById.getClientId()).isEqualTo(CLIENT_ID + "-client-id");
            assertThat(foundByClientId).isNotNull();
            assertThat(foundByClientId.getId()).isEqualTo(CLIENT_ID);
            assertThat(registeredClientRepository.findById(SHADOWED_CLIENT_ID)).isNull();
            assertThat(registeredClientRepository.findByClientId(SHADOWED_CLIENT_ID + "-client-id")).isNull();
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
        }
    }

    private static RegisteredClient createRegisteredClient(String id) {
        return RegisteredClient.withId(id)
            .clientId(id + "-client-id")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/cb")
            .build();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void copyRegisteredClientIntoTenantSchema(String id, String tenantId) {
        jdbcTemplate.update(
            "INSERT INTO " + getDatabaseSchema(tenantId) + ".oauth2_registered_client SELECT * FROM " + PUBLIC_SCHEMA +
                ".oauth2_registered_client WHERE id = ?",
            id);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private int countRegisteredClients(String schema) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + schema + ".oauth2_registered_client", Integer.class);

        return count == null ? 0 : count;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void deleteAllRows() {
        for (String schema : List.of(
            PUBLIC_SCHEMA, getDatabaseSchema(ACME_TENANT_ID), getDatabaseSchema(GLOBEX_TENANT_ID))) {

            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization_consent");
            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization");
        }

        jdbcTemplate.update("DELETE FROM " + PUBLIC_SCHEMA + ".oauth2_registered_client");
    }
}
