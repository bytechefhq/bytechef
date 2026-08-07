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
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.tool.exception.BraveToolErrorType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Spring AI tool for Brave Search integration.
 *
 * <p>
 * The tool method is deliberately named {@code braveWebSearch} rather than {@code webSearch}: Spring AI derives tool
 * names from method names, and this bean is registered on the same agents as {@link FirecrawlTools}, whose search
 * method is already called {@code webSearch}.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(name = "bytechef.ai.brave.enabled")
public class BraveWebSearchTools {

    private static final int MAX_COUNT = 20;
    private static final int MIN_COUNT = 1;

    private static final Logger log = LoggerFactory.getLogger(BraveWebSearchTools.class);

    private final RestClient restClient;

    public BraveWebSearchTools(ApplicationProperties applicationProperties, RestClient.Builder restClientBuilder) {
        ApplicationProperties.Ai.Brave brave = applicationProperties.getAi()
            .getBrave();

        this.restClient = restClientBuilder
            .baseUrl(brave.getBaseUrl())
            .defaultHeader("X-Subscription-Token", brave.getApiKey())
            .defaultHeader("Accept", "application/json")
            .build();
    }

    @Tool(
        description = "Search the web with the Brave Search API. Returns ranked results with title, URL, description and age. Use this for fast, broad web search; it returns snippets only and does not fetch full page content.")
    public BraveSearchResult braveWebSearch(
        @ToolParam(description = "The search query") String query,
        @ToolParam(required = false, description = "Number of results to return (1-20, default 20)") Integer count,
        @ToolParam(
            required = false,
            description = "ISO country code for geo-targeting (e.g., 'US', 'DE', 'JP')") String country,
        @ToolParam(
            required = false,
            description = "Freshness filter: 'pd' past day, 'pw' past week, 'pm' past month, 'py' past year, or a 'YYYY-MM-DDtoYYYY-MM-DD' range") String freshness) {

        try {
            if (log.isDebugEnabled()) {
                log.debug(
                    "braveWebSearch({}, {}, {}, {}): Performing Brave search for query: {}", query, count, country,
                    freshness, query);
            }

            BraveSearchResponse response = restClient.get()
                .uri(uriBuilder -> {
                    Map<String, Object> uriVariables = new HashMap<>();

                    uriVariables.put("query", query);

                    uriBuilder.path("/web/search")
                        .queryParam("q", "{query}");

                    if (count != null) {
                        uriBuilder.queryParam("count", Math.min(Math.max(count, MIN_COUNT), MAX_COUNT));
                    }

                    if (country != null) {
                        uriBuilder.queryParam("country", "{country}");
                        uriVariables.put("country", country);
                    }

                    if (freshness != null) {
                        uriBuilder.queryParam("freshness", "{freshness}");
                        uriVariables.put("freshness", freshness);
                    }

                    return uriBuilder.build(uriVariables);
                })
                .retrieve()
                .body(BraveSearchResponse.class);

            if (response == null || response.web() == null || response.web()
                .results() == null) {
                return new BraveSearchResult(query, List.of());
            }

            List<BraveSearchResultItem> results = response.web()
                .results()
                .stream()
                .map(webResult -> new BraveSearchResultItem(
                    webResult.title() != null ? webResult.title() : "",
                    webResult.url() != null ? webResult.url() : "",
                    webResult.description() != null ? webResult.description() : "",
                    webResult.age() != null ? webResult.age() : ""))
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "braveWebSearch({}, {}, {}, {}): Found {} search results for query: {}", query, count, country,
                    freshness, results.size(), query);
            }

            return new BraveSearchResult(query, results);
        } catch (Exception e) {
            log.error(
                "braveWebSearch({}, {}, {}, {}): Failed to perform Brave search for query: {}", query, count, country,
                freshness, query, e);

            throw new ExecutionException(
                "Failed to perform Brave search: " + e.getMessage(), e, BraveToolErrorType.WEB_SEARCH);
        }
    }

    @SuppressFBWarnings("EI")
    public record BraveSearchResult(
        @JsonProperty("query") @JsonPropertyDescription("The search query that was performed") String query,
        @JsonProperty("results") @JsonPropertyDescription("List of search results with title, URL, description and age") List<BraveSearchResultItem> results) {
    }

    @SuppressFBWarnings("EI")
    public record BraveSearchResultItem(
        @JsonProperty("title") @JsonPropertyDescription("Title of the search result") String title,
        @JsonProperty("url") @JsonPropertyDescription("URL of the search result") String url,
        @JsonProperty("description") @JsonPropertyDescription("Description of the search result") String description,
        @JsonProperty("age") @JsonPropertyDescription("Human readable age of the page, e.g. '3 days ago'") String age) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveSearchResponse(WebResults web) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResults(List<WebResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResult(String title, String url, String description, String age) {
    }
}
