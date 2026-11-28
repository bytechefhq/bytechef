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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderConnection;
import com.bytechef.platform.ai.provider.facade.WebSearchProviderFacade;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
class WebSearchToolCallbackProviderTest {

    private static final int ENVIRONMENT = 1;

    private final ApplicationProperties applicationProperties = new ApplicationProperties();
    private final WebSearchProviderFacade webSearchProviderFacade = mock(WebSearchProviderFacade.class);

    @Test
    void testOffersOnlyTheToolsOfEnabledProviders() {
        when(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, ENVIRONMENT))
            .thenReturn(new WebSearchProviderConnection("brave-key", "https://example.com"));

        assertThat(toolNames(provider(webSearchProviderFacade).getToolCallbacks(ENVIRONMENT)))
            .containsExactly("braveWebSearch");
    }

    @Test
    void testOffersTheToolsOfEveryEnabledProvider() {
        when(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, ENVIRONMENT))
            .thenReturn(new WebSearchProviderConnection("brave-key", "https://example.com"));
        when(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.FIRECRAWL, ENVIRONMENT))
            .thenReturn(new WebSearchProviderConnection("firecrawl-key", "https://example.com"));

        assertThat(toolNames(provider(webSearchProviderFacade).getToolCallbacks(ENVIRONMENT)))
            .containsExactlyInAnyOrder("braveWebSearch", "webSearch", "webpageScrape", "websiteMap");
    }

    @Test
    void testOffersNoToolsWhenNoProviderIsEnabled() {
        assertThat(provider(webSearchProviderFacade).getToolCallbacks(ENVIRONMENT)).isEmpty();
    }

    @Test
    void testSkipsAProviderWhoseKeyLookupFails() {
        when(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, ENVIRONMENT))
            .thenThrow(new IllegalStateException("database unavailable"));
        when(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.FIRECRAWL, ENVIRONMENT))
            .thenReturn(new WebSearchProviderConnection("firecrawl-key", "https://example.com"));

        assertThat(toolNames(provider(webSearchProviderFacade).getToolCallbacks(ENVIRONMENT)))
            .containsExactlyInAnyOrder("webSearch", "webpageScrape", "websiteMap");
    }

    @Test
    void testUsesTheApplicationPropertiesWhenTheFacadeIsNotDeployed() {
        ApplicationProperties.Ai.Brave brave = applicationProperties.getAi()
            .getBrave();

        brave.setApiKey("brave-key");
        brave.setEnabled(true);

        ApplicationProperties.Ai.Firecrawl firecrawl = applicationProperties.getAi()
            .getFirecrawl();

        firecrawl.setApiKey("firecrawl-key");

        assertThat(toolNames(provider(null).getToolCallbacks(ENVIRONMENT)))
            .containsExactly("braveWebSearch");
    }

    @SuppressWarnings("unchecked")
    private WebSearchToolCallbackProvider provider(WebSearchProviderFacade webSearchProviderFacade) {
        ObjectProvider<WebSearchProviderFacade> webSearchProviderFacadeProvider = mock(ObjectProvider.class);

        when(webSearchProviderFacadeProvider.getIfAvailable()).thenReturn(webSearchProviderFacade);

        return new WebSearchToolCallbackProvider(
            applicationProperties, RestClient.builder(), webSearchProviderFacadeProvider);
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
