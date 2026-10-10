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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
class BraveWebSearchToolsTest {

    private static final String SEARCH_URL = "https://api.search.brave.com/res/v1/web/search";

    private static final String RESPONSE_BODY = """
        {
          "type": "search",
          "web": {
            "type": "search",
            "results": [
              {
                "title": "ByteChef",
                "url": "https://bytechef.io",
                "description": "Open-source automation platform.",
                "age": "3 days ago",
                "family_friendly": true
              }
            ]
          }
        }
        """;

    @Test
    void testBraveWebSearchMapsResults() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Subscription-Token", "test-key"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        BraveWebSearchTools.BraveSearchResult result = braveWebSearchTools.braveWebSearch(
            "bytechef", null, null, null);

        assertThat(result.query()).isEqualTo("bytechef");
        assertThat(result.results()).hasSize(1);

        BraveWebSearchTools.BraveSearchResultItem item = result.results()
            .getFirst();

        assertThat(item.title()).isEqualTo("ByteChef");
        assertThat(item.url()).isEqualTo("https://bytechef.io");
        assertThat(item.description()).isEqualTo("Open-source automation platform.");
        assertThat(item.age()).isEqualTo("3 days ago");

        server.verify();
    }

    @Test
    void testBraveWebSearchClampsCountToBraveMaximum() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef&count=20"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        braveWebSearchTools.braveWebSearch("bytechef", 50, null, null);

        server.verify();
    }

    @Test
    void testBraveWebSearchClampsCountToBraveMinimum() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef&count=1"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        braveWebSearchTools.braveWebSearch("bytechef", 0, null, null);

        server.verify();
    }

    @Test
    void testBraveWebSearchEncodesQueryContainingExpressionSyntax() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=how%20to%20use%20%24%7BtaskName.property%7D"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        braveWebSearchTools.braveWebSearch("how to use ${taskName.property}", null, null, null);

        server.verify();
    }

    @Test
    void testBraveWebSearchPassesOptionalParameters() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef&country=DE&freshness=pw"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        braveWebSearchTools.braveWebSearch("bytechef", null, "DE", "pw");

        server.verify();
    }

    @Test
    void testBraveWebSearchReturnsEmptyResultsWhenWebSectionAbsent() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andRespond(withSuccess("{\"type\":\"search\"}", MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        BraveWebSearchTools.BraveSearchResult result = braveWebSearchTools.braveWebSearch(
            "bytechef", null, null, null);

        assertThat(result.results()).isEmpty();
    }

    @Test
    void testBraveWebSearchReturnsEmptyResultsWhenBodyIsEmpty() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andRespond(withSuccess());

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        BraveWebSearchTools.BraveSearchResult result = braveWebSearchTools.braveWebSearch(
            "bytechef", null, null, null);

        assertThat(result.query()).isEqualTo("bytechef");
        assertThat(result.results()).isEmpty();
    }

    @Test
    void testBraveWebSearchWrapsHttpFailure() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andRespond(withServerError());

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        assertThatThrownBy(() -> braveWebSearchTools.braveWebSearch("bytechef", null, null, null))
            .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testToolCallbackIsNamedBraveWebSearch() {
        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), RestClient.builder());

        ToolCallback[] toolCallbacks = ToolCallbacks.from(braveWebSearchTools);

        assertThat(toolCallbacks).hasSize(1);
        assertThat(
            toolCallbacks[0].getToolDefinition()
                .name()).isEqualTo("braveWebSearch");
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Ai.Brave brave = applicationProperties.getAi()
            .getBrave();

        brave.setApiKey("test-key");

        return applicationProperties;
    }
}
