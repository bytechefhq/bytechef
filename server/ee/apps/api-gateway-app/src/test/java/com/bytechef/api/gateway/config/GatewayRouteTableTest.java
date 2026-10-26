/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.gateway.server.mvc.config.PredicateProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @version ee
 */
class GatewayRouteTableTest {

    private static final String USER_SCHEMA_HEADER = "X-ByteChef-GraphQL-Schema";

    private static List<RouteProperties> routes;

    @BeforeAll
    static void beforeAll() throws IOException {
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(
            "api-gateway-app", new ClassPathResource("config/api-gateway-app.yml"));

        Binder binder = new Binder(
            ConfigurationPropertySources.from(propertySources), null,
            ApplicationConversionService.getSharedInstance());

        routes = binder.bind("spring.cloud.gateway.server.webmvc.routes", Bindable.listOf(RouteProperties.class))
            .orElse(List.of());
    }

    @ParameterizedTest
    @CsvSource({
        "/api/automation/internal/projects/1, configuration-app",
        "/api/automation/internal/workspaces/1/workflow-executions, execution-app",
        "/api/automation/internal/workspaces/1/projects, configuration-app",
        "/api/automation/internal/workflow-executions/5, execution-app",
        "/api/automation/internal/trigger-form/abc, execution-app",
        "/api/platform/internal/jobs/7, execution-app",
        "/api/platform/internal/trigger-executions, execution-app",
        "/api/platform/internal/approval-form/abc, execution-app",
        "/api/platform/internal/workflow-test-configurations/x, configuration-app",
        "/api/embedded/internal/workflow-executions, execution-app",
        "/api/embedded/internal/integrations, configuration-app",
        "/api/embedded/v1/ext-user/tools, execution-app",
        "/api/embedded/v1/ext-user/components/c/versions/1/actions/a, execution-app",
        "/api/embedded/v1/ext-user/components/c/connections, configuration-app",
        "/api/embedded/v1/ext-user/workflows/uuid, webhook-app",
        "/api/embedded/v1/app-events/1, webhook-app",
        "/api/embedded/v1/workflows/uuid, webhook-app",
        "/api/embedded/v1/integrations, configuration-app",
        "/api/embedded/v1/automation/workflows/uuid, configuration-embedded-automation",
        "/api/embedded/v1/ext-user/automation/workflows/uuid, configuration-app",
        "/api/embedded/v1/ext-user/integration-instances/1/workflows/uuid, configuration-app",
        "/webhooks/abc, webhook-app",
        "/webhooks/abc/test/environments/1, configuration-webhook-test",
        "/approvals/abc, webhook-app",
        "/callback, webhook-app",
        "/job/resume/abc, webhook-app",
        "/file-entries/abc/content, webhook-app",
        "/icons/slack.svg, configuration-app",
        "/graphql, configuration-app"
    })
    void testPathRoutesToApp(String path, String routeId) {
        assertThat(resolve(path, Map.of())).contains(routeId);
    }

    @Test
    void testUserGraphQlHeaderRoutesToGateway() {
        assertThat(resolve("/graphql", Map.of(USER_SCHEMA_HEADER, "user"))).contains("user-graphql");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/account", "/api/authentication", "/api/logout", "/api/register", "/api/activate",
        "/api/user/graphql", "/oauth2/authorization/google", "/login/oauth2/code/google", "/actuator/health"
    })
    void testGatewayOwnedPathsMatchNoRoute(String path) {
        assertThat(resolve(path, Map.of())).isEmpty();
    }

    private static Optional<String> resolve(String path, Map<String, String> headers) {
        for (RouteProperties route : routes) {
            if (matches(route, path, headers)) {
                return Optional.of(route.getId());
            }
        }

        return Optional.empty();
    }

    private static boolean matches(RouteProperties route, String path, Map<String, String> headers) {
        boolean pathMatched = false;

        for (PredicateProperties predicate : route.getPredicates()) {
            List<String> args = new ArrayList<>(predicate.getArgs()
                .values());

            switch (predicate.getName()) {
                case "Path" -> pathMatched = args.stream()
                    .map(pattern -> PathPatternParser.defaultInstance.parse(pattern.trim()))
                    .anyMatch(pathPattern -> pathPattern.matches(PathContainer.parsePath(path)));
                case "Header" -> {
                    String value = headers.get(args.get(0)
                        .trim());

                    if (value == null || !value.matches(args.get(1)
                        .trim())) {

                        return false;
                    }
                }
                default -> throw new IllegalStateException("Unsupported predicate " + predicate.getName());
            }
        }

        return pathMatched;
    }
}
