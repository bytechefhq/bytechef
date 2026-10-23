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

import com.bytechef.platform.ai.provider.dto.WebSearchProviderConnection;
import com.bytechef.platform.ai.provider.facade.WebSearchProviderConnectionTester;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Tests a web search provider with one search for a single result, through the same tools agents use.
 *
 * @author Ivica Cardic
 */
@Component
class WebSearchProviderConnectionTesterImpl implements WebSearchProviderConnectionTester {

    static final String TEST_QUERY = "ByteChef";

    private final RestClient.Builder restClientBuilder;

    @SuppressFBWarnings("EI")
    WebSearchProviderConnectionTesterImpl(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public void test(WebSearchProvider webSearchProvider, WebSearchProviderConnection webSearchProviderConnection) {
        String apiKey = webSearchProviderConnection.apiKey();
        String baseUrl = webSearchProviderConnection.baseUrl();

        Runnable testSearch = switch (webSearchProvider) {
            case BRAVE -> () -> {
                BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
                    baseUrl, apiKey, restClientBuilder.clone());

                braveWebSearchTools.braveWebSearch(TEST_QUERY, 1, null, null);
            };
            case FIRECRAWL -> () -> {
                FirecrawlTools firecrawlTools = new FirecrawlTools(baseUrl, apiKey, restClientBuilder.clone());

                firecrawlTools.webSearch(TEST_QUERY, 1, null);
            };
        };

        testSearch.run();
    }
}
