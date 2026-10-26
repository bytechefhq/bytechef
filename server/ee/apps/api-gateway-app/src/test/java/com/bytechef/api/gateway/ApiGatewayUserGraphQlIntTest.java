/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
class ApiGatewayUserGraphQlIntTest {

    private static final String ADMIN_LOGIN = "graphql-admin@localhost.com";
    private static final String ADMIN_PASSWORD = "secret-password";
    private static final Map<String, String> USER_SCHEMA_HEADERS = Map.of("X-ByteChef-GraphQL-Schema", "user");

    @Autowired
    private AuthorityService authorityService;

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        ApiGatewayLoginIntTest.createActivatedUser(userService, ADMIN_LOGIN, ADMIN_PASSWORD);

        User user = userService.getUser(ADMIN_LOGIN);

        Set<Authority> authorities = Set.copyOf(authorityService.getAuthorities());

        user.setAuthorities(authorities);

        userService.save(user);
    }

    @Test
    void testUsersQueryThroughHeaderRoute() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = loggedInAdmin();

        HttpResponse<String> response = gatewayBrowser.postJson(
            "/graphql", "{\"query\":\"query users { users(pageNumber: 0, pageSize: 10) { content { login } } }\"}",
            USER_SCHEMA_HEADERS, true);

        assertThat(response.statusCode()).as(response.body())
            .isEqualTo(200);
        assertThat(response.body()).contains("\"login\":\"" + ADMIN_LOGIN + "\"");
    }

    @Test
    void testAuthoritiesQueryThroughHeaderRoute() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = loggedInAdmin();

        HttpResponse<String> response = gatewayBrowser.postJson(
            "/graphql", "{\"query\":\"query authorities { authorities }\"}", USER_SCHEMA_HEADERS, true);

        assertThat(response.statusCode()).as(response.body())
            .isEqualTo(200);
        assertThat(response.body()).contains("ROLE_ADMIN");
    }

    @Test
    void testUserGraphQlRequiresAuthentication() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = new GatewayBrowser(port);

        gatewayBrowser.get("/api/account");

        HttpResponse<String> response = gatewayBrowser.postJson(
            "/graphql", "{\"query\":\"query authorities { authorities }\"}", USER_SCHEMA_HEADERS, true);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void testUserGraphQlMutationWithoutCsrfTokenIsForbidden() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = loggedInAdmin();

        HttpResponse<String> response = gatewayBrowser.postJson(
            "/graphql", "{\"query\":\"mutation { deleteUser(login: \\\"x\\\") }\"}", USER_SCHEMA_HEADERS, false);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private GatewayBrowser loggedInAdmin() throws IOException, InterruptedException {
        GatewayBrowser gatewayBrowser = new GatewayBrowser(port);

        gatewayBrowser.login(ADMIN_LOGIN, ADMIN_PASSWORD, false);

        return gatewayBrowser;
    }
}
