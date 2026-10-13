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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OAuth2DiscoveryClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private OAuth2DiscoveryClient oAuth2DiscoveryClient;
    private String baseUrl;

    @BeforeEach
    void beforeEach() {
        baseUrl = wireMock.baseUrl();
        oAuth2DiscoveryClient = new OAuth2DiscoveryClient(
            OAuth2HttpClientTest.createClient(List.of("localhost"), false));
    }

    private static void assertOAuth2Error(ThrowingCallable callable, OAuth2ErrorType expected) {
        Throwable throwable = catchThrowable(callable);

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        ConfigurationException configurationException = (ConfigurationException) throwable;

        assertThat(configurationException.getEntityClass()).isEqualTo(OAuth2ErrorType.class);
        assertThat(configurationException.getErrorKey()).isEqualTo(expected.getErrorKey());
    }

    private void stubAuthorizationServer(String issuerPath, String extraJson) {
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server" + issuerPath)).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {"issuer":"%s%s","authorization_endpoint":"%s/authorize","token_endpoint":"%s/token",\
                     "registration_endpoint":"%s/register","code_challenge_methods_supported":["S256"],\
                     "token_endpoint_auth_methods_supported":["none","client_secret_basic"]%s}\
                    """.formatted(baseUrl, issuerPath, baseUrl, baseUrl, baseUrl, extraJson))));
    }

    private void stubProtectedResource(String metadataPath, String scopesJson) {
        wireMock.stubFor(get(urlEqualTo(metadataPath)).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {"resource":"%s/mcp","authorization_servers":["%s"]%s}\
                    """.formatted(baseUrl, baseUrl, scopesJson))));
    }

    @Test
    void testDiscoverPrefersResourceMetadataFromWwwAuthenticateHeader() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(
            aResponse().withStatus(401)
                .withHeader("WWW-Authenticate",
                    "Bearer resource_metadata=\"" + baseUrl + "/.well-known/oauth-protected-resource/custom\"")));
        stubProtectedResource("/.well-known/oauth-protected-resource/custom", ",\"scopes_supported\":[\"read\"]");
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", ",\"scopes_supported\":[\"wrong\"]");
        stubAuthorizationServer("", "");

        DiscoveredAuthorizationServer authorizationServer = oAuth2DiscoveryClient.discover(baseUrl + "/mcp");

        assertThat(authorizationServer.scopes()).containsExactly("read");
        assertThat(authorizationServer.resource()).isEqualTo(baseUrl + "/mcp");
        assertThat(authorizationServer.registrationEndpoint()).isEqualTo(baseUrl + "/register");
    }

    @Test
    void testDiscoverFallsBackToWellKnownPathWithResourcePath() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", "");
        stubAuthorizationServer("", ",\"scopes_supported\":[\"as-scope\"]");

        DiscoveredAuthorizationServer authorizationServer = oAuth2DiscoveryClient.discover(baseUrl + "/mcp");

        assertThat(authorizationServer.scopes()).containsExactly("as-scope");
    }

    @Test
    void testDiscoverTriesOpenIdConfigurationForIssuerWithPath() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource/mcp")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"resource\":\"%s/mcp\",\"authorization_servers\":[\"%s/tenant\"]}"
                    .formatted(baseUrl, baseUrl))));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server/tenant"))
            .willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/openid-configuration/tenant"))
            .willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(get(urlEqualTo("/tenant/.well-known/openid-configuration")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"issuer\":\"%s/tenant\",\"authorization_endpoint\":\"%s/a\",\"token_endpoint\":\"%s/t\"}"
                    .formatted(baseUrl, baseUrl, baseUrl))));

        DiscoveredAuthorizationServer authorizationServer = oAuth2DiscoveryClient.discover(baseUrl + "/mcp");

        assertThat(authorizationServer.issuer()).isEqualTo(baseUrl + "/tenant");
        assertThat(authorizationServer.registrationEndpoint()).isNull();
        assertThat(authorizationServer.codeChallengeMethods()).isEmpty();
    }

    @Test
    void testDiscoverRejectsResourceFromAnotherOrigin() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource/mcp")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"resource\":\"https://evil.example.com/mcp\",\"authorization_servers\":[\"%s\"]}"
                    .formatted(baseUrl))));

        assertOAuth2Error(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"), OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testDiscoverRejectsIssuerMismatch() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", "");
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"issuer\":\"https://other.example.com\",\"authorization_endpoint\":\"%s/a\","
                    .formatted(baseUrl) + "\"token_endpoint\":\"%s/t\"}".formatted(baseUrl))));

        assertOAuth2Error(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"), OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testDiscoverRejectsAuthorizationServerWithoutS256() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", "");
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server")).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {"issuer":"%s","authorization_endpoint":"%s/a","token_endpoint":"%s/t",\
                     "code_challenge_methods_supported":["plain"]}\
                    """.formatted(baseUrl, baseUrl, baseUrl))));

        assertOAuth2Error(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"), OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testDiscoverFailsWhenServerDoesNotRequireAuthorization() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(200)
            .withBody("{}")));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource/mcp"))
            .willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource"))
            .willReturn(aResponse().withStatus(404)));

        assertOAuth2Error(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"), OAuth2ErrorType.METADATA_NOT_FOUND);
    }

    @Test
    void testDiscoverRejectsBlockedAuthorizationEndpoint() {
        assertEndpointRejected("http://10.0.0.5/authorize", baseUrl + "/token", baseUrl + "/register");
    }

    @Test
    void testDiscoverRejectsBlockedTokenEndpoint() {
        assertEndpointRejected(baseUrl + "/authorize", "http://10.0.0.5/token", baseUrl + "/register");
    }

    @Test
    void testDiscoverRejectsBlockedRegistrationEndpoint() {
        assertEndpointRejected(baseUrl + "/authorize", baseUrl + "/token", "http://10.0.0.5/register");
    }

    private void
        assertEndpointRejected(String authorizationEndpoint, String tokenEndpoint, String registrationEndpoint) {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", "");
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server")).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {"issuer":"%s","authorization_endpoint":"%s","token_endpoint":"%s",\
                     "registration_endpoint":"%s","code_challenge_methods_supported":["S256"]}\
                    """.formatted(baseUrl, authorizationEndpoint, tokenEndpoint, registrationEndpoint))));

        assertOAuth2Error(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"), OAuth2ErrorType.URL_NOT_ALLOWED);
    }

    @Test
    void testDiscoverRejectsBlockedProtectedResourceUrl() {
        assertOAuth2Error(() -> oAuth2DiscoveryClient.discover("http://10.0.0.5/mcp"), OAuth2ErrorType.URL_NOT_ALLOWED);
    }
}
