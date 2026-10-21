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
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.TENANT_IDS;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.getDatabaseSchema;
import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.APP_REFRESH_TOKEN_REUSE_GRACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.oauth2.authorizationserver.AppClientAuthorizationIntTest.AppClientAuthorizationTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.authentication.AppRefreshTokenReuseAuthenticationProvider;
import com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.domain.ClientRegistrationRequest;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import com.bytechef.platform.oauth2.authorizationserver.tenant.AuthorizationServerTokenRevocationChecker;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantRoutingOAuth2AuthorizationService;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantScope;
import com.bytechef.platform.security.web.config.McpJwtSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpOAuth2ResourceServerSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.security.web.mcp.TestMcpEndpoint;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AppClientAuthorizationTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee", "spring.liquibase.contexts=mono"
    },
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import({
    MultiTenantSchemaIntTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
class AppClientAuthorizationIntTest {

    private static final String APP_CLIENT_ID = "app-" + UUID.randomUUID();
    private static final String APP_REDIRECT_URI = "https://app.example.com/auth/callback";
    private static final String APP_SCOPE = "openid profile app:use";
    private static final String APP_USE_SCOPE = "app:use";
    private static final String CANONICAL_MCP_PATH = "/api/management/mcp";
    private static final String CODE_VERIFIER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01";
    private static final String MCP_CLIENT_ID = "mcp-client";
    private static final String MCP_CLIENT_SECRET = "mcp-secret";
    private static final String MCP_PUBLIC_CLIENT_ID = "mcp-public-client";
    private static final String MCP_REDIRECT_URI = "http://127.0.0.1/callback";
    private static final String MCP_SCOPE = "mcp:management";
    private static final int PORT = TestSocketUtils.findAvailableTcpPort();
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static final String MCP_RESOURCE = BASE_URL + CANONICAL_MCP_PATH;
    private static final String SESSION_TENANT_PATH = "/test/session-tenant";
    private static final String USER_EMAIL = "ada@example.com";
    private static final String USER_FIRST_NAME = "Ada";
    private static final String USER_LAST_NAME = "Lovelace";
    private static final String USER_LOGIN = "user";
    private static final int CONCURRENT_REFRESH_COUNT = 4;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TenantRoutingOAuth2AuthorizationService authorizationService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private RegisteredClientService registeredClientService;

    @Autowired
    private AuthorizationServerTokenRevocationChecker tokenRevocationChecker;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private UserService userService;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("server.port", () -> PORT);
        dynamicPropertyRegistry.add("bytechef.public-url", () -> BASE_URL);
        dynamicPropertyRegistry.add("bytechef.oauth2.resource-server.issuers[0].uri", () -> BASE_URL);
        dynamicPropertyRegistry.add("bytechef.oauth2.resource-server.issuers[0].self", () -> true);
        dynamicPropertyRegistry.add(
            "bytechef.oauth2.resource-server.issuers[0].tenant-claim", () -> "tenant_id");
    }

    @BeforeEach
    void beforeEach() {
        deleteAllRows();

        RegisteredClient appClient = RegisteredClient.withId(UUID.randomUUID()
            .toString())
            .clientId(APP_CLIENT_ID)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri(APP_REDIRECT_URI)
            .scope("openid")
            .scope("profile")
            .scope(APP_USE_SCOPE)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .reuseRefreshTokens(false)
                .build())
            .build();

        registeredClientRepository.save(appClient);

        RegisteredClient mcpClient = RegisteredClient.withId(UUID.randomUUID()
            .toString())
            .clientId(MCP_CLIENT_ID)
            .clientSecret("{noop}" + MCP_CLIENT_SECRET)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri(MCP_REDIRECT_URI)
            .scope(MCP_SCOPE)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true)
                .build())
            .build();

        registeredClientRepository.save(mcpClient);

        RegisteredClient mcpPublicClient = RegisteredClient.withId(UUID.randomUUID()
            .toString())
            .clientId(MCP_PUBLIC_CLIENT_ID)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri(MCP_REDIRECT_URI)
            .scope(MCP_SCOPE)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .reuseRefreshTokens(false)
                .build())
            .build();

        registeredClientRepository.save(mcpPublicClient);

        User user = mock(User.class);

        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(user.getFirstName()).thenReturn(USER_FIRST_NAME);
        when(user.getLastName()).thenReturn(USER_LAST_NAME);
        when(user.getLogin()).thenReturn(USER_LOGIN);
        when(user.isActivated()).thenReturn(true);
        when(userService.fetchUserByLogin(USER_LOGIN)).thenAnswer(invocation -> fetchAcmeUser(user));
    }

    @AfterEach
    void afterEach() {
        deleteAllRows();
    }

    @Test
    void testTokenResponseCarriesAnIdTokenWithTheSignedInUser() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String tokenResponse = requestAppTokens(httpClient, APP_SCOPE, null);

            String idTokenValue = extractJsonStringField(tokenResponse, "id_token");

            assertThat(idTokenValue).isNotNull();

            Jwt idToken = jwtDecoder.decode(idTokenValue);

            assertThat(idToken.getSubject()).isEqualTo(USER_LOGIN);
            assertThat(idToken.getClaimAsString("name")).isEqualTo(USER_FIRST_NAME + " " + USER_LAST_NAME);
            assertThat(idToken.getClaimAsString("email")).isEqualTo(USER_EMAIL);
            assertThat(idToken.getAudience()).containsExactly(APP_CLIENT_ID);
        }
    }

    @Test
    void testAccessTokenAudienceContainsTheClientId() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            Jwt accessToken = decodeAccessToken(requestAppTokens(httpClient, APP_SCOPE, null));

            assertThat(accessToken.getAudience()).containsExactly(APP_CLIENT_ID);
        }
    }

    @Test
    void testAccessTokenCarriesTheTenantClaimAndTheAppUseScope() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            Jwt accessToken = decodeAccessToken(requestAppTokens(httpClient, APP_SCOPE, null));

            assertThat(accessToken.getClaimAsString("tenant_id")).isEqualTo(ACME_TENANT_ID);
            assertThat(accessToken.getIssuer()
                .toString()).isEqualTo(BASE_URL);
            assertThat(accessToken.getClaimAsStringList("scope")).contains(APP_USE_SCOPE);
        }
    }

    @Test
    void testRefreshForThePublicClientKeepsTheAccessTokenAudience() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String tokenResponse = requestAppTokens(httpClient, APP_SCOPE, null);

            String refreshTokenValue = extractJsonStringField(tokenResponse, "refresh_token");

            assertThat(refreshTokenValue).isNotNull();

            HttpResponse<String> refreshResponse = sendTokenRequest(
                httpClient, null,
                "grant_type=refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(refreshTokenValue, StandardCharsets.UTF_8) +
                    "&client_id=" + APP_CLIENT_ID);

            assertThat(refreshResponse.statusCode()).isEqualTo(200);

            Jwt refreshedAccessToken = decodeAccessToken(refreshResponse.body());

            assertThat(refreshedAccessToken.getAudience()).containsExactly(APP_CLIENT_ID);
            assertThat(refreshedAccessToken.getClaimAsString("tenant_id")).isEqualTo(ACME_TENANT_ID);
            assertThat(refreshedAccessToken.getClaimAsStringList("scope")).contains(APP_USE_SCOPE);

            String refreshedIdTokenValue = extractJsonStringField(refreshResponse.body(), "id_token");

            assertThat(refreshedIdTokenValue).isNotNull();

            Jwt refreshedIdToken = jwtDecoder.decode(refreshedIdTokenValue);

            assertThat(refreshedIdToken.getClaimAsString("email")).isEqualTo(USER_EMAIL);
        }
    }

    @Test
    void testClientRegisteredThroughTheServiceAuthorizesExchangesAndRefreshes() throws Exception {
        String serviceClientId = "app-" + UUID.randomUUID();
        String serviceRedirectUri = "https://registered.example.com/auth/callback";

        registeredClientService.registerClient(
            new ClientRegistrationRequest(
                serviceClientId, "Registered App", List.of(serviceRedirectUri),
                Set.of("openid", "profile", APP_USE_SCOPE),
                false));

        try (HttpClient httpClient = createHttpClient()) {
            String tokenResponse = requestAppTokens(httpClient, serviceClientId, serviceRedirectUri, APP_SCOPE, null);

            Jwt accessToken = decodeAccessToken(tokenResponse);

            assertThat(accessToken.getAudience()).containsExactly(serviceClientId);
            assertThat(accessToken.getClaimAsString("tenant_id")).isEqualTo(ACME_TENANT_ID);
            assertThat(accessToken.getClaimAsStringList("scope")).contains(APP_USE_SCOPE);

            String idTokenValue = extractJsonStringField(tokenResponse, "id_token");

            assertThat(idTokenValue).isNotNull();

            Jwt idToken = jwtDecoder.decode(idTokenValue);

            assertThat(idToken.getClaimAsString("email")).isEqualTo(USER_EMAIL);

            String refreshTokenValue = extractJsonStringField(tokenResponse, "refresh_token");

            assertThat(refreshTokenValue).isNotNull();

            HttpResponse<String> refreshResponse = sendTokenRequest(
                httpClient, null,
                "grant_type=refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(refreshTokenValue, StandardCharsets.UTF_8) +
                    "&client_id=" + serviceClientId);

            assertThat(refreshResponse.statusCode())
                .as(refreshResponse.body())
                .isEqualTo(200);

            Jwt refreshedAccessToken = decodeAccessToken(refreshResponse.body());

            assertThat(refreshedAccessToken.getAudience()).containsExactly(serviceClientId);
            assertThat(extractJsonStringField(refreshResponse.body(), "refresh_token")).isNotNull()
                .isNotEqualTo(refreshTokenValue);
        }
    }

    @Test
    void testUserInfoEndpointIsNotEnabled() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String accessToken = extractJsonStringField(requestAppTokens(httpClient, APP_SCOPE, null), "access_token");

            HttpRequest userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<String> userInfoResponse = httpClient.send(
                userInfoRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(userInfoResponse.statusCode()).isNotEqualTo(200);
            assertThat(userInfoResponse.body()).doesNotContain(USER_EMAIL);

            HttpRequest metadataRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/.well-known/oauth-authorization-server"))
                .GET()
                .build();

            HttpResponse<String> metadataResponse = httpClient.send(
                metadataRequest, HttpResponse.BodyHandlers.ofString());

            assertThat(metadataResponse.statusCode()).isEqualTo(200);
            assertThat(metadataResponse.body()).doesNotContain("userinfo_endpoint");
        }
    }

    @Test
    void testAppTokenRequestedWithAnMcpResourceDoesNotGetTheMcpAudience() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            Jwt openIdAccessToken = decodeAccessToken(requestAppTokens(httpClient, APP_SCOPE, MCP_RESOURCE));

            assertThat(openIdAccessToken.getAudience()).containsExactly(APP_CLIENT_ID);

            Jwt appUseOnlyAccessToken = decodeAccessToken(requestAppTokens(httpClient, APP_USE_SCOPE, MCP_RESOURCE));

            assertThat(appUseOnlyAccessToken.getAudience()).containsExactly(APP_CLIENT_ID);
        }
    }

    @Test
    void testAppTokenIsRejectedOnTheMcpEndpoint() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String accessToken = extractJsonStringField(
                requestAppTokens(httpClient, APP_SCOPE, MCP_RESOURCE), "access_token");

            assertThat(sendMcpRequest(httpClient, accessToken).statusCode()).isEqualTo(403);
        }
    }

    @Test
    void testMcpTokenKeepsItsResourceAudience() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String sessionCookie = loginIntoAcme(httpClient);

            String code =
                authorize(httpClient, sessionCookie, MCP_CLIENT_ID, MCP_REDIRECT_URI, MCP_SCOPE, MCP_RESOURCE);

            HttpResponse<String> tokenResponse = sendTokenRequest(
                httpClient, EncodingUtils.base64EncodeToString(MCP_CLIENT_ID + ":" + MCP_CLIENT_SECRET),
                "grant_type=authorization_code" +
                    "&code=" + code +
                    "&redirect_uri=" + URLEncoder.encode(MCP_REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&code_verifier=" + CODE_VERIFIER +
                    "&resource=" + URLEncoder.encode(MCP_RESOURCE, StandardCharsets.UTF_8));

            assertThat(tokenResponse.statusCode()).isEqualTo(200);
            assertThat(extractJsonStringField(tokenResponse.body(), "id_token")).isNull();

            Jwt accessToken = decodeAccessToken(tokenResponse.body());

            assertThat(accessToken.getAudience()).containsExactly(MCP_RESOURCE);
            assertThat(accessToken.getClaims()).doesNotContainKeys("name", "email");

            HttpResponse<String> mcpResponse = sendMcpRequest(
                httpClient, extractJsonStringField(tokenResponse.body(), "access_token"));

            assertThat(mcpResponse.statusCode()).isEqualTo(200);
            assertThat(mcpResponse.body()).isEqualTo(ACME_TENANT_ID);
        }
    }

    @Test
    void testRefreshTokenPresentedAgainRightAfterItsRotationReturnsTheRotatedTokens() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String refreshTokenValue = extractJsonStringField(
                requestAppTokens(httpClient, APP_SCOPE, null), "refresh_token");

            HttpResponse<String> firstRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, refreshTokenValue);

            assertThat(firstRefreshResponse.statusCode())
                .as(firstRefreshResponse.body())
                .isEqualTo(200);

            HttpResponse<String> secondRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, refreshTokenValue);

            assertThat(secondRefreshResponse.statusCode())
                .as(secondRefreshResponse.body())
                .isEqualTo(200);

            String rotatedRefreshTokenValue = extractJsonStringField(firstRefreshResponse.body(), "refresh_token");
            String rotatedAccessTokenValue = extractJsonStringField(firstRefreshResponse.body(), "access_token");

            assertThat(rotatedRefreshTokenValue).isNotEqualTo(refreshTokenValue);
            assertThat(extractJsonStringField(secondRefreshResponse.body(), "refresh_token"))
                .isEqualTo(rotatedRefreshTokenValue);
            assertThat(extractJsonStringField(secondRefreshResponse.body(), "access_token"))
                .isEqualTo(rotatedAccessTokenValue);
            assertThat(tokenRevocationChecker.isRevoked(rotatedAccessTokenValue, ACME_TENANT_ID)).isFalse();

            HttpResponse<String> nextRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, rotatedRefreshTokenValue);

            assertThat(nextRefreshResponse.statusCode())
                .as(nextRefreshResponse.body())
                .isEqualTo(200);
        }
    }

    @Test
    void testRefreshTokenPresentedAgainAfterTheGraceIsRefusedAndTheGrantStaysValid() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String refreshTokenValue = extractJsonStringField(
                requestAppTokens(httpClient, APP_SCOPE, null), "refresh_token");

            HttpResponse<String> firstRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, refreshTokenValue);

            assertThat(firstRefreshResponse.statusCode()).isEqualTo(200);

            String rotatedRefreshTokenValue = extractJsonStringField(firstRefreshResponse.body(), "refresh_token");

            moveRotationBeforeTheGrace(rotatedRefreshTokenValue);

            HttpResponse<String> replayResponse = sendRefreshRequest(httpClient, APP_CLIENT_ID, refreshTokenValue);

            assertThat(replayResponse.statusCode()).isEqualTo(400);
            assertThat(extractJsonStringField(replayResponse.body(), "error")).isEqualTo("invalid_grant");

            HttpResponse<String> nextRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, rotatedRefreshTokenValue);

            assertThat(nextRefreshResponse.statusCode())
                .as(nextRefreshResponse.body())
                .isEqualTo(200);
        }
    }

    @Test
    void testRefreshTokenFromTwoRotationsBackIsRefusedWithinTheGrace() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String refreshTokenValue = extractJsonStringField(
                requestAppTokens(httpClient, APP_SCOPE, null), "refresh_token");

            HttpResponse<String> firstRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, refreshTokenValue);

            assertThat(firstRefreshResponse.statusCode()).isEqualTo(200);

            HttpResponse<String> secondRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, extractJsonStringField(firstRefreshResponse.body(), "refresh_token"));

            assertThat(secondRefreshResponse.statusCode()).isEqualTo(200);

            HttpResponse<String> replayResponse = sendRefreshRequest(httpClient, APP_CLIENT_ID, refreshTokenValue);

            assertThat(replayResponse.statusCode()).isEqualTo(400);
            assertThat(extractJsonStringField(replayResponse.body(), "error")).isEqualTo("invalid_grant");
        }
    }

    @Test
    void testRefreshTokenPresentedAgainForAnotherClientIsRefused() throws Exception {
        String otherClientId = "app-" + UUID.randomUUID();

        registeredClientService.registerClient(
            new ClientRegistrationRequest(
                otherClientId, "Other App", List.of("https://other.example.com/auth/callback"),
                Set.of("openid", "profile", APP_USE_SCOPE), false));

        try (HttpClient httpClient = createHttpClient()) {
            String refreshTokenValue = extractJsonStringField(
                requestAppTokens(httpClient, APP_SCOPE, null), "refresh_token");

            assertThat(sendRefreshRequest(httpClient, APP_CLIENT_ID, refreshTokenValue).statusCode()).isEqualTo(200);

            HttpResponse<String> replayResponse = sendRefreshRequest(httpClient, otherClientId, refreshTokenValue);

            assertThat(replayResponse.statusCode()).isEqualTo(400);
            assertThat(extractJsonStringField(replayResponse.body(), "error")).isEqualTo("invalid_grant");
        }
    }

    @Test
    void testConcurrentRefreshesWithOneRefreshTokenAllGetTheSameValidTokens() throws Exception {
        try (HttpClient httpClient = createHttpClient();
            ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_REFRESH_COUNT)) {

            String refreshTokenValue = extractJsonStringField(
                requestAppTokens(httpClient, APP_SCOPE, null), "refresh_token");

            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<HttpResponse<String>>> refreshResponseFutures = new ArrayList<>();

            for (int index = 0; index < CONCURRENT_REFRESH_COUNT; index++) {
                refreshResponseFutures.add(executorService.submit(() -> {
                    boolean started = startLatch.await(10, TimeUnit.SECONDS);

                    assertThat(started).isTrue();

                    return sendRefreshRequest(httpClient, APP_CLIENT_ID, refreshTokenValue);
                }));
            }

            startLatch.countDown();

            Set<String> refreshTokenValues = new HashSet<>();
            Set<String> accessTokenValues = new HashSet<>();

            for (Future<HttpResponse<String>> refreshResponseFuture : refreshResponseFutures) {
                HttpResponse<String> refreshResponse = refreshResponseFuture.get(30, TimeUnit.SECONDS);

                assertThat(refreshResponse.statusCode())
                    .as(refreshResponse.body())
                    .isEqualTo(200);

                refreshTokenValues.add(extractJsonStringField(refreshResponse.body(), "refresh_token"));
                accessTokenValues.add(extractJsonStringField(refreshResponse.body(), "access_token"));
            }

            assertThat(refreshTokenValues).hasSize(1);
            assertThat(accessTokenValues).hasSize(1);
            assertThat(tokenRevocationChecker.isRevoked(accessTokenValues.iterator()
                .next(), ACME_TENANT_ID)).isFalse();

            HttpResponse<String> nextRefreshResponse = sendRefreshRequest(
                httpClient, APP_CLIENT_ID, refreshTokenValues.iterator()
                    .next());

            assertThat(nextRefreshResponse.statusCode())
                .as(nextRefreshResponse.body())
                .isEqualTo(200);
        }
    }

    @Test
    void testRefreshTokenPresentedAgainIsRefusedForAPublicMcpClient() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String sessionCookie = loginIntoAcme(httpClient);

            String code = authorize(
                httpClient, sessionCookie, MCP_PUBLIC_CLIENT_ID, MCP_REDIRECT_URI, MCP_SCOPE, MCP_RESOURCE);

            HttpResponse<String> tokenResponse = sendTokenRequest(
                httpClient, null,
                "grant_type=authorization_code" +
                    "&code=" + code +
                    "&redirect_uri=" + URLEncoder.encode(MCP_REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&code_verifier=" + CODE_VERIFIER +
                    "&resource=" + URLEncoder.encode(MCP_RESOURCE, StandardCharsets.UTF_8) +
                    "&client_id=" + MCP_PUBLIC_CLIENT_ID);

            assertThat(tokenResponse.statusCode())
                .as(tokenResponse.body())
                .isEqualTo(200);

            String refreshTokenValue = extractJsonStringField(tokenResponse.body(), "refresh_token");

            assertThat(refreshTokenValue).isNotNull();

            HttpResponse<String> firstRefreshResponse = sendRefreshRequest(
                httpClient, MCP_PUBLIC_CLIENT_ID, refreshTokenValue);

            assertThat(firstRefreshResponse.statusCode())
                .as(firstRefreshResponse.body())
                .isEqualTo(200);

            HttpResponse<String> replayResponse = sendRefreshRequest(
                httpClient, MCP_PUBLIC_CLIENT_ID, refreshTokenValue);

            assertThat(replayResponse.statusCode()).isEqualTo(400);
            assertThat(extractJsonStringField(replayResponse.body(), "error")).isEqualTo("invalid_grant");
        }
    }

    private String requestAppTokens(HttpClient httpClient, String scope, @Nullable String resource)
        throws Exception {

        return requestAppTokens(httpClient, APP_CLIENT_ID, APP_REDIRECT_URI, scope, resource);
    }

    private String requestAppTokens(
        HttpClient httpClient, String clientId, String redirectUri, String scope, @Nullable String resource)
        throws Exception {

        String sessionCookie = loginIntoAcme(httpClient);

        String code = authorize(httpClient, sessionCookie, clientId, redirectUri, scope, resource);

        String tokenBody = "grant_type=authorization_code" +
            "&code=" + code +
            "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
            "&code_verifier=" + CODE_VERIFIER +
            "&client_id=" + clientId;

        if (resource != null) {
            tokenBody += "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8);
        }

        HttpResponse<String> tokenResponse = sendTokenRequest(httpClient, null, tokenBody);

        assertThat(tokenResponse.statusCode())
            .as(tokenResponse.body())
            .isEqualTo(200);

        return tokenResponse.body();
    }

    private String authorize(
        HttpClient httpClient, String sessionCookie, String clientId, String redirectUri, String scope,
        @Nullable String resource) throws Exception {

        String authorizeQuery = "response_type=code" +
            "&client_id=" + clientId +
            "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
            "&state=state" +
            "&code_challenge=" + createCodeChallenge() +
            "&code_challenge_method=S256";

        if (resource != null) {
            authorizeQuery += "&resource=" + URLEncoder.encode(resource, StandardCharsets.UTF_8);
        }

        HttpRequest authorizeRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/oauth2/authorize?" + authorizeQuery))
            .header("Cookie", sessionCookie)
            .GET()
            .build();

        HttpResponse<String> authorizeResponse = httpClient.send(
            authorizeRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(authorizeResponse.statusCode()).isEqualTo(302);

        String location = authorizeResponse.headers()
            .firstValue("Location")
            .orElseThrow(() -> new IllegalStateException("No Location header on authorize response"));

        assertThat(location).startsWith(redirectUri);

        return extractQueryParameter(location, "code");
    }

    private HttpResponse<String> sendTokenRequest(
        HttpClient httpClient, @Nullable String basicCredentials, String tokenBody) throws Exception {

        HttpRequest.Builder tokenRequestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/oauth2/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(tokenBody));

        if (basicCredentials != null) {
            tokenRequestBuilder.header("Authorization", "Basic " + basicCredentials);
        }

        return httpClient.send(tokenRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void moveRotationBeforeTheGrace(String rotatedRefreshTokenValue) {
        TenantScope.run(ACME_TENANT_ID, () -> {
            OAuth2Authorization authorization = authorizationService.findByToken(
                rotatedRefreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);

            assertThat(authorization).isNotNull();

            Instant rotatedAt = Instant.now()
                .minus(APP_REFRESH_TOKEN_REUSE_GRACE)
                .minusSeconds(1);

            authorizationService.save(
                OAuth2Authorization.from(authorization)
                    .attribute(
                        AppRefreshTokenReuseAuthenticationProvider.REFRESH_TOKEN_ROTATED_AT_ATTRIBUTE,
                        rotatedAt.toString())
                    .build());
        });
    }

    private HttpResponse<String> sendRefreshRequest(HttpClient httpClient, String clientId, String refreshTokenValue)
        throws Exception {

        return sendTokenRequest(
            httpClient, null,
            "grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(refreshTokenValue, StandardCharsets.UTF_8) +
                "&client_id=" + clientId);
    }

    private HttpResponse<String> sendMcpRequest(HttpClient httpClient, String accessToken) throws Exception {
        HttpRequest mcpRequest = HttpRequest.newBuilder()
            .uri(URI.create(MCP_RESOURCE))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        return httpClient.send(mcpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private Jwt decodeAccessToken(String tokenResponse) {
        String accessToken = extractJsonStringField(tokenResponse, "access_token");

        assertThat(accessToken).isNotNull();

        return jwtDecoder.decode(accessToken);
    }

    private String loginIntoAcme(HttpClient httpClient) throws Exception {
        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=" + USER_LOGIN + "&password=password"))
            .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode()).isEqualTo(302);

        List<String> setCookies = loginResponse.headers()
            .allValues("Set-Cookie");

        String sessionCookie = setCookies.stream()
            .filter(cookie -> cookie.startsWith("JSESSIONID="))
            .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No JSESSIONID in login response"));

        HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + SESSION_TENANT_PATH + "?tenantId=" + ACME_TENANT_ID))
            .header("Cookie", sessionCookie)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> sessionTenantResponse = httpClient.send(
            sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(sessionTenantResponse.statusCode()).isEqualTo(204);

        return sessionCookie;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void deleteAllRows() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<String> schemas = new ArrayList<>(List.of(TenantContext.DEFAULT_TENANT_ID));

        for (String tenantId : TENANT_IDS) {
            schemas.add(getDatabaseSchema(tenantId));
        }

        for (String schema : schemas) {
            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization_consent");
            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization");
        }

        jdbcTemplate.update("DELETE FROM " + TenantContext.DEFAULT_TENANT_ID + ".oauth2_registered_client");
    }

    private static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    private static String createCodeChallenge() throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        byte[] digest = messageDigest.digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII));

        return EncodingUtils.urlEncodeBase64ToString(digest);
    }

    private static String extractQueryParameter(String url, String parameterName) {
        Matcher matcher = Pattern.compile("[?&]" + Pattern.quote(parameterName) + "=([^&]+)")
            .matcher(url);

        if (!matcher.find()) {
            throw new IllegalStateException("No " + parameterName + " parameter in URL: " + url);
        }

        return matcher.group(1);
    }

    @Nullable
    private static String extractJsonStringField(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"")
            .matcher(json);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    private static Optional<User> fetchAcmeUser(User user) {
        if (!ACME_TENANT_ID.equals(TenantContext.getCurrentTenantId())) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    @Configuration
    @EnableConfigurationProperties(McpResourceServerProperties.class)
    @Import({
        RegisteredClientIntTestConfiguration.class, AppClientAuthorizationTestConfiguration.McpTestController.class
    })
    static class AppClientAuthorizationTestConfiguration {

        @Bean
        McpServerSecretVerifier canonicalManagementMcpServerSecretVerifier() {
            return request -> {
                if (CANONICAL_MCP_PATH.equals(request.getServletPath())) {
                    return Optional.of(true);
                }

                return Optional.empty();
            };
        }

        @Bean
        McpEndpoint managementMcpEndpoint() {
            return TestMcpEndpoint.management();
        }

        @Bean
        McpOAuth2ResourceServerSecurityConfigurerContributor mcpOAuth2ResourceServerSecurityConfigurerContributor(
            ObjectProvider<McpEndpoint> mcpEndpointProvider,
            ObjectProvider<McpJwtDecoderFactory> mcpJwtDecoderFactoryProvider,
            McpResourceServerProperties mcpResourceServerProperties) {

            return new McpOAuth2ResourceServerSecurityConfigurerContributor(
                mcpEndpointProvider, mcpJwtDecoderFactoryProvider, mcpResourceServerProperties,
                new ApplicationProperties());
        }

        @Bean
        McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor(
            McpResourceServerProperties mcpResourceServerProperties, UserService userService,
            ObjectProvider<McpEndpoint> mcpEndpointProvider,
            ObjectProvider<McpServerSecretVerifier> mcpServerSecretVerifierProvider,
            ObjectProvider<SelfIssuedTokenRevocationChecker> selfIssuedTokenRevocationCheckerProvider) {

            return new McpJwtSecurityConfigurerContributor(
                mcpResourceServerProperties, userService, mock(AuthorityService.class), mcpEndpointProvider,
                mcpServerSecretVerifierProvider, selfIssuedTokenRevocationCheckerProvider, new ApplicationProperties());
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 1)
        SecurityFilterChain mcpResourceServerSecurityFilterChain(
            HttpSecurity http, McpOAuth2ResourceServerSecurityConfigurerContributor resourceServerContributor,
            McpJwtSecurityConfigurerContributor jwtContributor) throws Exception {

            return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest()
                    .authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .with(resourceServerContributor.getSecurityConfigurerAdapter(), Customizer.withDefaults())
                .with(jwtContributor.getSecurityConfigurerAdapter(), Customizer.withDefaults())
                .build();
        }

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        SecurityFilterChain appFormLoginSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest()
                    .authenticated())
                .formLogin(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new SessionTenantTestFilter(), AuthorizationFilter.class)
                .build();
        }

        @Bean
        @SuppressFBWarnings("HARD_CODE_PASSWORD")
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                org.springframework.security.core.userdetails.User.withUsername(USER_LOGIN)
                    .password("{noop}password")
                    .authorities("USER")
                    .build());
        }

        @RestController
        static class McpTestController {

            @GetMapping(CANONICAL_MCP_PATH)
            String getMcp() {
                return TenantContext.getCurrentTenantId();
            }
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
