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
            String apiKey = fetchEnabledApiKey(webSearchProvider, environment);

            if (apiKey != null) {
                tools.add(createTools(webSearchProvider, apiKey));
            }
        }

        if (tools.isEmpty()) {
            return List.of();
        }

        return List.of(ToolCallbacks.from(tools.toArray()));
    }

    private Object createTools(WebSearchProvider webSearchProvider, String apiKey) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        return switch (webSearchProvider) {
            case BRAVE -> new BraveWebSearchTools(
                ai.getBrave()
                    .getBaseUrl(),
                apiKey, restClientBuilder.clone());
            case FIRECRAWL -> new FirecrawlTools(
                ai.getFirecrawl()
                    .getBaseUrl(),
                apiKey, restClientBuilder.clone());
        };
    }

    private @Nullable String fetchEnabledApiKey(WebSearchProvider webSearchProvider, int environment) {
        WebSearchProviderFacade webSearchProviderFacade = webSearchProviderFacadeProvider.getIfAvailable();

        if (webSearchProviderFacade == null) {
            return fetchPropertiesApiKey(webSearchProvider);
        }

        try {
            return webSearchProviderFacade.fetchEnabledApiKey(webSearchProvider, environment);
        } catch (RuntimeException exception) {
            log.warn(
                "Could not resolve the {} API key for environment {}; its web search tools stay off for this run",
                webSearchProvider.getLabel(), environment, exception);

            return null;
        }
    }

    private @Nullable String fetchPropertiesApiKey(WebSearchProvider webSearchProvider) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        boolean enabled;
        String apiKey;

        switch (webSearchProvider) {
            case BRAVE -> {
                enabled = ai.getBrave()
                    .isEnabled();
                apiKey = ai.getBrave()
                    .getApiKey();
            }
            case FIRECRAWL -> {
                enabled = ai.getFirecrawl()
                    .isEnabled();
                apiKey = ai.getFirecrawl()
                    .getApiKey();
            }
            default -> throw new IllegalStateException("Unexpected web search provider: " + webSearchProvider);
        }

        return enabled && apiKey != null && !apiKey.isBlank() ? apiKey : null;
    }
}
