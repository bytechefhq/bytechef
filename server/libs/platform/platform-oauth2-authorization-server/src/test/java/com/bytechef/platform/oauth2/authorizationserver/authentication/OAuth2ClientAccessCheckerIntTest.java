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

package com.bytechef.platform.oauth2.authorizationserver.authentication;

import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.ACME_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.GLOBEX_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.getDatabaseSchema;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.oauth2.authorizationserver.authentication.OAuth2ClientAccessCheckerIntTest.OAuth2ClientAccessCheckerTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.authentication.OAuth2ClientAccessCheckerIntTest.OAuth2ClientAccessCheckerTestConfiguration.TestClientAccessChecker;
import com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerIntTestConfiguration;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = OAuth2ClientAccessCheckerTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee", "spring.liquibase.contexts=mono"
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
    MultiTenantSchemaIntTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
class OAuth2ClientAccessCheckerIntTest {

    private static final String CHECKED_CLIENT_ID = TestClientAccessChecker.SUPPORTED_CLIENT_ID_PREFIX + "client";
    private static final String CHECKED_REDIRECT_URI = "https://app.example.com/auth/callback";
    private static final String CHECKED_SCOPE = "openid profile app:use";
    private static final String CLIENT_STATE = "client-state-123";
    private static final String CODE_VERIFIER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01";
    private static final String FOREIGN_REDIRECT_URI = "https://attacker.example/callback";
    private static final String OTHER_CLIENT_ID = "other-client";
    private static final String OTHER_REDIRECT_URI = "http://127.0.0.1/callback";
    private static final String OTHER_SCOPE = "mcp:automation";
    private static final String SESSION_TENANT_PATH = "/test/session-tenant";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private TestClientAccessChecker testClientAccessChecker;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        deleteAllRows();

        testClientAccessChecker.reset();

        registeredClientRepository.save(
            createPublicClient(CHECKED_CLIENT_ID, CHECKED_REDIRECT_URI, List.of("openid", "profile", "app:use")));
        registeredClientRepository.save(createPublicClient(OTHER_CLIENT_ID, OTHER_REDIRECT_URI, List.of(OTHER_SCOPE)));
    }

    @AfterEach
    void afterEach() {
        deleteAllRows();
    }

    @Test
    void testAllowingCheckerLetsTheCodeBeIssued() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            String location = requestAuthorization(
                httpClient, loginIntoTenant(httpClient), CHECKED_CLIENT_ID, CHECKED_REDIRECT_URI, CHECKED_SCOPE);

            assertThat(location).startsWith(CHECKED_REDIRECT_URI + "?code=");
            assertThat(extractQueryParameter(location, "state")).isEqualTo(CLIENT_STATE);
            assertThat(testClientAccessChecker.getAllowedCheckCount()).isEqualTo(1);
            assertThat(testClientAccessChecker.getLastPrincipalName()).isEqualTo("user");
        }
    }

    @Test
    void testDeniedCheckerAnswersAccessDenied() throws Exception {
        testClientAccessChecker.setDecision(TestClientAccessChecker.Decision.DENY);

        try (HttpClient httpClient = createHttpClient()) {
            String location = requestAuthorization(
                httpClient, loginIntoTenant(httpClient), CHECKED_CLIENT_ID, CHECKED_REDIRECT_URI, CHECKED_SCOPE);

            assertAccessDenied(location);
        }
    }

    @Test
    void testThrowingCheckerIsTreatedAsADenial() throws Exception {
        testClientAccessChecker.setDecision(TestClientAccessChecker.Decision.THROW);

        try (HttpClient httpClient = createHttpClient()) {
            String location = requestAuthorization(
                httpClient, loginIntoTenant(httpClient), CHECKED_CLIENT_ID, CHECKED_REDIRECT_URI, CHECKED_SCOPE);

            assertAccessDenied(location);
        }
    }

    @Test
    void testCheckerNotSupportingClientIsIgnored() throws Exception {
        testClientAccessChecker.setDecision(TestClientAccessChecker.Decision.DENY);

        try (HttpClient httpClient = createHttpClient()) {
            String location = requestAuthorization(
                httpClient, loginIntoTenant(httpClient), OTHER_CLIENT_ID, OTHER_REDIRECT_URI, OTHER_SCOPE);

            assertThat(location).startsWith(OTHER_REDIRECT_URI + "?code=");
            assertThat(testClientAccessChecker.getAllowedCheckCount()).isZero();
        }
    }

    @Test
    void testCheckerIsNotConsultedBeforeTheUserSignsIn() throws Exception {
        testClientAccessChecker.setDecision(TestClientAccessChecker.Decision.DENY);

        try (HttpClient httpClient = createHttpClient()) {
            HttpResponse<String> authorizeResponse = sendAuthorizationRequest(
                httpClient, null, CHECKED_CLIENT_ID, CHECKED_REDIRECT_URI, CHECKED_SCOPE);

            assertThat(authorizeResponse.statusCode()).isEqualTo(302);
            assertThat(getLocation(authorizeResponse)).startsWith("/login?redirect=");
            assertThat(testClientAccessChecker.getAllowedCheckCount()).isZero();
        }
    }

    @Test
    void testCheckedClientRefusesAnUnregisteredRedirectUri() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            assertUnregisteredRedirectUriRefused(httpClient, CHECKED_CLIENT_ID, CHECKED_SCOPE);

            assertThat(testClientAccessChecker.getAllowedCheckCount()).isZero();
        }
    }

    @Test
    void testUncheckedClientRefusesAnUnregisteredRedirectUri() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            assertUnregisteredRedirectUriRefused(httpClient, OTHER_CLIENT_ID, OTHER_SCOPE);
        }
    }

    @Test
    void testCheckedClientRefusesAnUnregisteredScope() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            assertUnregisteredScopeRefused(httpClient, CHECKED_CLIENT_ID, CHECKED_REDIRECT_URI, CHECKED_SCOPE);

            assertThat(testClientAccessChecker.getAllowedCheckCount()).isZero();
        }
    }

    @Test
    void testUncheckedClientRefusesAnUnregisteredScope() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            assertUnregisteredScopeRefused(httpClient, OTHER_CLIENT_ID, OTHER_REDIRECT_URI, OTHER_SCOPE);
        }
    }

    @Test
    void testDynamicRegistrationStillRejectsAppScope() throws Exception {
        try (HttpClient httpClient = createHttpClient()) {
            for (String scope : List.of("app:use", "openid profile app:use", "mcp:automation app:use")) {
                HttpResponse<String> registrationResponse = registerClient(httpClient, scope);

                assertThat(registrationResponse.statusCode()).as(scope)
                    .isEqualTo(400);
                assertThat(registrationResponse.body()).contains("invalid_scope");
            }

            assertThat(countRows(TenantContext.DEFAULT_TENANT_ID, "oauth2_registered_client")).isEqualTo(2);
        }
    }

    private void assertUnregisteredRedirectUriRefused(HttpClient httpClient, String clientId, String scope)
        throws Exception {

        HttpResponse<String> authorizeResponse = sendAuthorizationRequest(
            httpClient, loginIntoTenant(httpClient), clientId, FOREIGN_REDIRECT_URI, scope);

        assertThat(authorizeResponse.statusCode()).isEqualTo(400);

        String location = authorizeResponse.headers()
            .firstValue("Location")
            .orElse("");

        assertThat(location).doesNotContain("attacker.example");
        assertThat(authorizeResponse.body()).doesNotContain("code=");

        assertNoAuthorizationStored();
    }

    private void assertUnregisteredScopeRefused(
        HttpClient httpClient, String clientId, String redirectUri, String scope) throws Exception {

        String location = requestAuthorization(
            httpClient, loginIntoTenant(httpClient), clientId, redirectUri, scope + " unregistered:scope");

        assertThat(location).startsWith(redirectUri + "?");
        assertThat(extractQueryParameter(location, "error")).isEqualTo("invalid_scope");
        assertThat(extractQueryParameter(location, "state")).isEqualTo(CLIENT_STATE);
        assertThat(location).doesNotContain("code=");

        assertNoAuthorizationStored();
    }

    private void assertNoAuthorizationStored() {
        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isZero();
        assertThat(countRows(TenantContext.DEFAULT_TENANT_ID, "oauth2_authorization")).isZero();
    }

    private void assertAccessDenied(String location) {
        assertThat(location).startsWith(CHECKED_REDIRECT_URI + "?");
        assertThat(extractQueryParameter(location, "error")).isEqualTo("access_denied");
        assertThat(extractQueryParameter(location, "state")).isEqualTo(CLIENT_STATE);
        assertThat(location).doesNotContain("code=");
        assertThat(testClientAccessChecker.getAllowedCheckCount()).isEqualTo(1);

        assertNoAuthorizationStored();
    }

    private HttpResponse<String> registerClient(HttpClient httpClient, String scope) throws Exception {
        HttpRequest registrationRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/oauth2/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"client_name\":\"Dynamic Client\",\"redirect_uris\":[\"" + CHECKED_REDIRECT_URI + "\"]," +
                    "\"grant_types\":[\"authorization_code\",\"refresh_token\"],\"response_types\":[\"code\"]," +
                    "\"token_endpoint_auth_method\":\"none\",\"scope\":\"" + scope + "\"}"))
            .build();

        return httpClient.send(registrationRequest, HttpResponse.BodyHandlers.ofString());
    }

    private String requestAuthorization(
        HttpClient httpClient, String sessionCookie, String clientId, String redirectUri, String scope)
        throws Exception {

        HttpResponse<String> authorizeResponse = sendAuthorizationRequest(
            httpClient, sessionCookie, clientId, redirectUri, scope);

        assertThat(authorizeResponse.statusCode()).isEqualTo(302);

        return getLocation(authorizeResponse);
    }

    private HttpResponse<String> sendAuthorizationRequest(
        HttpClient httpClient, @Nullable String sessionCookie, String clientId, String redirectUri, String scope)
        throws Exception {

        String authorizeQuery = "response_type=code" +
            "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
            "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
            "&state=" + CLIENT_STATE +
            "&code_challenge=" + createCodeChallenge() +
            "&code_challenge_method=S256";

        HttpRequest.Builder authorizeRequestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/oauth2/authorize?" + authorizeQuery))
            .header("Accept", "text/html")
            .GET();

        if (sessionCookie != null) {
            authorizeRequestBuilder.header("Cookie", sessionCookie);
        }

        return httpClient.send(authorizeRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
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

        String sessionCookie = setCookies.stream()
            .filter(cookie -> cookie.startsWith("JSESSIONID="))
            .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No JSESSIONID in login response"));

        HttpRequest sessionTenantRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + SESSION_TENANT_PATH + "?tenantId=" + ACME_TENANT_ID))
            .header("Cookie", sessionCookie)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> sessionTenantResponse = httpClient.send(
            sessionTenantRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(sessionTenantResponse.statusCode()).isEqualTo(204);

        return sessionCookie;
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
            TenantContext.DEFAULT_TENANT_ID, getDatabaseSchema(ACME_TENANT_ID), getDatabaseSchema(GLOBEX_TENANT_ID))) {

            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization_consent");
            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization");
        }

        jdbcTemplate.update("DELETE FROM " + TenantContext.DEFAULT_TENANT_ID + ".oauth2_registered_client");
    }

    private static RegisteredClient createPublicClient(String clientId, String redirectUri, List<String> scopes) {
        return RegisteredClient.withId(UUID.randomUUID()
            .toString())
            .clientId(clientId)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri(redirectUri)
            .scopes(registeredScopes -> registeredScopes.addAll(scopes))
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .reuseRefreshTokens(false)
                .build())
            .build();
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
    static class OAuth2ClientAccessCheckerTestConfiguration {

        @Bean
        TestClientAccessChecker testClientAccessChecker() {
            return new TestClientAccessChecker();
        }

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

        static final class TestClientAccessChecker implements OAuth2ClientAccessChecker {

            static final String SUPPORTED_CLIENT_ID_PREFIX = "checked-";

            private final AtomicInteger allowedCheckCount = new AtomicInteger();
            private final AtomicReference<Decision> decision = new AtomicReference<>(Decision.ALLOW);
            private final AtomicReference<String> lastPrincipalName = new AtomicReference<>();

            @Override
            public boolean supports(RegisteredClient registeredClient) {
                String clientId = registeredClient.getClientId();

                return clientId.startsWith(SUPPORTED_CLIENT_ID_PREFIX);
            }

            @Override
            public boolean isAllowed(RegisteredClient registeredClient, Authentication principal) {
                allowedCheckCount.incrementAndGet();
                lastPrincipalName.set(principal.getName());

                Decision currentDecision = decision.get();

                if (currentDecision == Decision.THROW) {
                    throw new IllegalStateException("Access check failed");
                }

                return currentDecision == Decision.ALLOW;
            }

            int getAllowedCheckCount() {
                return allowedCheckCount.get();
            }

            String getLastPrincipalName() {
                return lastPrincipalName.get();
            }

            void reset() {
                allowedCheckCount.set(0);
                decision.set(Decision.ALLOW);
                lastPrincipalName.set(null);
            }

            void setDecision(Decision decision) {
                this.decision.set(decision);
            }

            enum Decision {
                ALLOW, DENY, THROW
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
