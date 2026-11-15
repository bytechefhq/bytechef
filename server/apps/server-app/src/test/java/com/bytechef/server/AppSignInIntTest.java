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

package com.bytechef.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.facade.AppSharingFacade;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee",
        "bytechef.public-url=http://localhost"
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
    PostgreSQLContainerConfiguration.class, ServerApplicationIntTest.ServerApplicationIntTestConfiguration.class
})
class AppSignInIntTest {

    private static final String CLIENT_STATE = "state-1";
    private static final String CODE_VERIFIER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01";
    private static final String PASSWORD = "password";
    private static final String SCOPE = "openid profile app:use";
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final String USER_TOKEN_HEADER = "X-ByteChef-User-Token";

    @Autowired
    private AppFacade appFacade;

    @Autowired
    private AppInstanceFacade appInstanceFacade;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppSharingFacade appSharingFacade;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @LocalServerPort
    private int port;

    @Autowired
    private RegisteredClientService registeredClientService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMemberWithAppUseSignsInToAnInstanceThatRequiresSignIn() throws Exception {
        User owner = createUser("owner", AuthorityConstants.ADMIN, WorkspaceRole.ADMIN);
        User member = createUser("member", AuthorityConstants.USER, WorkspaceRole.VIEWER);

        AppInstance appInstance = createAppInstanceWithUrl(owner, ResourceVisibility.WORKSPACE)
            .appInstance();

        try (HttpClient httpClient = createHttpClient()) {
            String location = authorize(httpClient, login(httpClient, member), appInstance);

            assertThat(location).startsWith(appInstance.getUrl() + "/auth/callback?");
            assertThat(extractQueryParameter(location, "code")).isNotBlank();
            assertThat(extractQueryParameter(location, "state")).isEqualTo(CLIENT_STATE);
        }
    }

    @Test
    void testUserWithoutAppUseGetsAccessDenied() throws Exception {
        User owner = createUser("owner", AuthorityConstants.ADMIN, WorkspaceRole.ADMIN);
        User outsider = createUser("outsider", AuthorityConstants.USER, null);

        AppInstance appInstance = createAppInstanceWithUrl(owner, ResourceVisibility.WORKSPACE)
            .appInstance();

        try (HttpClient httpClient = createHttpClient()) {
            assertAccessDenied(authorize(httpClient, login(httpClient, outsider), appInstance), appInstance);
        }
    }

    @Test
    void testUserOfPrivateInstanceWithoutGrantGetsAccessDenied() throws Exception {
        User owner = createUser("owner", AuthorityConstants.ADMIN, WorkspaceRole.ADMIN);
        User member = createUser("member", AuthorityConstants.USER, WorkspaceRole.VIEWER);

        AppInstance appInstance = createAppInstanceWithUrl(owner, ResourceVisibility.PRIVATE)
            .appInstance();

        try (HttpClient httpClient = createHttpClient()) {
            assertAccessDenied(authorize(httpClient, login(httpClient, member), appInstance), appInstance);
        }
    }

    @Test
    void testSignedInMemberLosesMeWhenAccessIsWithdrawnAndWhenRemoved() throws Exception {
        User owner = createUser("owner", AuthorityConstants.ADMIN, WorkspaceRole.ADMIN);
        User member = createUser("member", AuthorityConstants.USER, WorkspaceRole.VIEWER);

        CreatedAppInstance createdAppInstance = createAppInstanceWithUrl(owner, ResourceVisibility.WORKSPACE);

        AppInstance appInstance = createdAppInstance.appInstance();

        try (HttpClient httpClient = createHttpClient()) {
            String location = authorize(httpClient, login(httpClient, member), appInstance);

            String accessToken = exchangeCode(
                httpClient, appInstance, Objects.requireNonNull(extractQueryParameter(location, "code")));

            HttpResponse<String> withoutTokenResponse = getMe(httpClient, createdAppInstance.secretKey(), null);

            assertThat(withoutTokenResponse.statusCode()).isEqualTo(401);
            assertThat(withoutTokenResponse.body()).contains("APP_USER_REQUIRED");

            HttpResponse<String> meResponse = getMe(httpClient, createdAppInstance.secretKey(), accessToken);

            assertThat(meResponse.statusCode())
                .as(meResponse.body())
                .isEqualTo(200);
            assertThat(meResponse.body()).contains("\"login\":\"" + member.getLogin() + "\"");
            assertThat(meResponse.body()).contains("APP_USE");

            authenticateAsTenantAdmin(owner.getLogin());

            try {
                appSharingFacade.setAppInstanceVisibility(
                    Workspace.DEFAULT_WORKSPACE_ID, Objects.requireNonNull(appInstance.getId()),
                    ResourceVisibility.PRIVATE);
            } finally {
                SecurityContextHolder.clearContext();
            }

            HttpResponse<String> privateResponse = getMe(httpClient, createdAppInstance.secretKey(), accessToken);

            assertThat(privateResponse.statusCode())
                .as(privateResponse.body())
                .isEqualTo(403);
            assertThat(privateResponse.body()).contains("APP_USER_FORBIDDEN");

            authenticateAsTenantAdmin(owner.getLogin());

            try {
                workspaceUserService.removeWorkspaceUser(
                    Objects.requireNonNull(member.getId()), Workspace.DEFAULT_WORKSPACE_ID);
            } finally {
                SecurityContextHolder.clearContext();
            }

            HttpResponse<String> removedResponse = getMe(httpClient, createdAppInstance.secretKey(), accessToken);

            assertThat(removedResponse.statusCode())
                .as(removedResponse.body())
                .isEqualTo(401);
            assertThat(removedResponse.body()).contains("APP_USER_TOKEN_INVALID");
        }
    }

    private void assertAccessDenied(String location, AppInstance appInstance) {
        assertThat(location).startsWith(appInstance.getUrl() + "/auth/callback?");
        assertThat(extractQueryParameter(location, "error")).isEqualTo("access_denied");
        assertThat(location).doesNotContain("code=");
    }

    private CreatedAppInstance createAppInstanceWithUrl(User owner, ResourceVisibility visibility) {
        authenticateAsTenantAdmin(owner.getLogin());

        try {
            AppSecretKeyDTO appSecretKey = appFacade.createApp(
                Workspace.DEFAULT_WORKSPACE_ID, Environment.DEVELOPMENT, "Sign-in " + UUID.randomUUID(), null,
                AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null, null, false);

            AppInstanceDTO createdAppInstance = appSecretKey.appInstance();

            if (visibility != ResourceVisibility.WORKSPACE) {
                appSharingFacade.setAppInstanceVisibility(
                    Workspace.DEFAULT_WORKSPACE_ID, createdAppInstance.id(), visibility);
            }

            assertThat(createdAppInstance.userAuthMode()).isEqualTo(AppUserAuthMode.REQUIRED);
            assertThat(appInstanceService.getAppInstance(createdAppInstance.id())
                .getRegisteredClientId()).isNull();

            String url = "https://crm-" + createdAppInstance.uuid() + ".example.com";

            appInstanceFacade.updateAppInstance(createdAppInstance.id(), null, null, url);

            AppInstance appInstance = appInstanceService.getAppInstance(createdAppInstance.id());

            String clientId = "app-" + appInstance.getUuid();

            assertThat(appInstance.getRegisteredClientId()).isEqualTo(clientId);
            assertThat(appInstance.getVisibility()).isEqualTo(visibility);

            RegisteredClientInfo registeredClient = registeredClientService.getRegisteredClients(true)
                .stream()
                .filter(registeredClientInfo -> Objects.equals(registeredClientInfo.clientId(), clientId))
                .findFirst()
                .orElseThrow();

            assertThat(registeredClient.redirectUris()).contains(url + "/auth/callback");
            assertThat(registeredClient.scopes()).containsExactlyInAnyOrder("openid", "profile", "app:use");

            return new CreatedAppInstance(appInstance, appSecretKey.secretKey());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private User createUser(String loginPrefix, String authorityName, @Nullable WorkspaceRole workspaceRole) {
        String login = (loginPrefix + "-" + UUID.randomUUID()).toLowerCase(Locale.ENGLISH);

        Set<Authority> authorities = authorityService.getAuthorities()
            .stream()
            .filter(authority -> Objects.equals(authority.getName(), authorityName) ||
                Objects.equals(authority.getName(), AuthorityConstants.USER))
            .collect(Collectors.toSet());

        User user = new User();

        user.setActivated(true);
        user.setAuthorities(authorities);
        user.setEmail(login + "@example.com");
        user.setFirstName(loginPrefix);
        user.setLangKey("en");
        user.setLastName("Tester");
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(PASSWORD));

        userService.save(user);

        User savedUser = userService.getUser(login);

        if (workspaceRole != null) {
            authenticateAsTenantAdmin(login);

            try {
                workspaceUserService.addWorkspaceUser(
                    Objects.requireNonNull(savedUser.getId()), Workspace.DEFAULT_WORKSPACE_ID, workspaceRole);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        return savedUser;
    }

    private String authorize(HttpClient httpClient, String cookies, AppInstance appInstance) throws Exception {
        String redirectUri = appInstance.getUrl() + "/auth/callback";

        String authorizeQuery = "response_type=code" +
            "&client_id=" + URLEncoder.encode("app-" + appInstance.getUuid(), StandardCharsets.UTF_8) +
            "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
            "&state=" + CLIENT_STATE +
            "&code_challenge=" + createCodeChallenge() +
            "&code_challenge_method=S256";

        HttpRequest authorizeRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/oauth2/authorize?" + authorizeQuery))
            .header("Accept", "text/html")
            .header("Cookie", cookies)
            .GET()
            .build();

        HttpResponse<String> authorizeResponse = httpClient.send(
            authorizeRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(authorizeResponse.statusCode())
            .as(authorizeResponse.body())
            .isEqualTo(302);

        return authorizeResponse.headers()
            .firstValue("Location")
            .orElseThrow();
    }

    private String exchangeCode(HttpClient httpClient, AppInstance appInstance, String code) throws Exception {
        String tokenForm = "grant_type=authorization_code" +
            "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
            "&redirect_uri=" + URLEncoder.encode(appInstance.getUrl() + "/auth/callback", StandardCharsets.UTF_8) +
            "&client_id=" + URLEncoder.encode("app-" + appInstance.getUuid(), StandardCharsets.UTF_8) +
            "&code_verifier=" + CODE_VERIFIER;

        HttpRequest tokenRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/oauth2/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(tokenForm))
            .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(tokenResponse.statusCode())
            .as(tokenResponse.body())
            .isEqualTo(200);

        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(tokenResponse.body());

        assertThat(matcher.find())
            .as(tokenResponse.body())
            .isTrue();

        return matcher.group(1);
    }

    private HttpResponse<String> getMe(HttpClient httpClient, String secretKey, @Nullable String userToken)
        throws Exception {

        HttpRequest.Builder meRequestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/api/app/v1/me"))
            .header("Authorization", "Bearer " + secretKey)
            .GET();

        if (userToken != null) {
            meRequestBuilder.header(USER_TOKEN_HEADER, userToken);
        }

        return httpClient.send(meRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String login(HttpClient httpClient, User user) throws Exception {
        HttpRequest csrfRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/api/account"))
            .GET()
            .build();

        HttpResponse<String> csrfResponse = httpClient.send(csrfRequest, HttpResponse.BodyHandlers.ofString());

        String csrfToken = findCookie(csrfResponse, "XSRF-TOKEN").orElseThrow(
            () -> new IllegalStateException("No XSRF-TOKEN cookie in response"));

        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/api/authentication"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", "XSRF-TOKEN=" + csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "username=" + URLEncoder.encode(user.getLogin(), StandardCharsets.UTF_8) +
                        "&password=" + PASSWORD))
            .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode())
            .as(loginResponse.body())
            .isEqualTo(200);

        String sessionId = findCookie(loginResponse, "JSESSIONID").orElseThrow(
            () -> new IllegalStateException("No JSESSIONID cookie in login response"));

        return "JSESSIONID=" + sessionId;
    }

    private static void authenticateAsTenantAdmin(String login) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    login, null, List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    private String baseUrl() {
        return "http://localhost:" + port;
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

    private static @Nullable String extractQueryParameter(String url, String parameterName) {
        String query = URI.create(url)
            .getRawQuery();

        for (String parameter : query.split("&")) {
            int separatorIndex = parameter.indexOf('=');

            if (separatorIndex > 0 && parameter.substring(0, separatorIndex)
                .equals(parameterName)) {

                return URLDecoder.decode(parameter.substring(separatorIndex + 1), StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    private static Optional<String> findCookie(HttpResponse<String> response, String cookieName) {
        return response.headers()
            .allValues("Set-Cookie")
            .stream()
            .filter(cookie -> cookie.startsWith(cookieName + "="))
            .map(cookie -> cookie.substring(cookieName.length() + 1, cookie.indexOf(';')))
            .findFirst();
    }

    private record CreatedAppInstance(AppInstance appInstance, String secretKey) {
    }
}
