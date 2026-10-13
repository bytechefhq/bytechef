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
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
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
class OAuth2DynamicClientRegistrarTest {

    private static final String REDIRECT_URI = "https://bytechef.example.com/callback";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private OAuth2DynamicClientRegistrar registrar;

    @BeforeEach
    void beforeEach() {
        registrar = new OAuth2DynamicClientRegistrar(OAuth2HttpClientTest.createClient(List.of("localhost"), false));
    }

    private static void assertOAuth2Error(ThrowingCallable callable, OAuth2ErrorType expected) {
        Throwable throwable = catchThrowable(callable);

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        ConfigurationException configurationException = (ConfigurationException) throwable;

        assertThat(configurationException.getEntityClass()).isEqualTo(OAuth2ErrorType.class);
        assertThat(configurationException.getErrorKey()).isEqualTo(expected.getErrorKey());
    }

    private DiscoveredAuthorizationServer authorizationServer(String registrationEndpoint, List<String> authMethods) {
        return new DiscoveredAuthorizationServer(
            wireMock.baseUrl(), wireMock.baseUrl() + "/authorize", wireMock.baseUrl() + "/token",
            registrationEndpoint, wireMock.baseUrl() + "/mcp", List.of(), List.of("S256"), authMethods);
    }

    @Test
    void testRegisterRequestsPublicClientWhenNoneIsSupported() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201)
                .withBody("{\"client_id\":\"public-1\",\"token_endpoint_auth_method\":\"none\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of("client_secret_basic", "none")),
            REDIRECT_URI);

        assertThat(registeredClient).isEqualTo(new RegisteredClient("public-1", null, "none"));

        wireMock.verify(postRequestedFor(urlEqualTo("/register")).withRequestBody(equalToJson("""
            {"client_name":"ByteChef","redirect_uris":["%s"],"grant_types":["authorization_code","refresh_token"],\
             "response_types":["code"],"token_endpoint_auth_method":"none"}""".formatted(REDIRECT_URI))));
    }

    @Test
    void testRegisterFallsBackToClientSecretBasicThenPost() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201)
                .withBody(
                    "{\"client_id\":\"c\",\"client_secret\":\"s\",\"token_endpoint_auth_method\":\"client_secret_post\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of("client_secret_post")), REDIRECT_URI);

        assertThat(registeredClient.tokenEndpointAuthMethod()).isEqualTo("client_secret_post");
        assertThat(registeredClient.clientSecret()).isEqualTo("s");
    }

    @Test
    void testRegisterUsesAuthMethodFromResponse() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201)
                .withBody(
                    "{\"client_id\":\"c\",\"client_secret\":\"s\",\"token_endpoint_auth_method\":\"client_secret_basic\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI);

        assertThat(registeredClient.tokenEndpointAuthMethod()).isEqualTo("client_secret_basic");
    }

    @Test
    void testRegisterDefaultsToRequestedMethodWhenResponseOmitsIt() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201)
                .withBody("{\"client_id\":\"c\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI);

        assertThat(registeredClient.tokenEndpointAuthMethod()).isEqualTo("none");
    }

    @Test
    void testRegisterFailsWithoutRegistrationEndpoint() {
        assertOAuth2Error(
            () -> registrar.register(authorizationServer(null, List.of("none")), REDIRECT_URI),
            OAuth2ErrorType.REGISTRATION_NOT_SUPPORTED);
    }

    @Test
    void testRegisterPassesThroughServerErrorDescription() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(400)
                .withBody("{\"error\":\"invalid_redirect_uri\",\"error_description\":\"redirect not allowed\"}")));

        ThrowingCallable callable = () -> registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI);

        assertOAuth2Error(callable, OAuth2ErrorType.REGISTRATION_REJECTED);

        assertThat(catchThrowable(callable)).hasMessageContaining("redirect not allowed");
    }

    @Test
    void testRegisterCapsRejectionMessageLength() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(400)
                .withBody("{\"error_description\":\"" + "x".repeat(5000) + "\"}")));

        Throwable throwable = catchThrowable(() -> registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI));

        assertThat(throwable.getMessage()).hasSizeLessThan(700);
    }

    @Test
    void testRegisterFailsWhenResponseHasNoClientId() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201)
                .withBody("{}")));

        assertOAuth2Error(
            () -> registrar.register(authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI),
            OAuth2ErrorType.REGISTRATION_REJECTED);
    }
}
