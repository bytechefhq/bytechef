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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proves that, with {@code bytechef.oauth2.authorization-server.enabled=true}, Spring Boot 4.0.7 actually hosts the
 * Spring Authorization Server: the metadata endpoint {@code /.well-known/oauth-authorization-server} returns issuer
 * metadata and Dynamic Client Registration (DCR) persists a registered client.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = Oauth2AuthorizationServerIntTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class Oauth2AuthorizationServerEnabledIntTest {

    @Autowired
    private DataSource dataSource;

    @LocalServerPort
    private int port;

    @Test
    void testMetadataEndpointReturnsIssuerAndRegistrationEndpoint() throws Exception {
        HttpResponse<String> httpResponse = getMetadata();

        assertThat(httpResponse.statusCode()).isEqualTo(200);

        String body = httpResponse.body();

        assertThat(body).contains("\"issuer\"");
        assertThat(body).contains("\"registration_endpoint\"");
    }

    @Test
    void testDynamicClientRegistrationRegistersClient() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM oauth2_registered_client");

        String registrationEndpoint = extractRegistrationEndpoint(getMetadata().body());

        String registrationRequest = """
            {"client_name":"test-client","redirect_uris":["https://client.example.com/callback"],\
            "grant_types":["authorization_code"],"response_types":["code"],\
            "token_endpoint_auth_method":"client_secret_basic"}""";

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(registrationEndpoint))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registrationRequest))
            .build();

        HttpResponse<String> httpResponse;

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }

        assertThat(httpResponse.statusCode()).isEqualTo(201);
        assertThat(httpResponse.body()).contains("client_id");

        Integer registeredClientCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM oauth2_registered_client", Integer.class);

        assertThat(registeredClientCount).isEqualTo(1);
    }

    private HttpResponse<String> getMetadata() throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/.well-known/oauth-authorization-server"))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }
    }

    private static String extractRegistrationEndpoint(String metadataBody) {
        Pattern pattern = Pattern.compile("\"registration_endpoint\"\\s*:\\s*\"([^\"]+)\"");

        Matcher matcher = pattern.matcher(metadataBody);

        if (!matcher.find()) {
            throw new IllegalStateException("No registration_endpoint in metadata: " + metadataBody);
        }

        return matcher.group(1);
    }
}
