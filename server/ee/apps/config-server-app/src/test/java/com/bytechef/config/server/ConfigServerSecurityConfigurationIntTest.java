/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.config.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConfigServerSecurityConfigurationIntTest.TestApp.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.security.user.name=configserver",
        "spring.security.user.password=test-secret",
        "spring.cloud.config.enabled=false"
    })
class ConfigServerSecurityConfigurationIntTest {

    @LocalServerPort
    private int port;

    private final RestClient restClient = RestClient.create();

    @Test
    void testUnauthenticatedRequestRejected() {
        HttpStatusCode statusCode = restClient
            .get()
            .uri(url("/test"))
            .exchange((request, response) -> response.getStatusCode());

        assertThat(statusCode.value()).isEqualTo(401);
    }

    @Test
    void testAuthenticatedRequestAllowed() {
        HttpStatusCode statusCode = restClient
            .get()
            .uri(url("/test"))
            .headers(httpHeaders -> httpHeaders.setBasicAuth("configserver", "test-secret"))
            .exchange((request, response) -> response.getStatusCode());

        assertThat(statusCode.value()).isEqualTo(200);
    }

    @Test
    void testWrongPasswordRejected() {
        HttpStatusCode statusCode = restClient
            .get()
            .uri(url("/test"))
            .headers(httpHeaders -> httpHeaders.setBasicAuth("configserver", "wrong"))
            .exchange((request, response) -> response.getStatusCode());

        assertThat(statusCode.value()).isEqualTo(401);
    }

    @Test
    void testHealthProbePermittedWithoutAuth() {
        HttpStatusCode statusCode = restClient
            .get()
            .uri(url("/actuator/health/liveness"))
            .exchange((request, response) -> response.getStatusCode());

        assertThat(statusCode.value()).isEqualTo(200);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // Plain @Configuration + @EnableAutoConfiguration (NOT @SpringBootApplication) with explicit @Import, so the
    // context does not component-scan com.bytechef.config.server and pull in the real ConfigServerApplication
    // (@EnableConfigServer, which would require a git/native backend).
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({
        ConfigServerSecurityConfiguration.class, ConfigServerSecurityConfigurationIntTest.StubController.class
    })
    static class TestApp {
    }

    @RestController
    static class StubController {

        @GetMapping("/test")
        String test() {
            return "ok";
        }

        // Stands in for the actuator liveness probe to verify the permit rule without pulling the actuator
        // endpoint infrastructure into the slice.
        @GetMapping("/actuator/health/liveness")
        String liveness() {
            return "UP";
        }
    }
}
