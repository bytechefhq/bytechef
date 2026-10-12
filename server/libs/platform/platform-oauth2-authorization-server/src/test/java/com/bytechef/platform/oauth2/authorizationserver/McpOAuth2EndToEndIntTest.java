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
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.TENANT_IDS;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.getDatabaseSchema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.oauth2.authorizationserver.McpOAuth2EndToEndIntTest.McpOAuth2EndToEndTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantScope;
import com.bytechef.platform.security.web.config.McpJwtSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpOAuth2ResourceServerSecurityConfigurerContributor;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtDecoderFactory;
import com.bytechef.platform.user.domain.User;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
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
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
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
 * Runs the real chain end to end against one server that hosts both the embedded authorization server and the MCP
 * resource server: a logged-in user in tenant {@code acme} authorizes a client for the canonical management MCP
 * endpoint, the issued access token is accepted there, and after the grant is revoked the same token is rejected. The
 * resource server discovers the authorization server through its issuer metadata, as in production, so the issuer
 * ({@code bytechef.public-url}) and the self issuer URI both point at this server's port.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpOAuth2EndToEndTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee", "spring.liquibase.contexts=mono"
    },
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import({
    MultiTenantSchemaIntTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
class McpOAuth2EndToEndIntTest {

    private static final String CANONICAL_MCP_PATH = "/api/management/mcp";
    private static final String CLIENT_ID = "mcp-client";
    private static final String CLIENT_SECRET = "mcp-secret";
    private static final String CODE_VERIFIER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01";
    private static final int PORT = TestSocketUtils.findAvailableTcpPort();
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static final String REDIRECT_URI = "http://127.0.0.1/callback";
    private static final String REGISTERED_CLIENT_ID = "mcp-registered-client";
    private static final String SCOPE = "mcp:management";
    private static final String SESSION_TENANT_PATH = "/test/session-tenant";
    private static final String OTHER_USER_LOGIN = "other-user";
    private static final String USER_LOGIN = "user";

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private McpServerService mcpServerService;

    @Autowired
    private OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

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

        RegisteredClient registeredClient = RegisteredClient.withId(REGISTERED_CLIENT_ID)
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

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(userService.fetchUserByLogin(USER_LOGIN)).thenAnswer(invocation -> fetchAcmeUser(user));
        when(userService.fetchUserByLogin(OTHER_USER_LOGIN)).thenAnswer(invocation -> fetchAcmeUser(user));
    }

    @AfterEach
    void afterEach() {
        deleteAllRows();
    }

    @Test
    void testRevokedGrantRejectsItsAccessTokenOnTheMcpEndpoint() throws Exception {
        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()) {

            String accessToken = requestAccessToken(httpClient, USER_LOGIN);
            String otherAccessToken = requestAccessToken(httpClient, OTHER_USER_LOGIN);

            assertThat(countAuthorizations(getDatabaseSchema(ACME_TENANT_ID))).isEqualTo(2);
            assertThat(countAuthorizations(getDatabaseSchema(GLOBEX_TENANT_ID))).isZero();
            assertThat(countAuthorizations(TenantContext.DEFAULT_TENANT_ID)).isZero();

            for (String token : List.of(accessToken, otherAccessToken)) {
                HttpResponse<String> authorizedResponse = sendMcpRequest(httpClient, token);

                assertThat(authorizedResponse.statusCode()).isEqualTo(200);
                assertThat(authorizedResponse.body()).isEqualTo(ACME_TENANT_ID);
            }

            TenantScope.run(
                ACME_TENANT_ID, () -> oAuth2AuthorizationGrantService.revokeGrant(REGISTERED_CLIENT_ID, USER_LOGIN));

            assertThat(countAuthorizations(getDatabaseSchema(ACME_TENANT_ID))).isEqualTo(1);

            HttpResponse<String> revokedResponse = sendMcpRequest(httpClient, accessToken);

            assertThat(revokedResponse.statusCode()).isEqualTo(401);

            HttpResponse<String> unrevokedResponse = sendMcpRequest(httpClient, otherAccessToken);

            assertThat(unrevokedResponse.statusCode()).isEqualTo(200);
            assertThat(unrevokedResponse.body()).isEqualTo(ACME_TENANT_ID);
        }
    }

    private String requestAccessToken(HttpClient httpClient, String login) throws Exception {
        String sessionCookie = login(httpClient, login);

        HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + SESSION_TENANT_PATH + "?tenantId=" + ACME_TENANT_ID))
            .header("Cookie", sessionCookie)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> sessionTenantResponse = httpClient.send(
            sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(sessionTenantResponse.statusCode()).isEqualTo(204);

        String resource = URLEncoder.encode(BASE_URL + CANONICAL_MCP_PATH, StandardCharsets.UTF_8);

        String authorizeQuery = "response_type=code" +
            "&client_id=" + CLIENT_ID +
            "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
            "&code_challenge=" + createCodeChallenge() +
            "&code_challenge_method=S256" +
            "&resource=" + resource;

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

        String tokenBody = "grant_type=authorization_code" +
            "&code=" + extractQueryParameter(location, "code") +
            "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
            "&code_verifier=" + CODE_VERIFIER +
            "&resource=" + resource;

        HttpRequest tokenRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/oauth2/token"))
            .header("Authorization", "Basic " + EncodingUtils.base64EncodeToString(CLIENT_ID + ":" + CLIENT_SECRET))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
            .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(tokenResponse.statusCode()).isEqualTo(200);

        return extractJsonStringField(tokenResponse.body(), "access_token");
    }

    private HttpResponse<String> sendMcpRequest(HttpClient httpClient, String accessToken) throws Exception {
        HttpRequest mcpRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + CANONICAL_MCP_PATH))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        return httpClient.send(mcpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private String login(HttpClient httpClient, String login) throws Exception {
        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=" + login + "&password=password"))
            .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode()).isEqualTo(302);

        List<String> setCookies = loginResponse.headers()
            .allValues("Set-Cookie");

        return setCookies.stream()
            .filter(cookie -> cookie.startsWith("JSESSIONID="))
            .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No JSESSIONID in login response"));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private int countAuthorizations(String schema) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Integer count =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".oauth2_authorization", Integer.class);

        return count == null ? 0 : count;
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

    private static String extractJsonStringField(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"")
            .matcher(json);

        if (!matcher.find()) {
            throw new IllegalStateException("No " + fieldName + " field in JSON: " + json);
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
        RegisteredClientIntTestConfiguration.class, McpOAuth2EndToEndTestConfiguration.McpTestController.class
    })
    static class McpOAuth2EndToEndTestConfiguration {

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
        McpOAuth2ResourceServerSecurityConfigurerContributor mcpOAuth2ResourceServerSecurityConfigurerContributor(
            ObjectProvider<McpJwtDecoderFactory> mcpJwtDecoderFactoryProvider,
            McpResourceServerProperties mcpResourceServerProperties) {

            return new McpOAuth2ResourceServerSecurityConfigurerContributor(
                mcpJwtDecoderFactoryProvider, mcpResourceServerProperties);
        }

        @Bean
        McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor(
            McpResourceServerProperties mcpResourceServerProperties, UserService userService,
            ObjectProvider<McpServerSecretVerifier> mcpServerSecretVerifierProvider,
            ObjectProvider<SelfIssuedTokenRevocationChecker> selfIssuedTokenRevocationCheckerProvider) {

            return new McpJwtSecurityConfigurerContributor(
                mcpResourceServerProperties, userService, mcpServerSecretVerifierProvider,
                selfIssuedTokenRevocationCheckerProvider);
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
                    .build(),
                org.springframework.security.core.userdetails.User.withUsername(OTHER_USER_LOGIN)
                    .password("{noop}password")
                    .authorities("USER")
                    .build());
        }

        /**
         * Stands in for the MCP server on the canonical management endpoint and echoes the tenant it runs in.
         */
        @RestController
        static class McpTestController {

            @GetMapping(CANONICAL_MCP_PATH)
            String getMcp() {
                return TenantContext.getCurrentTenantId();
            }
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
    }
}
