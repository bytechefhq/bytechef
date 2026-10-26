/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * @version ee
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
class ApiGatewayLoginIntTest {

    static final String LOGIN = "login-test@localhost.com";
    static final String PASSWORD = "secret-password";

    @LocalServerPort
    private int port;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        createActivatedUser(userService, LOGIN, PASSWORD);
    }

    @Test
    void testFormLoginWritesSessionToRedis() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = new GatewayBrowser(port);

        HttpResponse<String> response = gatewayBrowser.login(LOGIN, PASSWORD, false);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(sessionLogin(gatewayBrowser)).contains(LOGIN);
    }

    @Test
    void testAccountUsesTheSession() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = new GatewayBrowser(port);

        gatewayBrowser.login(LOGIN, PASSWORD, false);

        HttpResponse<String> response = gatewayBrowser.get("/api/account");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"login\":\"" + LOGIN + "\"");
    }

    @Test
    void testRememberMeRestoresSessionReadableFromRedis() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = new GatewayBrowser(port);

        gatewayBrowser.login(LOGIN, PASSWORD, true);

        String rememberMe = gatewayBrowser.cookie("remember-me")
            .orElseThrow();

        GatewayBrowser reloadedGatewayBrowser = new GatewayBrowser(port);

        reloadedGatewayBrowser.addCookie("remember-me", rememberMe);

        HttpResponse<String> response = reloadedGatewayBrowser.get("/api/account");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(sessionLogin(reloadedGatewayBrowser)).contains(LOGIN);
    }

    @Test
    void testLogoutDeletesTheSession() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = new GatewayBrowser(port);

        gatewayBrowser.login(LOGIN, PASSWORD, false);

        String sessionId = gatewayBrowser.sessionId()
            .orElseThrow();

        HttpResponse<String> response = gatewayBrowser.postForm("/api/logout", Map.of(), true);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(sessionRepository.findById(sessionId)).isNull();
    }

    @Test
    void testProxiedPathIsNotAuthenticatedByGateway() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = new GatewayBrowser(port);

        HttpResponse<String> response = gatewayBrowser.postJson(
            "/api/automation/internal/projects", "{}", Map.of(), false);

        assertThat(response.statusCode()).as(response.body())
            .isNotIn(401, 403);
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles({
        "testint", "prod"
    })
    class StaticClient {

        @TempDir
        static Path clientPath;

        @LocalServerPort
        private int staticClientPort;

        @DynamicPropertySource
        static void properties(DynamicPropertyRegistry registry) throws IOException {
            Files.writeString(clientPath.resolve("index.html"), "<html>bytechef-client</html>");

            registry.add("bytechef.resources.web", () -> clientPath.toUri()
                .toString());
        }

        @Test
        void testSpaPathServesIndex() throws IOException, InterruptedException {
            GatewayBrowser gatewayBrowser = new GatewayBrowser(staticClientPort);

            HttpResponse<String> response = gatewayBrowser.get("/automation/projects");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("bytechef-client");
        }

        @Test
        void testRoutedPathIsNotServedTheIndex() throws IOException, InterruptedException {
            GatewayBrowser gatewayBrowser = new GatewayBrowser(staticClientPort);

            HttpResponse<String> response = gatewayBrowser.get("/webhooks/abc");

            assertThat(response.body()).doesNotContain("bytechef-client");
        }
    }

    static void createActivatedUser(UserService userService, String login, String password) {
        Optional<User> existingUser = userService.fetchUserByLogin(login);

        if (existingUser.isPresent()) {
            return;
        }

        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setEmail(login);
        adminUserDTO.setLogin(login);

        User user = userService.registerUser(adminUserDTO, password);

        user.setActivated(true);

        userService.save(user);
    }

    private Optional<String> sessionLogin(GatewayBrowser gatewayBrowser) {
        return gatewayBrowser.sessionId()
            .map(sessionRepository::findById)
            .map(session -> session.<SecurityContext>getAttribute("SPRING_SECURITY_CONTEXT"))
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName);
    }
}
