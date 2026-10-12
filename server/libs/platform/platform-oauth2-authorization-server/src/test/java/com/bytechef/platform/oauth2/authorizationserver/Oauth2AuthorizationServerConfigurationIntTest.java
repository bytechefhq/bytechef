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

import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.ACME_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.GLOBEX_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.getDatabaseSchema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.facade.RegisteredClientFacade;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import com.bytechef.platform.oauth2.authorizationserver.tenant.AuthorizationServerTokenRevocationChecker;
import com.bytechef.platform.oauth2.authorizationserver.tenant.SessionTenantFilter;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantKeyedTokenValues;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantRoutingOAuth2AuthorizationService;
import com.bytechef.platform.oauth2.authorizationserver.web.graphql.RegisteredClientGraphQlController;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Integration tests of
 * {@link com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerConfiguration}, one nested
 * class per application context.
 *
 * @author Ivica Cardic
 */
class Oauth2AuthorizationServerConfigurationIntTest {

    /**
     * Drives the full OAuth2 authorization-code flow with PKCE end-to-end against the embedded authorization server,
     * with end-user login delegated to a separate, co-hosted form-login chain that stands in for ByteChef's existing
     * {@code SecurityConfiguration}. A passing run proves the authorization server reuses the application's login
     * session (no second user store): the user authenticates once against the form-login chain, and the authorize
     * endpoint then issues an authorization code for that same session, which is exchanged for a JWT whose {@code sub}
     * claim is the logged-in user.
     *
     * @author Ivica Cardic
     */
    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = AuthorizationCodeFlow.AuthorizationCodeFlowTestConfiguration.class,
        properties = {
            "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee",
            "bytechef.public-url=https://bytechef.example.test/"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Import(PostgreSQLContainerConfiguration.class)
    class AuthorizationCodeFlow {

        private static final String CLIENT_ID = "test-client";
        private static final String CLIENT_SECRET = "test-secret";
        private static final String CODE_VERIFIER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01";
        private static final String ISSUER = "https://bytechef.example.test";
        private static final String REDIRECT_URI = "http://127.0.0.1/callback";
        private static final String SCOPE = "mcp:automation";
        private static final String SESSION_TENANT_ID = "acme";
        private static final String SESSION_TENANT_PATH = "/test/session-tenant";

        @Autowired
        private AuthorizationServerTokenRevocationChecker authorizationServerTokenRevocationChecker;

        @Autowired
        private DataSource dataSource;

        @Autowired
        private RegisteredClientRepository registeredClientRepository;

        @Autowired
        private TenantRoutingOAuth2AuthorizationService tenantRoutingOAuth2AuthorizationService;

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

                HttpResponse<String> tokenResponse = sendTokenRequest(httpClient, tokenBody);

                assertThat(tokenResponse.statusCode()).isEqualTo(200);

                String tokenResponseBody = tokenResponse.body();

                assertThat(tokenResponseBody).contains("\"access_token\"");
                assertThat(tokenResponseBody).contains("\"token_type\":\"Bearer\"");

                String accessToken = extractJsonStringField(tokenResponseBody, "access_token");
                String subject = extractJwtSubject(accessToken);

                assertThat(subject).isEqualTo("user");
                assertThat(extractJwtPayload(accessToken)).contains("\"authorities\":[\"USER\"]")
                    .doesNotContain("FACTOR_");

                String tenantId = extractJwtClaim(accessToken, "tenant_id");

                assertThat(tenantId).isEqualTo("public");
                assertThat(TenantKeyedTokenValues.parseTenantId(authorizationCode))
                    .contains(TenantContext.DEFAULT_TENANT_ID);

                String refreshToken = extractJsonStringField(tokenResponseBody, "refresh_token");

                assertThat(TenantKeyedTokenValues.parseTenantId(refreshToken))
                    .contains(TenantContext.DEFAULT_TENANT_ID);
            }
        }

        @Test
        void testSessionTenantIsCarriedByCodeRefreshTokenAndAccessToken() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = login(httpClient);

                HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
                    .uri(
                        URI.create("http://localhost:" + port + SESSION_TENANT_PATH + "?tenantId=" + SESSION_TENANT_ID))
                    .header("Cookie", sessionCookie)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                HttpResponse<String> sessionTenantResponse = httpClient.send(
                    sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

                assertThat(sessionTenantResponse.statusCode()).isEqualTo(204);

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

                String authorizationCode = extractQueryParameter(location, "code");

                assertThat(TenantKeyedTokenValues.parseTenantId(authorizationCode)).contains(SESSION_TENANT_ID);

                HttpResponse<String> tokenResponse = sendTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=" + authorizationCode +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER);

                assertThat(tokenResponse.statusCode()).isEqualTo(200);

                String accessToken = extractJsonStringField(tokenResponse.body(), "access_token");
                String refreshToken = extractJsonStringField(tokenResponse.body(), "refresh_token");

                assertThat(extractJwtClaim(accessToken, "tenant_id")).isEqualTo(SESSION_TENANT_ID);
                assertThat(TenantKeyedTokenValues.parseTenantId(refreshToken)).contains(SESSION_TENANT_ID);

                HttpResponse<String> refreshResponse = sendTokenRequest(
                    httpClient, "grant_type=refresh_token&refresh_token=" + refreshToken);

                assertThat(refreshResponse.statusCode()).isEqualTo(200);

                String refreshedAccessToken = extractJsonStringField(refreshResponse.body(), "access_token");

                assertThat(extractJwtClaim(refreshedAccessToken, "tenant_id")).isEqualTo(SESSION_TENANT_ID);
                assertThat(extractJwtPayload(refreshedAccessToken)).contains("\"authorities\":[\"USER\"]")
                    .doesNotContain("FACTOR_");

                HttpResponse<String> injectedCodeRefreshResponse = sendTokenRequest(
                    httpClient,
                    "grant_type=refresh_token&refresh_token=" + refreshToken +
                        "&code=" + TenantKeyedTokenValues.generate("other"));

                assertThat(injectedCodeRefreshResponse.statusCode()).isEqualTo(200);

                String injectedCodeAccessToken = extractJsonStringField(
                    injectedCodeRefreshResponse.body(), "access_token");
                String injectedCodeRefreshToken = extractJsonStringField(
                    injectedCodeRefreshResponse.body(), "refresh_token");

                assertThat(extractJwtClaim(injectedCodeAccessToken, "tenant_id")).isEqualTo(SESSION_TENANT_ID);
                assertThat(TenantKeyedTokenValues.parseTenantId(injectedCodeRefreshToken)).contains(SESSION_TENANT_ID);
            }
        }

        @Test
        void testRevokedAuthorizationIsReportedRevoked() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = login(httpClient);

                HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
                    .uri(
                        URI.create("http://localhost:" + port + SESSION_TENANT_PATH + "?tenantId=" + SESSION_TENANT_ID))
                    .header("Cookie", sessionCookie)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                httpClient.send(sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

                String authorizeQuery = "response_type=code" +
                    "&client_id=" + CLIENT_ID +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                    "&code_challenge=" + createCodeChallenge() +
                    "&code_challenge_method=S256";

                HttpRequest authorizeRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
                    .header("Cookie", sessionCookie)
                    .GET()
                    .build();

                HttpResponse<String> authorizeResponse = httpClient.send(
                    authorizeRequest, HttpResponse.BodyHandlers.ofString());

                String location = authorizeResponse.headers()
                    .firstValue("Location")
                    .orElseThrow(() -> new IllegalStateException("No Location header on authorize response"));

                HttpResponse<String> tokenResponse = sendTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=" + extractQueryParameter(location, "code") +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER);

                assertThat(tokenResponse.statusCode()).isEqualTo(200);

                String accessToken = extractJsonStringField(tokenResponse.body(), "access_token");

                assertThat(authorizationServerTokenRevocationChecker.isRevoked(accessToken, SESSION_TENANT_ID))
                    .isFalse();

                OAuth2Authorization authorization = tenantRoutingOAuth2AuthorizationService.findByAccessToken(
                    accessToken, SESSION_TENANT_ID);

                assertThat(authorization).isNotNull();

                tenantRoutingOAuth2AuthorizationService.remove(authorization);

                assertThat(authorizationServerTokenRevocationChecker.isRevoked(accessToken, SESSION_TENANT_ID))
                    .isTrue();
            }
        }

        @Test
        void testClientRevocationOfAccessTokenInvalidatesItInTheTokenTenant() throws Exception {
            String accessToken = requestAccessTokenWithMcpResource(SESSION_TENANT_ID);

            assertThat(authorizationServerTokenRevocationChecker.isRevoked(accessToken, SESSION_TENANT_ID))
                .isFalse();

            HttpResponse<String> revokeResponse;

            try (HttpClient httpClient = HttpClient.newHttpClient()) {
                revokeResponse = sendClientAuthenticatedRequest(
                    httpClient, "/oauth2/revoke", "token=" + accessToken + "&token_type_hint=access_token");
            }

            assertThat(revokeResponse.statusCode()).isEqualTo(200);

            OAuth2Authorization authorization = tenantRoutingOAuth2AuthorizationService.findByAccessToken(
                accessToken, SESSION_TENANT_ID);

            assertThat(authorization).isNotNull();
            assertThat(authorization.getAccessToken()
                .isInvalidated()).isTrue();
            assertThat(authorizationServerTokenRevocationChecker.isRevoked(accessToken, SESSION_TENANT_ID)).isTrue();
        }

        @Test
        void testRefreshAfterRevocationIsRejectedAndRecreatesNoAuthorization() throws Exception {
            String tokenResponseBody = requestTokenResponseBody();

            String refreshToken = extractJsonStringField(tokenResponseBody, "refresh_token");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            jdbcTemplate.update("DELETE FROM oauth2_authorization");

            HttpResponse<String> refreshResponse;

            try (HttpClient httpClient = HttpClient.newHttpClient()) {
                refreshResponse =
                    sendTokenRequest(httpClient, "grant_type=refresh_token&refresh_token=" + refreshToken);
            }

            assertThat(refreshResponse.statusCode()).isEqualTo(400);
            assertThat(refreshResponse.body()).contains("invalid_grant");
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_authorization", Integer.class))
                .isZero();
        }

        @Test
        void testRefreshTokenRekeyedToAnotherTenantIsRejected() throws Exception {
            String refreshToken = extractJsonStringField(requestTokenResponseBody(), "refresh_token");

            HttpResponse<String> refreshResponse;

            try (HttpClient httpClient = HttpClient.newHttpClient()) {
                refreshResponse = sendTokenRequest(
                    httpClient, "grant_type=refresh_token&refresh_token=" + rekeyTokenValue(refreshToken, "globex"));
            }

            assertThat(refreshResponse.statusCode()).isEqualTo(400);
            assertThat(refreshResponse.body()).contains("invalid_grant");
        }

        @Test
        void testAuthorizationCodeRekeyedToAnotherTenantIsRejected() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = login(httpClient);

                HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
                    .uri(
                        URI.create("http://localhost:" + port + SESSION_TENANT_PATH + "?tenantId=" + SESSION_TENANT_ID))
                    .header("Cookie", sessionCookie)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                httpClient.send(sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

                String authorizeQuery = "response_type=code" +
                    "&client_id=" + CLIENT_ID +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                    "&code_challenge=" + createCodeChallenge() +
                    "&code_challenge_method=S256";

                HttpRequest authorizeRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
                    .header("Cookie", sessionCookie)
                    .GET()
                    .build();

                HttpResponse<String> authorizeResponse = httpClient.send(
                    authorizeRequest, HttpResponse.BodyHandlers.ofString());

                String location = authorizeResponse.headers()
                    .firstValue("Location")
                    .orElseThrow(() -> new IllegalStateException("No Location header on authorize response"));

                String forgedCode = rekeyTokenValue(extractQueryParameter(location, "code"), "globex");

                HttpResponse<String> tokenResponse = sendTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=" + forgedCode +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER);

                assertThat(tokenResponse.statusCode()).isEqualTo(400);
                assertThat(tokenResponse.body()).contains("invalid_grant");
            }
        }

        @Test
        void testUnkeyedAuthorizationCodeIsRejectedAsInvalidGrant() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                HttpResponse<String> codeResponse = sendTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=legacy-opaque-value" +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER);

                assertThat(codeResponse.statusCode()).isEqualTo(400);
                assertThat(codeResponse.body()).contains("invalid_grant");

                HttpResponse<String> refreshResponse = sendTokenRequest(
                    httpClient, "grant_type=refresh_token&refresh_token=eyJhbGciOiJSUzI1NiJ9.e30.sig");

                assertThat(refreshResponse.statusCode()).isEqualTo(400);
                assertThat(refreshResponse.body()).contains("invalid_grant");
            }
        }

        @Test
        void testIssuerIsPinnedToPublicUrl() throws Exception {
            HttpRequest metadataRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/.well-known/oauth-authorization-server"))
                .GET()
                .build();

            HttpResponse<String> metadataResponse;

            try (HttpClient httpClient = HttpClient.newHttpClient()) {
                metadataResponse = httpClient.send(metadataRequest, HttpResponse.BodyHandlers.ofString());
            }

            assertThat(metadataResponse.statusCode()).isEqualTo(200);
            assertThat(extractJsonStringField(metadataResponse.body(), "issuer")).isEqualTo(ISSUER);

            String accessToken = requestAccessTokenWithMcpResource(SESSION_TENANT_ID);

            assertThat(extractJwtClaim(accessToken, "iss")).isEqualTo(ISSUER);
        }

        @Test
        void testTokenTenantIdIsSessionTenantWhenResourceNamesTheSameTenant() throws Exception {
            String accessToken = requestAccessTokenWithMcpResource(SESSION_TENANT_ID);

            assertThat(extractJwtClaim(accessToken, "tenant_id")).isEqualTo(SESSION_TENANT_ID);
        }

        @Test
        void testTokenTenantIdIgnoresResourceNamingAnotherTenant() throws Exception {
            String accessToken = requestAccessTokenWithMcpResource("other");

            assertThat(extractJwtClaim(accessToken, "tenant_id")).isEqualTo(SESSION_TENANT_ID);
        }

        @Test
        void testTokenAudienceIsDerivedFromResourceParameter() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = login(httpClient);

                String codeChallenge = createCodeChallenge();

                String resource = "http://localhost:" + port + "/api/automation/" + TenantKey.of("acme") + "/mcp";

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

                HttpResponse<String> tokenResponse = sendTokenRequest(httpClient, tokenBody);

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

                assertThat(location).isEqualTo(
                    "/login?redirect="
                        + URLEncoder.encode("/oauth2/authorize?" + authorizeQuery, StandardCharsets.UTF_8));
            }
        }

        private HttpResponse<String> sendClientAuthenticatedRequest(HttpClient httpClient, String path, String body)
            throws Exception {

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Basic " + EncodingUtils.base64EncodeToString(CLIENT_ID + ":" + CLIENT_SECRET))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> sendTokenRequest(HttpClient httpClient, String tokenBody) throws Exception {
            return sendClientAuthenticatedRequest(httpClient, "/oauth2/token", tokenBody);
        }

        private String requestTokenResponseBody() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = login(httpClient);

                HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
                    .uri(
                        URI.create("http://localhost:" + port + SESSION_TENANT_PATH + "?tenantId=" + SESSION_TENANT_ID))
                    .header("Cookie", sessionCookie)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                httpClient.send(sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

                String authorizeQuery = "response_type=code" +
                    "&client_id=" + CLIENT_ID +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                    "&code_challenge=" + createCodeChallenge() +
                    "&code_challenge_method=S256";

                HttpRequest authorizeRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
                    .header("Cookie", sessionCookie)
                    .GET()
                    .build();

                HttpResponse<String> authorizeResponse = httpClient.send(
                    authorizeRequest, HttpResponse.BodyHandlers.ofString());

                String location = authorizeResponse.headers()
                    .firstValue("Location")
                    .orElseThrow(() -> new IllegalStateException("No Location header on authorize response"));

                HttpResponse<String> tokenResponse = sendTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=" + extractQueryParameter(location, "code") +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER);

                assertThat(tokenResponse.statusCode()).isEqualTo(200);

                return tokenResponse.body();
            }
        }

        private String requestAccessTokenWithMcpResource(String resourceTenantId) throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = login(httpClient);

                HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
                    .uri(
                        URI.create("http://localhost:" + port + SESSION_TENANT_PATH + "?tenantId=" + SESSION_TENANT_ID))
                    .header("Cookie", sessionCookie)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                HttpResponse<String> sessionTenantResponse = httpClient.send(
                    sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

                assertThat(sessionTenantResponse.statusCode()).isEqualTo(204);

                String codeChallenge = createCodeChallenge();

                String resource =
                    "http://localhost:" + port + "/api/automation/" + TenantKey.of(resourceTenantId) + "/mcp";

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

                HttpResponse<String> tokenResponse = sendTokenRequest(httpClient, tokenBody);

                assertThat(tokenResponse.statusCode()).isEqualTo(200);

                String accessToken = extractJsonStringField(tokenResponse.body(), "access_token");

                return accessToken;
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

        private static String rekeyTokenValue(String tokenValue, String tenantId) {
            String decoded = new String(EncodingUtils.urlDecodeBase64FromString(tokenValue), StandardCharsets.UTF_8);

            String randomPart = decoded.substring(decoded.indexOf(':') + 1);

            String rekeyedTokenValue = EncodingUtils.urlEncodeBase64ToString(tenantId + ":" + randomPart);

            assertThat(TenantKeyedTokenValues.parseTenantId(rekeyedTokenValue)).contains(tenantId);

            return rekeyedTokenValue;
        }

        private static String createCodeChallenge() throws Exception {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII));

            return EncodingUtils.urlEncodeBase64ToString(digest);
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

            return new String(EncodingUtils.urlDecodeBase64FromString(parts[1]), StandardCharsets.UTF_8);
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
                    .csrf(csrf -> csrf.disable())
                    .addFilterBefore(new SessionTenantTestFilter(), AuthorizationFilter.class);

                return http.build();
            }

            /**
             * Stores the requested tenant in the session the way ByteChef's multi-tenant login success handler does.
             */
            private static final class SessionTenantTestFilter extends OncePerRequestFilter {

                @Override
                protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                    if (!SESSION_TENANT_PATH.equals(request.getRequestURI())) {
                        filterChain.doFilter(request, response);

                        return;
                    }

                    HttpSession session = request.getSession();

                    session.setAttribute(TenantConstants.CURRENT_TENANT_ID, request.getParameter("tenantId"));

                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
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

    /**
     * Proves the authorization-server security chain is scoped to the authorization-server endpoints and does not
     * shadow the surrounding application's other security chains. A separate chain guards {@code /protected} with
     * authentication; if the authorization-server chain matched every request (as an unscoped highest-precedence
     * permit-all chain would), it would let an unauthenticated {@code /protected} request through with 200. Correctly
     * scoped, the request falls through to the guarding chain and is rejected, while the authorization-server's own
     * metadata endpoint still responds.
     *
     * @author Ivica Cardic
     */
    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = ChainScoping.ChainScopingTestConfiguration.class,
        properties = {
            "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Import(PostgreSQLContainerConfiguration.class)
    class ChainScoping {

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

    /**
     * Drives the consent round trip for a client requiring authorization consent against a schema-per-tenant database:
     * the authorization endpoint redirects the logged-in user to ByteChef's consent page ({@code /oauth2/consent}) with
     * the {@code client_id}, the requested {@code scope} and the authorization server's consent {@code state}, and the
     * form that page posts back to {@code /oauth2/authorize} either issues a code keyed to the session tenant (approve)
     * or returns {@code access_denied} (deny), echoing the client's own {@code state} in both cases.
     *
     * @author Ivica Cardic
     */
    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = Consent.ConsentTestConfiguration.class,
        properties = {
            "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee", "spring.liquibase.contexts=mono"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Import({
        MultiTenantSchemaIntTestConfiguration.class, PostgreSQLContainerConfiguration.class
    })
    class Consent {

        private static final String AUTOMATION_SCOPE = "mcp:automation";
        private static final String CLIENT_ID = "consent-client";
        private static final String CLIENT_SECRET = "consent-secret";
        private static final String CLIENT_STATE = "client-state-123";
        private static final String CODE_VERIFIER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01";
        private static final String MANAGEMENT_SCOPE = "mcp:management";
        private static final String OTHER_RESOURCE = "https://bytechef.example.test/api/automation/other/mcp";
        private static final String REDIRECT_URI = "http://127.0.0.1/callback";
        private static final String RESOURCE = "https://bytechef.example.test/api/automation/secret/mcp";
        private static final String SESSION_TENANT_PATH = "/test/session-tenant";

        @Autowired
        private DataSource dataSource;

        @Autowired
        private RegisteredClientRepository registeredClientRepository;

        @LocalServerPort
        private int port;

        private String registeredClientId;

        @BeforeEach
        void beforeEach() {
            deleteAllRows();

            registeredClientId = UUID.randomUUID()
                .toString();

            RegisteredClient registeredClient = RegisteredClient.withId(registeredClientId)
                .clientId(CLIENT_ID)
                .clientSecret("{noop}" + CLIENT_SECRET)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(REDIRECT_URI)
                .scope(AUTOMATION_SCOPE)
                .scope(MANAGEMENT_SCOPE)
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    .requireProofKey(true)
                    .build())
                .build();

            registeredClientRepository.save(registeredClient);
        }

        @AfterEach
        void afterEach() {
            deleteAllRows();
        }

        @Test
        void testConsentRequiredAuthorizeRedirectsToConsentPage() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = loginIntoTenant(httpClient);

                String location = requestConsentPage(httpClient, sessionCookie);

                assertThat(location).contains("/oauth2/consent?");
                assertThat(extractQueryParameter(location, "client_id")).isEqualTo(CLIENT_ID);
                assertThat(extractQueryParameter(location, "scope"))
                    .isEqualTo(AUTOMATION_SCOPE + " " + MANAGEMENT_SCOPE);
                assertThat(extractQueryParameter(location, "state")).isNotBlank()
                    .isNotEqualTo(CLIENT_STATE);
            }
        }

        @Test
        @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
        void testApprovedConsentIssuesCodeKeyedToTheSessionTenant() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = loginIntoTenant(httpClient);

                String consentState = extractQueryParameter(requestConsentPage(httpClient, sessionCookie), "state");

                HttpResponse<String> consentResponse = postConsent(
                    httpClient, sessionCookie,
                    "client_id=" + CLIENT_ID +
                        "&state=" + URLEncoder.encode(consentState, StandardCharsets.UTF_8) +
                        "&scope=" + URLEncoder.encode(AUTOMATION_SCOPE, StandardCharsets.UTF_8) +
                        "&scope=" + URLEncoder.encode(MANAGEMENT_SCOPE, StandardCharsets.UTF_8));

                assertThat(consentResponse.statusCode()).isEqualTo(302);

                String location = getLocation(consentResponse);

                assertThat(location).startsWith(REDIRECT_URI + "?code=");
                assertThat(extractQueryParameter(location, "state")).isEqualTo(CLIENT_STATE);

                String authorizationCode = extractQueryParameter(location, "code");

                assertThat(TenantKeyedTokenValues.parseTenantId(authorizationCode)).contains(ACME_TENANT_ID);

                HttpResponse<String> tokenResponse = sendTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER);

                assertThat(tokenResponse.statusCode()).as(tokenResponse::body)
                    .isEqualTo(200);
                assertThat(tokenResponse.body()).contains("\"access_token\"");

                assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization_consent")).isEqualTo(1);
                assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
                assertThat(countRows(getDatabaseSchema(GLOBEX_TENANT_ID), "oauth2_authorization_consent")).isZero();
                assertThat(countRows(TenantContext.DEFAULT_TENANT_ID, "oauth2_authorization_consent")).isZero();
                assertThat(countRows(TenantContext.DEFAULT_TENANT_ID, "oauth2_authorization")).isZero();

                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                String consentedAuthorities = jdbcTemplate.queryForObject(
                    "SELECT authorities FROM " + getDatabaseSchema(ACME_TENANT_ID) + ".oauth2_authorization_consent " +
                        "WHERE registered_client_id = ? AND principal_name = 'user'",
                    String.class, registeredClientId);

                assertThat(consentedAuthorities).contains(AUTOMATION_SCOPE, MANAGEMENT_SCOPE);
            }
        }

        @Test
        void testDeniedConsentRedirectsWithAccessDeniedAndStoresNoAuthorization() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String sessionCookie = loginIntoTenant(httpClient);

                String consentState = extractQueryParameter(requestConsentPage(httpClient, sessionCookie), "state");

                HttpResponse<String> consentResponse = postConsent(
                    httpClient, sessionCookie,
                    "client_id=" + CLIENT_ID + "&state=" + URLEncoder.encode(consentState, StandardCharsets.UTF_8));

                assertThat(consentResponse.statusCode()).isEqualTo(302);

                String location = getLocation(consentResponse);

                assertThat(location).startsWith(REDIRECT_URI + "?");
                assertThat(extractQueryParameter(location, "error")).isEqualTo("access_denied");
                assertThat(extractQueryParameter(location, "state")).isEqualTo(CLIENT_STATE);
                assertThat(location).doesNotContain("code=");

                assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isZero();
                assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization_consent")).isZero();
            }
        }

        @Test
        void testDynamicallyRegisteredClientWithoutScopeIsGrantedTheMcpScopes() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                HttpResponse<String> registrationResponse = registerClient(httpClient, REDIRECT_URI, null);

                assertThat(registrationResponse.statusCode()).isEqualTo(201);
                assertThat(AuthorizationCodeFlow.extractJsonStringField(registrationResponse.body(), "scope")
                    .split(" "))
                        .containsExactlyInAnyOrder(AUTOMATION_SCOPE, MANAGEMENT_SCOPE);
            }
        }

        @Test
        void testDynamicClientRegistrationRejectsAScopeOtherThanTheMcpScopes() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                HttpResponse<String> registrationResponse = registerClient(httpClient, REDIRECT_URI, "openid");

                assertThat(registrationResponse.statusCode()).isEqualTo(400);
                assertThat(registrationResponse.body()).contains("invalid_scope");
            }
        }

        @Test
        void testAuthorizeWithoutScopeForDynamicClientShowsConsentInsteadOfIssuingCode() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String foreignRedirectUri = "https://attacker.example/callback";

                String dynamicClientId = AuthorizationCodeFlow.extractJsonStringField(
                    registerClient(httpClient, foreignRedirectUri, null).body(), "client_id");

                String sessionCookie = loginIntoTenant(httpClient);

                String location =
                    requestAuthorization(httpClient, sessionCookie, dynamicClientId, foreignRedirectUri, null);

                assertThat(location).contains("/oauth2/consent?")
                    .doesNotStartWith(foreignRedirectUri);
            }
        }

        @Test
        void testApprovingConsentWithoutScopeForDynamicClientIssuesNoCode() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String dynamicClientId = AuthorizationCodeFlow.extractJsonStringField(
                    registerClient(httpClient, REDIRECT_URI, null).body(), "client_id");

                String sessionCookie = loginIntoTenant(httpClient);

                String consentState = extractQueryParameter(
                    requestAuthorization(httpClient, sessionCookie, dynamicClientId, REDIRECT_URI, null), "state");

                HttpResponse<String> consentResponse = postConsent(
                    httpClient, sessionCookie,
                    "client_id=" + dynamicClientId + "&state="
                        + URLEncoder.encode(consentState, StandardCharsets.UTF_8));

                String location = getLocation(consentResponse);

                assertThat(location).contains("error=access_denied")
                    .doesNotContain("code=");
            }
        }

        @Test
        void testPublicDynamicClientRefreshesItsTokensAndEachRefreshTokenWorksOnce() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String dynamicClientId = AuthorizationCodeFlow.extractJsonStringField(
                    registerClient(
                        httpClient, REDIRECT_URI, AUTOMATION_SCOPE, "\"authorization_code\",\"refresh_token\"")
                            .body(),
                    "client_id");

                String sessionCookie = loginIntoTenant(httpClient);

                String consentState = extractQueryParameter(
                    requestAuthorization(
                        httpClient, sessionCookie, dynamicClientId, REDIRECT_URI, AUTOMATION_SCOPE, RESOURCE),
                    "state");

                String location = getLocation(
                    postConsent(
                        httpClient, sessionCookie,
                        "client_id=" + dynamicClientId +
                            "&state=" + URLEncoder.encode(consentState, StandardCharsets.UTF_8) +
                            "&scope=" + URLEncoder.encode(AUTOMATION_SCOPE, StandardCharsets.UTF_8)));

                HttpResponse<String> tokenResponse = sendPublicClientTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=" + URLEncoder.encode(extractQueryParameter(location, "code"), StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER +
                        "&client_id=" + dynamicClientId +
                        "&resource=" + URLEncoder.encode(RESOURCE, StandardCharsets.UTF_8));

                assertThat(tokenResponse.statusCode()).as(tokenResponse::body)
                    .isEqualTo(200);
                assertThat(tokenResponse.body()).contains("\"expires_in\":3599");

                String firstRefreshToken = AuthorizationCodeFlow.extractJsonStringField(
                    tokenResponse.body(), "refresh_token");

                assertThat(TenantKeyedTokenValues.parseTenantId(firstRefreshToken)).contains(ACME_TENANT_ID);

                HttpResponse<String> refreshResponse = sendPublicClientTokenRequest(
                    httpClient, refreshTokenRequestBody(firstRefreshToken, dynamicClientId));

                assertThat(refreshResponse.statusCode()).as(refreshResponse::body)
                    .isEqualTo(200);
                assertThat(
                    AuthorizationCodeFlow.extractJwtPayload(
                        AuthorizationCodeFlow.extractJsonStringField(refreshResponse.body(), "access_token")))
                            .as("a refreshed access token keeps the authorized resource as its audience")
                            .contains(RESOURCE);

                String secondRefreshToken = AuthorizationCodeFlow.extractJsonStringField(
                    refreshResponse.body(), "refresh_token");

                assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

                HttpResponse<String> reusedRefreshResponse = sendPublicClientTokenRequest(
                    httpClient, refreshTokenRequestBody(firstRefreshToken, dynamicClientId));

                assertThat(reusedRefreshResponse.statusCode()).isEqualTo(400);
                assertThat(reusedRefreshResponse.body()).contains("invalid_grant");

                HttpResponse<String> otherResourceRefreshResponse = sendPublicClientTokenRequest(
                    httpClient,
                    refreshTokenRequestBody(secondRefreshToken, dynamicClientId) +
                        "&resource=" + URLEncoder.encode(OTHER_RESOURCE, StandardCharsets.UTF_8));

                assertThat(otherResourceRefreshResponse.statusCode()).isEqualTo(400);
                assertThat(otherResourceRefreshResponse.body()).contains("invalid_target");
            }
        }

        @Test
        void testCodeCannotBeExchangedForATokenToAnotherResource() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String dynamicClientId = AuthorizationCodeFlow.extractJsonStringField(
                    registerClient(httpClient, REDIRECT_URI, AUTOMATION_SCOPE).body(), "client_id");

                String sessionCookie = loginIntoTenant(httpClient);

                String consentState = extractQueryParameter(
                    requestAuthorization(
                        httpClient, sessionCookie, dynamicClientId, REDIRECT_URI, AUTOMATION_SCOPE, RESOURCE),
                    "state");

                String location = getLocation(
                    postConsent(
                        httpClient, sessionCookie,
                        "client_id=" + dynamicClientId +
                            "&state=" + URLEncoder.encode(consentState, StandardCharsets.UTF_8) +
                            "&scope=" + URLEncoder.encode(AUTOMATION_SCOPE, StandardCharsets.UTF_8)));

                HttpResponse<String> tokenResponse = sendPublicClientTokenRequest(
                    httpClient,
                    "grant_type=authorization_code" +
                        "&code=" + URLEncoder.encode(extractQueryParameter(location, "code"), StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                        "&code_verifier=" + CODE_VERIFIER +
                        "&client_id=" + dynamicClientId +
                        "&resource=" + URLEncoder.encode(OTHER_RESOURCE, StandardCharsets.UTF_8));

                assertThat(tokenResponse.statusCode()).as(tokenResponse::body)
                    .isEqualTo(400);
                assertThat(tokenResponse.body()).contains("invalid_target");
            }
        }

        @Test
        void testPublicClientRefreshIsRejectedForAClientRegisteredWithoutTheRefreshTokenGrant() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                String dynamicClientId = AuthorizationCodeFlow.extractJsonStringField(
                    registerClient(httpClient, REDIRECT_URI, AUTOMATION_SCOPE).body(), "client_id");

                HttpResponse<String> refreshResponse = sendPublicClientTokenRequest(
                    httpClient,
                    refreshTokenRequestBody(TenantKeyedTokenValues.generate(ACME_TENANT_ID), dynamicClientId));

                assertThat(refreshResponse.statusCode()).as(refreshResponse::body)
                    .isEqualTo(400);
                assertThat(refreshResponse.body()).contains("invalid_grant")
                    .doesNotContain("access_token");
            }
        }

        @Test
        void testPublicClientRefreshIsRejectedForAConfidentialClient() throws Exception {
            try (HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {

                HttpResponse<String> refreshResponse = sendPublicClientTokenRequest(
                    httpClient, refreshTokenRequestBody(TenantKeyedTokenValues.generate(ACME_TENANT_ID), CLIENT_ID));

                assertThat(refreshResponse.statusCode()).as(refreshResponse::body)
                    .isEqualTo(401);
                assertThat(refreshResponse.body()).contains("invalid_client");
            }
        }

        private HttpResponse<String> registerClient(HttpClient httpClient, String redirectUri, String scope)
            throws Exception {

            return registerClient(httpClient, redirectUri, scope, "\"authorization_code\"");
        }

        private HttpResponse<String> registerClient(
            HttpClient httpClient, String redirectUri, String scope, String grantTypes) throws Exception {

            String scopeMember = scope == null ? "" : ",\"scope\":\"" + scope + "\"";

            HttpRequest registrationRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"client_name\":\"Dynamic Client\",\"redirect_uris\":[\"" + redirectUri + "\"]," +
                        "\"grant_types\":[" + grantTypes + "],\"response_types\":[\"code\"]," +
                        "\"token_endpoint_auth_method\":\"none\"" + scopeMember + "}"))
                .build();

            return httpClient.send(registrationRequest, HttpResponse.BodyHandlers.ofString());
        }

        private String requestAuthorization(
            HttpClient httpClient, String sessionCookie, String clientId, String redirectUri, String scope)
            throws Exception {

            return requestAuthorization(httpClient, sessionCookie, clientId, redirectUri, scope, null);
        }

        private String requestAuthorization(
            HttpClient httpClient, String sessionCookie, String clientId, String redirectUri, String scope,
            String resource) throws Exception {

            String authorizeQuery = "response_type=code" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                (scope == null ? "" : "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)) +
                (resource == null ? "" : "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8)) +
                "&state=" + CLIENT_STATE +
                "&code_challenge=" + createCodeChallenge() +
                "&code_challenge_method=S256";

            HttpRequest authorizeRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
                .header("Cookie", sessionCookie)
                .GET()
                .build();

            HttpResponse<String> authorizeResponse = httpClient.send(
                authorizeRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(authorizeResponse.statusCode()).isEqualTo(302);

            return getLocation(authorizeResponse);
        }

        private String requestConsentPage(HttpClient httpClient, String sessionCookie) throws Exception {
            String authorizeQuery = "response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(AUTOMATION_SCOPE + " " + MANAGEMENT_SCOPE, StandardCharsets.UTF_8) +
                "&state=" + CLIENT_STATE +
                "&code_challenge=" + createCodeChallenge() +
                "&code_challenge_method=S256";

            HttpRequest authorizeRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
                .header("Cookie", sessionCookie)
                .GET()
                .build();

            HttpResponse<String> authorizeResponse = httpClient.send(
                authorizeRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(authorizeResponse.statusCode()).isEqualTo(302);

            return getLocation(authorizeResponse);
        }

        private HttpResponse<String> postConsent(HttpClient httpClient, String sessionCookie, String body)
            throws Exception {

            HttpRequest consentRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", sessionCookie)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            return httpClient.send(consentRequest, HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> sendPublicClientTokenRequest(HttpClient httpClient, String body)
            throws Exception {

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            return httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        }

        private static String refreshTokenRequestBody(String refreshToken, String clientId) {
            return "grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);
        }

        private HttpResponse<String> sendTokenRequest(HttpClient httpClient, String body) throws Exception {
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Authorization", "Basic " + EncodingUtils.base64EncodeToString(CLIENT_ID + ":" + CLIENT_SECRET))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            return httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        }

        private String loginIntoTenant(HttpClient httpClient) throws Exception {
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

            String cookie =
                sessionCookie.orElseThrow(() -> new IllegalStateException("No JSESSIONID in login response"));

            HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + SESSION_TENANT_PATH + "?tenantId=" + ACME_TENANT_ID))
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<String> sessionTenantResponse = httpClient.send(
                sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(sessionTenantResponse.statusCode()).isEqualTo(204);

            return cookie;
        }

        @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
        private int countRows(String schema, String table) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + "." + table, Integer.class);

            return count == null ? 0 : count;
        }

        @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
        private void deleteAllRows() {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            for (String schema : List.of(
                TenantContext.DEFAULT_TENANT_ID, getDatabaseSchema(ACME_TENANT_ID),
                getDatabaseSchema(GLOBEX_TENANT_ID))) {

                jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization_consent");
                jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization");
            }

            jdbcTemplate.update("DELETE FROM " + TenantContext.DEFAULT_TENANT_ID + ".oauth2_registered_client");
        }

        private static String createCodeChallenge() throws Exception {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII));

            return EncodingUtils.urlEncodeBase64ToString(digest);
        }

        private static String getLocation(HttpResponse<String> httpResponse) {
            return httpResponse.headers()
                .firstValue("Location")
                .orElseThrow(() -> new IllegalStateException("No Location header on response"));
        }

        private static String extractQueryParameter(String url, String parameterName) {
            Pattern pattern = Pattern.compile("[?&]" + Pattern.quote(parameterName) + "=([^&]+)");

            Matcher matcher = pattern.matcher(url);

            if (!matcher.find()) {
                throw new IllegalStateException("No " + parameterName + " parameter in URL: " + url);
            }

            return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
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
                    .csrf(csrf -> csrf.disable())
                    .addFilterBefore(new SessionTenantTestFilter(), AuthorizationFilter.class);

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

            private static final class SessionTenantTestFilter extends OncePerRequestFilter {

                @Override
                protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                    if (!SESSION_TENANT_PATH.equals(request.getRequestURI())) {
                        filterChain.doFilter(request, response);

                        return;
                    }

                    HttpSession session = request.getSession();

                    session.setAttribute(TenantConstants.CURRENT_TENANT_ID, request.getParameter("tenantId"));

                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            }
        }
    }

    /**
     * Proves that, with the {@code bytechef.oauth2.authorization-server.enabled} flag off (the default), none of the
     * authorization server endpoints are exposed and none of its beans or filters are registered, even with the
     * module's service, facade and GraphQL packages scanned on a coordinator.
     *
     * @author Ivica Cardic
     */
    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = Disabled.DisabledTestConfiguration.class, properties = "bytechef.coordinator.enabled=true",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Import(PostgreSQLContainerConfiguration.class)
    class Disabled {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private FilterChainProxy filterChainProxy;

        @LocalServerPort
        private int port;

        @ParameterizedTest
        @ValueSource(strings = {
            "/.well-known/oauth-authorization-server", "/oauth2/authorize?response_type=code&client_id=client",
            "/oauth2/jwks"
        })
        void testGetEndpointIsNotExposed(String path) throws Exception {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();

            assertThat(send(httpRequest).statusCode()).isEqualTo(404);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/oauth2/register", "/oauth2/token", "/oauth2/authorize"
        })
        void testPostEndpointIsNotExposed(String path) throws Exception {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

            assertThat(send(httpRequest).statusCode()).isIn(404, 405);
        }

        @Test
        void testAuthorizationServerBeansAreNotRegistered() {
            assertThat(applicationContext.getBeansOfType(RegisteredClientGraphQlController.class)).isEmpty();
            assertThat(applicationContext.getBeansOfType(RegisteredClientFacade.class)).isEmpty();
            assertThat(applicationContext.getBeansOfType(RegisteredClientService.class)).isEmpty();
            assertThat(applicationContext.getBeansOfType(OAuth2AuthorizationGrantService.class)).isEmpty();
            assertThat(applicationContext.getBeansOfType(AuthorizationServerTokenRevocationChecker.class)).isEmpty();
            assertThat(applicationContext.getBeansOfType(SessionTenantFilter.class)).isEmpty();
        }

        @Test
        void testNoSecurityFilterChainContainsTheSessionTenantFilter() {
            List<SecurityFilterChain> securityFilterChains = filterChainProxy.getFilterChains();

            assertThat(securityFilterChains).isNotEmpty();

            for (SecurityFilterChain securityFilterChain : securityFilterChains) {
                List<Filter> filters = securityFilterChain.getFilters();

                assertThat(filters).noneMatch(SessionTenantFilter.class::isInstance);
            }
        }

        private static HttpResponse<String> send(HttpRequest httpRequest) throws Exception {
            try (HttpClient httpClient = HttpClient.newHttpClient()) {
                return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            }
        }

        @ComponentScan(basePackages = {
            "com.bytechef.platform.oauth2.authorizationserver.facade",
            "com.bytechef.platform.oauth2.authorizationserver.service",
            "com.bytechef.platform.oauth2.authorizationserver.web.graphql"
        })
        @Configuration
        @Import(Oauth2AuthorizationServerIntTestConfiguration.class)
        static class DisabledTestConfiguration {
        }
    }

    /**
     * Proves that, with {@code bytechef.oauth2.authorization-server.enabled=true}, Spring Boot 4.0.7 actually hosts the
     * Spring Authorization Server: the metadata endpoint {@code /.well-known/oauth-authorization-server} returns issuer
     * metadata and Dynamic Client Registration (DCR) persists a registered client.
     *
     * @author Ivica Cardic
     */
    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = Oauth2AuthorizationServerIntTestConfiguration.class,
        properties = {
            "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Import(PostgreSQLContainerConfiguration.class)
    class Enabled {

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
}
