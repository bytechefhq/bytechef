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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
class RestMcpServerCatalogProxyClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private RestMcpServerCatalogProxyClient client;

    @BeforeEach
    void beforeEach() {
        client = new RestMcpServerCatalogProxyClient(
            RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build());
    }

    @Test
    void testGetMcpServerCatalogParsesEntriesAndIgnoresUnknownFields() {
        wireMock.stubFor(get(urlEqualTo("/mcp-servers")).willReturn(
            aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"servers":[{"slug":"notion","title":"Notion","url":"https://mcp.notion.com/mcp",\
                    "transportType":"httpStreamable","authentication":{"type":"oauth2",\
                    "dynamicClientRegistration":true,"futureAuthField":1},"component":"notion","futureField":"x"}]}
                    """)));

        List<McpServerCatalogEntry> entries = client.getMcpServerCatalog();

        assertThat(entries).hasSize(1);

        McpServerCatalogEntry entry = entries.getFirst();

        assertThat(entry.slug()).isEqualTo("notion");
        assertThat(entry.transportType()).isEqualTo("httpStreamable");
        assertThat(entry.description()).isNull();

        McpServerCatalogAuthentication authentication = entry.authentication();

        assertThat(authentication.type()).isEqualTo("oauth2");
        assertThat(authentication.dynamicClientRegistration()).isTrue();
    }

    @Test
    void testGetMcpServerCatalogFailsOnServerError() {
        wireMock.stubFor(get(urlEqualTo("/mcp-servers")).willReturn(aResponse().withStatus(502)));

        assertThatThrownBy(client::getMcpServerCatalog).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void testGetMcpServerCatalogRejectsBodyWithoutServers() {
        wireMock.stubFor(get(urlEqualTo("/mcp-servers")).willReturn(
            aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")));

        assertThatThrownBy(client::getMcpServerCatalog).isInstanceOf(IllegalStateException.class);
    }
}
