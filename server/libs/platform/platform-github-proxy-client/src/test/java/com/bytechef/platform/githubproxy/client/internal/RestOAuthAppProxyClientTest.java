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

package com.bytechef.platform.githubproxy.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Ivica Cardic
 */
class RestOAuthAppProxyClientTest {

    @Test
    void testGetOAuthAppsSendsBearerAndParsesMap() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://proxy.test")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer k1");

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("http://proxy.test/oauth-apps"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer k1"))
            .andRespond(
                withSuccess(
                    "{\"googleDrive\":{\"clientId\":\"id\",\"clientSecret\":\"s\"}}", MediaType.APPLICATION_JSON));

        RestOAuthAppProxyClient client = new RestOAuthAppProxyClient(restClientBuilder.build());

        Map<String, OAuthApp> oAuthApps = client.getOAuthApps();

        assertThat(oAuthApps).containsExactly(Map.entry("googleDrive", new OAuthApp("id", "s")));

        server.verify();
    }

    @Test
    void testGetOAuthAppsThrowsOnUnauthorized() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://proxy.test");

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("http://proxy.test/oauth-apps"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        RestOAuthAppProxyClient client = new RestOAuthAppProxyClient(restClientBuilder.build());

        assertThatThrownBy(client::getOAuthApps).isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void testGetOAuthAppsThrowsOnResponseWithoutBody() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://proxy.test");

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("http://proxy.test/oauth-apps"))
            .andRespond(withSuccess());

        RestOAuthAppProxyClient client = new RestOAuthAppProxyClient(restClientBuilder.build());

        assertThatThrownBy(client::getOAuthApps)
            .isInstanceOfSatisfying(
                RestClientResponseException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.OK));
    }

    @Test
    void testGetOAuthAppsThrowsOnRedirect() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://proxy.test");

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("http://proxy.test/oauth-apps"))
            .andRespond(withStatus(HttpStatus.FOUND));

        RestOAuthAppProxyClient client = new RestOAuthAppProxyClient(restClientBuilder.build());

        assertThatThrownBy(client::getOAuthApps)
            .isInstanceOfSatisfying(
                RestClientResponseException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FOUND));
    }

    @Test
    void testGetOAuthAppsReturnsEmptyMapOnEmptyObject() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://proxy.test");

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("http://proxy.test/oauth-apps"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RestOAuthAppProxyClient client = new RestOAuthAppProxyClient(restClientBuilder.build());

        assertThat(client.getOAuthApps()).isEmpty();
    }

    @Test
    void testGetOAuthAppsIgnoresUnknownFields() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://proxy.test");

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("http://proxy.test/oauth-apps"))
            .andRespond(
                withSuccess(
                    "{\"googleDrive\":{\"clientId\":\"id\",\"clientSecret\":\"s\",\"scopes\":[\"drive\"]}}",
                    MediaType.APPLICATION_JSON));

        RestOAuthAppProxyClient client = new RestOAuthAppProxyClient(restClientBuilder.build());

        assertThat(client.getOAuthApps()).containsExactly(Map.entry("googleDrive", new OAuthApp("id", "s")));
    }

    @Test
    void testToStringMasksSecret() {
        OAuthApp oAuthApp = new OAuthApp("id", "topSecret");

        String text = oAuthApp.toString();

        assertThat(text).contains("id");
        assertThat(text).doesNotContain("topSecret");
    }
}
