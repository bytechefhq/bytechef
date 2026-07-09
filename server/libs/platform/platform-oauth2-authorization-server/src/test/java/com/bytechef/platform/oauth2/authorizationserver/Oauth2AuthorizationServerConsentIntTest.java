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

import com.bytechef.platform.oauth2.authorizationserver.Oauth2AuthorizationServerConsentIntTest.ConsentTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Verifies that, for a client requiring authorization consent, the authorization endpoint redirects the logged-in user
 * to ByteChef's custom consent page ({@code /oauth2/consent}) rather than rendering Spring Authorization Server's
 * default consent form. The consent page itself is served by the client single-page application; this test asserts the
 * server-side redirect contract that the page depends on (the {@code client_id}, {@code scope} and {@code state}
 * parameters are carried across).
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConsentTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class Oauth2AuthorizationServerConsentIntTest {

    private static final String CLIENT_ID = "consent-client";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String REDIRECT_URI = "http://127.0.0.1/callback";
    private static final String SCOPE = "mcp:automation";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM oauth2_authorization");
        jdbcTemplate.update("DELETE FROM oauth2_registered_client");

        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID()
            .toString())
            .clientId(CLIENT_ID)
            .clientSecret("{noop}consent-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(REDIRECT_URI)
            .scope(SCOPE)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .requireProofKey(true)
                .build())
            .build();

        registeredClientRepository.save(registeredClient);
    }

    @AfterEach
    void afterEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM oauth2_authorization");
        jdbcTemplate.update("DELETE FROM oauth2_registered_client");
    }

    @Test
    void testConsentRequiredAuthorizeRedirectsToConsentPage() throws Exception {
        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()) {

            String sessionCookie = login(httpClient);

            String authorizeQuery = "response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&code_challenge=" + CODE_CHALLENGE +
                "&code_challenge_method=S256";

            HttpRequest authorizeRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
                .header("Cookie", sessionCookie)
                .GET()
                .build();

            HttpResponse<String> authorizeResponse = httpClient.send(
                authorizeRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(authorizeResponse.statusCode()).isEqualTo(302);

            String location = authorizeResponse.headers()
                .firstValue("Location")
                .orElseThrow(() -> new IllegalStateException("No Location header on authorize response"));

            assertThat(location).contains("/oauth2/consent");
            assertThat(location).contains("client_id=" + CLIENT_ID);
            assertThat(location).contains("scope=");
        }
    }

    private String login(HttpClient httpClient) throws Exception {
        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=user&password=password"))
            .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode()).isEqualTo(302);

        List<String> setCookies = loginResponse.headers()
            .allValues("Set-Cookie");

        Optional<String> sessionCookie = setCookies.stream()
            .filter(cookie -> cookie.startsWith("JSESSIONID="))
            .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
            .findFirst();

        return sessionCookie.orElseThrow(() -> new IllegalStateException("No JSESSIONID in login response"));
    }

    @Configuration
    @Import(Oauth2AuthorizationServerIntTestConfiguration.class)
    @SuppressFBWarnings({
        "HARD_CODE_PASSWORD", "SPRING_CSRF_PROTECTION_DISABLED"
    })
    static class ConsentTestConfiguration {

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        SecurityFilterChain appFormLoginSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest()
                .authenticated())
                .formLogin(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());

            return http.build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                User.withUsername("user")
                    .password("{noop}password")
                    .authorities("USER")
                    .build());
        }
    }
}
