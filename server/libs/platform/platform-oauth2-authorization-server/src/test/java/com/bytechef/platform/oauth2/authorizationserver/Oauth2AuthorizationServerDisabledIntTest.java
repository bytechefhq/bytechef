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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Proves that, with the {@code bytechef.oauth2.authorization-server.enabled} flag off (the default), the authorization
 * server metadata endpoint is not exposed and returns a 404.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = Oauth2AuthorizationServerIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class Oauth2AuthorizationServerDisabledIntTest {

    @LocalServerPort
    private int port;

    @Test
    void testMetadataEndpointIsNotExposed() throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/.well-known/oauth-authorization-server"))
            .GET()
            .build();

        HttpResponse<String> httpResponse;

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }

        assertThat(httpResponse.statusCode()).isEqualTo(404);
    }
}
