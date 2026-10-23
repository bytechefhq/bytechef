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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderConnection;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
class WebSearchProviderConnectionTesterTest {

    @Test
    void testBraveRunsOneSearchForASingleResultWithTheGivenKeyAndBaseUrl() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("https://brave.proxy/res/v1/web/search?q=ByteChef&count=1"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Subscription-Token", "brave-key"))
            .andRespond(withSuccess("{\"web\":{\"results\":[]}}", MediaType.APPLICATION_JSON));

        new WebSearchProviderConnectionTesterImpl(restClientBuilder).test(
            WebSearchProvider.BRAVE, new WebSearchProviderConnection("brave-key", "https://brave.proxy/res/v1"));

        server.verify();
    }

    @Test
    void testFirecrawlRunsOneSearchAgainstASelfHostedBaseUrl() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("https://firecrawl.internal/v2/search"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer firecrawl-key"))
            .andRespond(withSuccess("{\"success\":true,\"data\":{\"web\":[]}}", MediaType.APPLICATION_JSON));

        new WebSearchProviderConnectionTesterImpl(restClientBuilder).test(
            WebSearchProvider.FIRECRAWL,
            new WebSearchProviderConnection("firecrawl-key", "https://firecrawl.internal/v2"));

        server.verify();
    }

    @Test
    void testARejectedKeyFailsTheTest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo("https://brave.proxy/res/v1/web/search?q=ByteChef&count=1"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        WebSearchProviderConnectionTesterImpl webSearchProviderConnectionTester =
            new WebSearchProviderConnectionTesterImpl(restClientBuilder);

        assertThatThrownBy(() -> webSearchProviderConnectionTester.test(
            WebSearchProvider.BRAVE, new WebSearchProviderConnection("bad-key", "https://brave.proxy/res/v1")))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseMessage("401 Unauthorized: [no body]");
    }
}
