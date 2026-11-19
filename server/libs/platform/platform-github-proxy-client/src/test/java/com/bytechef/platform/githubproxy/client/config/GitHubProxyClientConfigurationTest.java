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

package com.bytechef.platform.githubproxy.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.githubproxy.client.OAuthAppProxyClient;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Ivica Cardic
 */
class GitHubProxyClientConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(GitHubProxyClientConfiguration.class);

    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();
    private final AtomicInteger redirectTargetHits = new AtomicInteger();

    private HttpServer httpServer;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/oauth-apps", this::handleOAuthApps);
        httpServer.createContext("/redirect/oauth-apps", this::handleRedirect);
        httpServer.createContext("/elsewhere", this::handleRedirectTarget);

        httpServer.start();
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);
    }

    @Test
    void testOAuthAppProxyClientSendsBearerKeyFromProperties() {
        contextRunner.withPropertyValues("github-proxy.base-url=" + baseUrl(""), "github-proxy.oauth-apps.api-key=k1")
            .run(context -> {
                OAuthAppProxyClient oAuthAppProxyClient = context.getBean(OAuthAppProxyClient.class);

                Map<String, OAuthApp> oAuthApps = oAuthAppProxyClient.getOAuthApps();

                assertThat(oAuthApps).containsExactly(Map.entry("googleDrive", new OAuthApp("id", "secret")));
                assertThat(receivedAuthorization.get()).isEqualTo("Bearer k1");
            });
    }

    @Test
    void testOAuthAppProxyClientSendsBearerKeyFromEnvironmentVariable() {
        contextRunner.withInitializer(context -> setEnvironmentVariable(
            context.getEnvironment(), "GITHUB_PROXY_OAUTH_APPS_API_KEY", "k1"))
            .withPropertyValues("github-proxy.base-url=" + baseUrl(""))
            .run(context -> {
                OAuthAppProxyClient oAuthAppProxyClient = context.getBean(OAuthAppProxyClient.class);

                oAuthAppProxyClient.getOAuthApps();

                assertThat(receivedAuthorization.get()).isEqualTo("Bearer k1");
            });
    }

    @Test
    void testOAuthAppProxyClientDoesNotFollowRedirects() {
        contextRunner
            .withPropertyValues("github-proxy.base-url=" + baseUrl("/redirect"), "github-proxy.oauth-apps.api-key=k1")
            .run(context -> {
                OAuthAppProxyClient oAuthAppProxyClient = context.getBean(OAuthAppProxyClient.class);

                assertThatThrownBy(oAuthAppProxyClient::getOAuthApps)
                    .isInstanceOfSatisfying(
                        RestClientResponseException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FOUND));
                assertThat(redirectTargetHits.get()).isZero();
            });
    }

    @Test
    void testNoOAuthAppProxyClientWithoutApiKey() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(OAuthAppProxyClient.class));
    }

    @Test
    void testNoOAuthAppProxyClientWithBlankApiKey() {
        contextRunner.withPropertyValues("github-proxy.oauth-apps.api-key=")
            .run(context -> assertThat(context).doesNotHaveBean(OAuthAppProxyClient.class));
    }

    @Test
    void testOAuthAppProxyClientPresentWithApiKey() {
        contextRunner.withPropertyValues("github-proxy.oauth-apps.api-key=k1")
            .run(context -> assertThat(context).hasSingleBean(OAuthAppProxyClient.class));
    }

    @Test
    void testApiKeyBindsFromNestedOAuthAppsProperty() {
        contextRunner.withPropertyValues("github-proxy.oauth-apps.api-key=k1")
            .run(context -> {
                GitHubProxyClientProperties properties = context.getBean(GitHubProxyClientProperties.class);

                GitHubProxyClientProperties.OauthApps oauthApps = properties.getOauthApps();

                assertThat(oauthApps.getApiKey()).isEqualTo("k1");
            });
    }

    @Test
    void testOAuthAppProxyClientPresentWithApiKeyContainingQuote() {
        contextRunner.withPropertyValues("github-proxy.oauth-apps.api-key=k'1")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(OAuthAppProxyClient.class);
            });
    }

    private String baseUrl(String path) {
        InetSocketAddress address = httpServer.getAddress();

        return "http://127.0.0.1:" + address.getPort() + path;
    }

    private void handleOAuthApps(HttpExchange exchange) throws IOException {
        Headers requestHeaders = exchange.getRequestHeaders();

        receivedAuthorization.set(requestHeaders.getFirst("Authorization"));

        byte[] body = "{\"googleDrive\":{\"clientId\":\"id\",\"clientSecret\":\"secret\"}}"
            .getBytes(StandardCharsets.UTF_8);

        Headers responseHeaders = exchange.getResponseHeaders();

        responseHeaders.add("Content-Type", "application/json");

        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private void handleRedirect(HttpExchange exchange) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();

        responseHeaders.add("Location", baseUrl("/elsewhere"));

        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void handleRedirectTarget(HttpExchange exchange) throws IOException {
        redirectTargetHits.incrementAndGet();

        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private static void setEnvironmentVariable(ConfigurableEnvironment environment, String name, String value) {
        MutablePropertySources propertySources = environment.getPropertySources();

        Map<String, Object> environmentVariables = new HashMap<>(System.getenv());

        environmentVariables.put(name, value);

        propertySources.replace(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environmentVariables));
    }
}
