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

package com.bytechef.platform.oauth2.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class OAuth2HttpClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private OAuth2HttpClient oAuth2HttpClient;

    @BeforeEach
    void beforeEach() {
        oAuth2HttpClient = createClient(List.of("localhost"), false);
    }

    static OAuth2HttpClient createClient(List<String> allowedHosts, boolean devProfile) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getOauth2()
            .getDiscovery()
            .setAllowedHosts(allowedHosts);

        return new OAuth2HttpClient(applicationProperties, createEnvironment(devProfile));
    }

    private static OAuth2HttpClient createClient(Duration timeout) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getOauth2()
            .getDiscovery()
            .setAllowedHosts(List.of("localhost"));

        return new OAuth2HttpClient(applicationProperties, createEnvironment(false), timeout);
    }

    private static Environment createEnvironment(boolean devProfile) {
        Environment environment = mock(Environment.class);

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(devProfile);

        return environment;
    }

    private static void assertOAuth2Error(ThrowingCallable callable, OAuth2ErrorType expected) {
        Throwable throwable = catchThrowable(callable);

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        ConfigurationException configurationException = (ConfigurationException) throwable;

        assertThat(configurationException.getEntityClass()).isEqualTo(OAuth2ErrorType.class);
        assertThat(configurationException.getErrorKey()).isEqualTo(expected.getErrorKey());
    }

    @Test
    void testConstructorTreatsNullAllowedHostsAsEmpty() {
        OAuth2HttpClient client = new OAuth2HttpClient(new ApplicationProperties(), createEnvironment(false));

        assertOAuth2Error(() -> client.validateUrl("http://localhost:1234/mcp"), OAuth2ErrorType.URL_NOT_ALLOWED);
    }

    @Test
    void testGetReturnsStatusBodyAndHeaders() {
        wireMock.stubFor(get(urlEqualTo("/metadata")).willReturn(
            aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"issuer\":\"x\"}")));

        OAuth2HttpClient.Response response = oAuth2HttpClient.get(wireMock.baseUrl() + "/metadata");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"issuer\":\"x\"}");
        assertThat(response.headers()
            .firstValue("Content-Type")).contains("application/json");
    }

    @Test
    void testGetDoesNotFollowRedirects() {
        wireMock.stubFor(get(urlEqualTo("/redirect")).willReturn(
            aResponse().withStatus(302)
                .withHeader("Location", wireMock.baseUrl() + "/metadata")));

        OAuth2HttpClient.Response response = oAuth2HttpClient.get(wireMock.baseUrl() + "/redirect");

        assertThat(response.statusCode()).isEqualTo(302);
    }

    @Test
    void testGetRejectsOversizedBody() {
        wireMock.stubFor(get(urlEqualTo("/big")).willReturn(aResponse().withStatus(200)
            .withBody("x".repeat(65 * 1024))));

        assertOAuth2Error(() -> oAuth2HttpClient.get(wireMock.baseUrl() + "/big"), OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testValidateUrlRejectsLoopbackHostThatIsNotAllowlisted() {
        OAuth2HttpClient strictClient = createClient(List.of(), false);

        assertOAuth2Error(() -> strictClient.validateUrl("http://localhost:1234/mcp"), OAuth2ErrorType.URL_NOT_ALLOWED);
    }

    @Test
    void testValidateUrlRejectsPlainHttpForPublicHost() {
        assertOAuth2Error(
            () -> oAuth2HttpClient.validateUrl("http://mcp.example.com/mcp"), OAuth2ErrorType.URL_NOT_ALLOWED);
    }

    @Test
    void testValidateUrlAllowsLocalhostUnderDevProfile() {
        OAuth2HttpClient devClient = createClient(List.of(), true);

        devClient.validateUrl("http://localhost:1234/mcp");
    }

    @Test
    void testPostSendsBodyAndHeaders() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(aResponse().withStatus(201)
            .withBody("{}")));

        OAuth2HttpClient.Response response = oAuth2HttpClient.post(
            wireMock.baseUrl() + "/register", "application/json", "{\"a\":1}", Map.of("X-Test", "yes"));

        assertThat(response.statusCode()).isEqualTo(201);

        wireMock.verify(
            postRequestedFor(urlEqualTo("/register"))
                .withHeader("X-Test", equalTo("yes"))
                .withRequestBody(equalToJson("{\"a\":1}")));
    }

    @Test
    void testGetFailsWithinDeadlineWhenBodyIsDripFed() {
        wireMock.stubFor(get(urlEqualTo("/drip")).willReturn(
            aResponse().withStatus(200)
                .withBody("x".repeat(1000))
                .withChunkedDribbleDelay(10, 8000)));

        OAuth2HttpClient shortClient = createClient(Duration.ofSeconds(1));
        long start = System.nanoTime();

        assertOAuth2Error(() -> shortClient.get(wireMock.baseUrl() + "/drip"), OAuth2ErrorType.METADATA_NOT_FOUND);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(4));
    }

    @Test
    void testProbeReturnsStatusWithoutWaitingForBody() {
        wireMock.stubFor(post(urlEqualTo("/stream")).willReturn(
            aResponse().withStatus(200)
                .withBody("x".repeat(1000))
                .withChunkedDribbleDelay(10, 8000)));

        OAuth2HttpClient shortClient = createClient(Duration.ofSeconds(5));
        long start = System.nanoTime();

        OAuth2HttpClient.Response response = shortClient.probe(wireMock.baseUrl() + "/stream", "{}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void testProbeReturnsWwwAuthenticateHeaderOn401() {
        wireMock.stubFor(post(urlEqualTo("/protected")).willReturn(
            aResponse().withStatus(401)
                .withHeader("WWW-Authenticate", "Bearer resource_metadata=\"https://x/.well-known/y\"")));

        OAuth2HttpClient.Response response = oAuth2HttpClient.probe(wireMock.baseUrl() + "/protected", "{}");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers()
            .firstValue("WWW-Authenticate")).hasValueSatisfying(
                value -> assertThat(value).contains("resource_metadata"));
    }
}
