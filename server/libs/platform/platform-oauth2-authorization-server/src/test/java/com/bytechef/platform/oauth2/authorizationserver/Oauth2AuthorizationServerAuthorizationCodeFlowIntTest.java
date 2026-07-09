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

import com.bytechef.platform.oauth2.authorizationserver.Oauth2AuthorizationServerAuthorizationCodeFlowIntTest.AuthorizationCodeFlowTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerIntTestConfiguration;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * Drives the full OAuth2 authorization-code flow with PKCE end-to-end against the embedded authorization server, with
 * end-user login delegated to a separate, co-hosted form-login chain that stands in for ByteChef's existing
 * {@code SecurityConfiguration}. A passing run proves the authorization server reuses the application's login session
 * (no second user store): the user authenticates once against the form-login chain, and the authorize endpoint then
 * issues an authorization code for that same session, which is exchanged for a JWT whose {@code sub} claim is the
 * logged-in user.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AuthorizationCodeFlowTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class Oauth2AuthorizationServerAuthorizationCodeFlowIntTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String CODE_VERIFIER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01";
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
            .clientSecret("{noop}" + CLIENT_SECRET)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri(REDIRECT_URI)
            .scope(SCOPE)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
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
    void testAuthorizationCodeWithPkceFlow() throws Exception {
        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()) {

            String sessionCookie = login(httpClient);

            String codeChallenge = createCodeChallenge();

            String authorizeQuery = "response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&code_challenge=" + codeChallenge +
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

            assertThat(location).startsWith(REDIRECT_URI + "?code=");

            String authorizationCode = extractQueryParameter(location, "code");

            String tokenBody = "grant_type=authorization_code" +
                "&code=" + authorizationCode +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&code_verifier=" + CODE_VERIFIER;

            String basicAuth = Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(tokenResponse.statusCode()).isEqualTo(200);

            String tokenResponseBody = tokenResponse.body();

            assertThat(tokenResponseBody).contains("\"access_token\"");
            assertThat(tokenResponseBody).contains("\"token_type\":\"Bearer\"");

            String accessToken = extractJsonStringField(tokenResponseBody, "access_token");
            String subject = extractJwtSubject(accessToken);

            assertThat(subject).isEqualTo("user");

            String tenantId = extractJwtClaim(accessToken, "tenant_id");

            assertThat(tenantId).isEqualTo("public");
        }
    }

    @Test
    void testTokenTenantIdIsDerivedFromResourceParameter() throws Exception {
        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()) {

            String sessionCookie = login(httpClient);

            String codeChallenge = createCodeChallenge();

            String resource = "http://localhost:" + port + "/api/embedded/" + TenantKey.of("acme") + "/mcp";

            String authorizeQuery = "response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256" +
                "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8);

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

            String authorizationCode = extractQueryParameter(location, "code");

            String tokenBody = "grant_type=authorization_code" +
                "&code=" + authorizationCode +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&code_verifier=" + CODE_VERIFIER +
                "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8);

            String basicAuth = Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(tokenResponse.statusCode()).isEqualTo(200);

            String accessToken = extractJsonStringField(tokenResponse.body(), "access_token");

            String tenantId = extractJwtClaim(accessToken, "tenant_id");

            assertThat(tenantId).isEqualTo("acme");
        }
    }

    @Test
    void testTokenAudienceIsDerivedFromResourceParameter() throws Exception {
        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()) {

            String sessionCookie = login(httpClient);

            String codeChallenge = createCodeChallenge();

            String resource = "http://localhost:" + port + "/api/embedded/" + TenantKey.of("acme") + "/mcp";

            String authorizeQuery = "response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256" +
                "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8);

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

            String authorizationCode = extractQueryParameter(location, "code");

            String tokenBody = "grant_type=authorization_code" +
                "&code=" + authorizationCode +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&code_verifier=" + CODE_VERIFIER +
                "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8);

            String basicAuth = Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(tokenResponse.statusCode()).isEqualTo(200);

            String accessToken = extractJsonStringField(tokenResponse.body(), "access_token");

            String payload = extractJwtPayload(accessToken);

            assertThat(payload).contains("\"aud\"");
            assertThat(payload).contains(resource);
        }
    }

    @Test
    void testAuthorizeWithoutSessionRedirectsToLogin() throws Exception {
        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()) {

            String codeChallenge = createCodeChallenge();

            String authorizeQuery = "response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256";

            HttpRequest authorizeRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
                .header("Accept", "text/html")
                .GET()
                .build();

            HttpResponse<String> authorizeResponse = httpClient.send(
                authorizeRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(authorizeResponse.statusCode()).isEqualTo(302);

            String location = authorizeResponse.headers()
                .firstValue("Location")
                .orElseThrow(() -> new IllegalStateException("No Location header on unauthenticated authorize"));

            assertThat(location).endsWith("/");
        }
    }

    private String login(HttpClient httpClient) throws Exception {
        String loginBody = "username=user&password=password";

        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode()).isEqualTo(302);
        assertThat(loginResponse.headers()
            .firstValue("Location")
            .orElseThrow(() -> new IllegalStateException("No Location header on login response"))).endsWith("/");

        List<String> setCookies = loginResponse.headers()
            .allValues("Set-Cookie");

        Optional<String> sessionCookie = setCookies.stream()
            .filter(cookie -> cookie.startsWith("JSESSIONID="))
            .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
            .findFirst();

        return sessionCookie.orElseThrow(() -> new IllegalStateException("No JSESSIONID in login response"));
    }

    private static String createCodeChallenge() throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        byte[] digest = messageDigest.digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII));

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(digest);
    }

    private static String extractQueryParameter(String url, String parameterName) {
        Pattern pattern = Pattern.compile("[?&]" + Pattern.quote(parameterName) + "=([^&]+)");

        Matcher matcher = pattern.matcher(url);

        if (!matcher.find()) {
            throw new IllegalStateException("No " + parameterName + " parameter in URL: " + url);
        }

        return matcher.group(1);
    }

    private static String extractJsonStringField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"");

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalStateException("No " + fieldName + " field in JSON: " + json);
        }

        return matcher.group(1);
    }

    private static String extractJwtSubject(String jwt) {
        return extractJwtClaim(jwt, "sub");
    }

    private static String extractJwtClaim(String jwt, String claim) {
        return extractJsonStringField(extractJwtPayload(jwt), claim);
    }

    private static String extractJwtPayload(String jwt) {
        String[] parts = jwt.split("\\.");

        if (parts.length < 2) {
            throw new IllegalStateException("Malformed JWT: " + jwt);
        }

        return new String(Base64.getUrlDecoder()
            .decode(parts[1]), StandardCharsets.UTF_8);
    }

    @Configuration
    @Import(Oauth2AuthorizationServerIntTestConfiguration.class)
    @SuppressFBWarnings({
        "HARD_CODE_PASSWORD", "SPRING_CSRF_PROTECTION_DISABLED"
    })
    static class AuthorizationCodeFlowTestConfiguration {

        /**
         * The application's form-login chain that stands in for ByteChef's {@code SecurityConfiguration}. It has no
         * {@code securityMatcher}, so it is the fallback for every request that the higher-precedence, scoped
         * authorization-server chain does not match. CSRF is disabled to keep the test's login POST free of token
         * juggling.
         */
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
