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

import com.bytechef.platform.oauth2.authorizationserver.Oauth2AuthorizationServerChainScopingIntTest.ChainScopingTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proves the authorization-server security chain is scoped to the authorization-server endpoints and does not shadow
 * the surrounding application's other security chains. A separate chain guards {@code /protected} with authentication;
 * if the authorization-server chain matched every request (as an unscoped highest-precedence permit-all chain would),
 * it would let an unauthenticated {@code /protected} request through with 200. Correctly scoped, the request falls
 * through to the guarding chain and is rejected, while the authorization-server's own metadata endpoint still responds.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ChainScopingTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class Oauth2AuthorizationServerChainScopingIntTest {

    @LocalServerPort
    private int port;

    @Test
    void testNonAuthorizationServerRequestIsNotShadowed() throws Exception {
        HttpResponse<String> httpResponse = get("/protected");

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testAuthorizationServerMetadataStillServed() throws Exception {
        HttpResponse<String> httpResponse = get("/.well-known/oauth-authorization-server");

        assertThat(httpResponse.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Configuration
    @Import(Oauth2AuthorizationServerIntTestConfiguration.class)
    static class ChainScopingTestConfiguration {

        @Bean
        @Order(1)
        SecurityFilterChain protectedResourceSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/protected")
                .authorizeHttpRequests(authorize -> authorize.anyRequest()
                    .authenticated())
                .httpBasic(Customizer.withDefaults());

            return http.build();
        }

        @RestController
        static class ProtectedController {

            @GetMapping("/protected")
            String protectedResource() {
                return "secret";
            }
        }
    }
}
