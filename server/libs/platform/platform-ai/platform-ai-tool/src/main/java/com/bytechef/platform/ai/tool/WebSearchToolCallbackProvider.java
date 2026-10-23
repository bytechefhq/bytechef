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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderConnection;
import com.bytechef.platform.ai.provider.facade.WebSearchProviderFacade;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Builds the web search tools of the providers enabled in an environment, so an agent run is offered only the web
 * search tools that can work.
 *
 * @author Ivica Cardic
 */
@Component
public class WebSearchToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(WebSearchToolCallbackProvider.class);

    private final ApplicationProperties applicationProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectProvider<WebSearchProviderFacade> webSearchProviderFacadeProvider;

    @SuppressFBWarnings("EI")
    public WebSearchToolCallbackProvider(
        ApplicationProperties applicationProperties, RestClient.Builder restClientBuilder,
        ObjectProvider<WebSearchProviderFacade> webSearchProviderFacadeProvider) {

        this.applicationProperties = applicationProperties;
        this.restClientBuilder = restClientBuilder;
        this.webSearchProviderFacadeProvider = webSearchProviderFacadeProvider;
    }

    public List<ToolCallback> getToolCallbacks(int environment) {
        List<Object> tools = new ArrayList<>();

        for (WebSearchProvider webSearchProvider : WebSearchProvider.values()) {
            WebSearchProviderConnection webSearchProviderConnection = fetchEnabledConnection(
                webSearchProvider, environment);

            if (webSearchProviderConnection != null) {
                tools.add(createTools(webSearchProvider, webSearchProviderConnection));
            }
        }

        if (tools.isEmpty()) {
            return List.of();
        }

        return List.of(ToolCallbacks.from(tools.toArray()));
    }

    Object createTools(WebSearchProvider webSearchProvider, WebSearchProviderConnection webSearchProviderConnection) {
        String apiKey = webSearchProviderConnection.apiKey();
        String baseUrl = webSearchProviderConnection.baseUrl();

        return switch (webSearchProvider) {
            case BRAVE -> new BraveWebSearchTools(baseUrl, apiKey, restClientBuilder.clone());
            case FIRECRAWL -> new FirecrawlTools(baseUrl, apiKey, restClientBuilder.clone());
        };
    }

    private @Nullable WebSearchProviderConnection fetchEnabledConnection(
        WebSearchProvider webSearchProvider, int environment) {

        WebSearchProviderFacade webSearchProviderFacade = webSearchProviderFacadeProvider.getIfAvailable();

        if (webSearchProviderFacade == null) {
            return fetchPropertiesConnection(webSearchProvider);
        }

        try {
            return webSearchProviderFacade.fetchEnabledConnection(webSearchProvider, environment);
        } catch (RuntimeException exception) {
            log.warn(
                "Could not resolve the {} API key for environment {}; its web search tools stay off for this run",
                webSearchProvider.getLabel(), environment, exception);

            return null;
        }
    }

    private @Nullable WebSearchProviderConnection fetchPropertiesConnection(WebSearchProvider webSearchProvider) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        return switch (webSearchProvider) {
            case BRAVE -> {
                ApplicationProperties.Ai.Brave brave = ai.getBrave();

                yield toConnection(brave.isEnabled(), brave.getApiKey(), brave.getBaseUrl());
            }
            case FIRECRAWL -> {
                ApplicationProperties.Ai.Firecrawl firecrawl = ai.getFirecrawl();

                yield toConnection(firecrawl.isEnabled(), firecrawl.getApiKey(), firecrawl.getBaseUrl());
            }
        };
    }

    private static @Nullable WebSearchProviderConnection toConnection(
        boolean enabled, @Nullable String apiKey, String baseUrl) {

        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return null;
        }

        return new WebSearchProviderConnection(apiKey, baseUrl);
    }
}
