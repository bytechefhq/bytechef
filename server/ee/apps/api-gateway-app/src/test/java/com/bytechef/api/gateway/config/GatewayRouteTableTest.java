/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcPropertiesBeanDefinitionRegistrar;
import org.springframework.cloud.gateway.server.mvc.filter.FilterAutoConfiguration;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctionAutoConfiguration;
import org.springframework.cloud.gateway.server.mvc.predicate.PredicateAutoConfiguration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * @version ee
 */
class GatewayRouteTableTest {

    private static final String USER_SCHEMA_HEADER = "X-ByteChef-GraphQL-Schema";

    private static RouterFunction<?> gatewayRouterFunction;
    private static Map<String, RouterFunction<?>> routeRouterFunctions;

    @BeforeAll
    static void beforeAll() throws IOException {
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(
            "api-gateway-app", new ClassPathResource("config/api-gateway-app.yml"));

        new ApplicationContextRunner()
            .withInitializer(applicationContext -> {
                ConfigurableEnvironment configurableEnvironment = applicationContext.getEnvironment();

                MutablePropertySources mutablePropertySources = configurableEnvironment.getPropertySources();

                propertySources.forEach(mutablePropertySources::addLast);
            })
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withConfiguration(
                AutoConfigurations.of(
                    FilterAutoConfiguration.class, GatewayServerMvcAutoConfiguration.class,
                    HandlerFunctionAutoConfiguration.class, PredicateAutoConfiguration.class))
            .run(applicationContext -> {
                GatewayMvcPropertiesBeanDefinitionRegistrar.RouterFunctionHolder routerFunctionHolder =
                    applicationContext.getBean(GatewayMvcPropertiesBeanDefinitionRegistrar.RouterFunctionHolder.class);

                gatewayRouterFunction = routerFunctionHolder.getRouterFunction();

                RouteRouterFunctionsVisitor routeRouterFunctionsVisitor = new RouteRouterFunctionsVisitor();

                gatewayRouterFunction.accept(routeRouterFunctionsVisitor);

                routeRouterFunctions = routeRouterFunctionsVisitor.routeRouterFunctions;
            });
    }

    @ParameterizedTest
    @CsvSource({
        "/api/automation/abc123/mcp, configuration-app",
        "/api/automation/internal/projects/1, configuration-app",
        "/api/management/mcp, configuration-app",
        "/api/automation/internal/workspaces/1/workflow-executions, execution-app",
        "/api/automation/internal/workspaces/1/projects, configuration-app",
        "/api/automation/internal/workflow-executions/5, execution-app",
        "/api/automation/internal/trigger-form/abc, execution-app",
        "/api/automation/v1/projects/deploy, configuration-app",
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
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", path);

        headers.forEach(mockHttpServletRequest::addHeader);

        ServerRequest serverRequest = ServerRequest.create(mockHttpServletRequest, List.of());

        Optional<? extends HandlerFunction<?>> handlerFunction = gatewayRouterFunction.route(serverRequest);

        if (handlerFunction.isEmpty()) {
            return Optional.empty();
        }

        for (Map.Entry<String, RouterFunction<?>> entry : routeRouterFunctions.entrySet()) {
            RouterFunction<?> routeRouterFunction = entry.getValue();

            Optional<? extends HandlerFunction<?>> routeHandlerFunction = routeRouterFunction.route(serverRequest);

            if (routeHandlerFunction.isPresent()) {
                return Optional.of(entry.getKey());
            }
        }

        throw new IllegalStateException("The gateway routed " + path + " through no configured route");
    }

    private static final class RouteRouterFunctionsVisitor implements RouterFunctions.Visitor {

        private Map<String, RouterFunction<?>> routeRouterFunctions = new LinkedHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public void attributes(Map<String, Object> attributes) {
            Object gatewayRouterFunctions = attributes.get("gatewayRouterFunctions");

            if (gatewayRouterFunctions instanceof Map<?, ?> gatewayRouterFunctionMap) {
                routeRouterFunctions = new LinkedHashMap<>((Map<String, RouterFunction<?>>) gatewayRouterFunctionMap);
            }
        }

        @Override
        public void endNested(RequestPredicate requestPredicate) {
        }

        @Override
        public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
        }

        @Override
        public void route(RequestPredicate requestPredicate, HandlerFunction<?> handlerFunction) {
        }

        @Override
        public void startNested(RequestPredicate requestPredicate) {
        }

        @Override
        public void unknown(RouterFunction<?> routerFunction) {
        }
    }
}
