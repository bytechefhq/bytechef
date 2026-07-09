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

package com.bytechef.platform.oauth2.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that the Liquibase changelog for the embedded OAuth2 authorization server creates the three Spring
 * Authorization Server persistence tables. The authorization server itself does not need to be enabled for the schema
 * to be provisioned.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = Oauth2AuthorizationServerIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class PlatformOauth2AuthorizationServerSchemaIntTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testAuthorizationServerTablesExist() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tableNames)
            .contains("oauth2_registered_client", "oauth2_authorization", "oauth2_authorization_consent");
    }
}
