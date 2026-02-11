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

package com.bytechef.ai.mcp.tool.platform;

import com.bytechef.ai.mcp.tool.config.ConditionalOnAiEnabled;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;import org.springframework.web.client.RestClient;

/**
 * Spring AI tool for Brave Search integration.
 *
 * @author Marko Krišković
 */
@Component
@ConditionalOnAiEnabled
@ConditionalOnProperty(name = "brave.search.api-key")
public class BraveSearchTools {

    private static final Logger logger = LoggerFactory.getLogger(BraveSearchTools.class);

    private static final String BRAVE_SEARCH_API_URL = "https://api.search.brave.com/res/v1/web/search";

    private final RestClient restClient;
    private final String apiKey;

    public BraveSearchTools(
        RestClient.Builder restClientBuilder,
        @Value("${brave.search.api-key}") String apiKey) {

        this.restClient = restClientBuilder
            .baseUrl(BRAVE_SEARCH_API_URL)
            .build();
        this.apiKey = apiKey;
    }

    @Tool(description = "Search the web using Brave Search API. Returns web search results including title, URL, and description for each result.")
    public BraveSearchResult braveSearch(
        @ToolParam(description = "The search query (max 400 characters, 50 words)") String query,
        @ToolParam(required = false, description = "Number of results to return (1-20, default 10)") Integer count,
        @ToolParam(required = false, description = "Offset for pagination (0-9)") Integer offset,
        @ToolParam(required = false, description = "Time filter: 'pd' (24h), 'pw' (7d), 'pm' (31d), 'py' (365d)") String freshness,
        @ToolParam(required = false, description = "Apply search operators") Boolean operators) {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Performing Brave search for query: {}", query);
            }

            Integer resultCount = count != null ? Math.min(Math.max(count, 1), 20) : 10;
            Integer resultOffset = offset != null ? Math.min(Math.max(offset, 0), 9) : null;

            BraveSearchResponse response = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.queryParam("q", query);
                    uriBuilder.queryParam("count", resultCount);

                    if (resultOffset != null) {
                        uriBuilder.queryParam("offset", resultOffset);
                    }
                    if (freshness != null) {
                        uriBuilder.queryParam("freshness", freshness);
                    }
                    uriBuilder.queryParam("result_filter", "discussions,faq,infobox,web");
                    if (operators != null) {
                        uriBuilder.queryParam("operators", operators);
                    }

                    return uriBuilder.build();
                })
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip")
                .header("X-Subscription-Token", apiKey)
                .retrieve()
                .body(BraveSearchResponse.class);

            if (response == null || response.web() == null || response.web().results() == null) {
                return new BraveSearchResult(query, List.of());
            }

            List<SearchResultItem> results = response.web()
                .results()
                .stream()
                .map(result -> new SearchResultItem(
                    result.title() != null ? result.title() : "",
                    result.url() != null ? result.url() : "",
                    result.description() != null ? result.description() : ""))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} search results for query: {}", results.size(), query);
            }

            return new BraveSearchResult(query, results);

        } catch (Exception e) {
            logger.error("Failed to perform Brave search for query: {}", query, e);

            throw new RuntimeException("Failed to perform Brave search: " + e.getMessage(), e);
        }
    }

    @SuppressFBWarnings("EI")
    public record BraveSearchResult(
        @JsonProperty("query") @JsonPropertyDescription("The search query that was performed") String query,
        @JsonProperty("results") @JsonPropertyDescription("List of search results with title, URL, and description") List<SearchResultItem> results) {
    }

    @SuppressFBWarnings("EI")
    public record SearchResultItem(
        @JsonProperty("title") @JsonPropertyDescription("Title of the search result") String title,
        @JsonProperty("url") @JsonPropertyDescription("URL of the search result") String url,
        @JsonProperty("description") @JsonPropertyDescription("Description of the search result") String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveSearchResponse(WebResults web) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResults(List<WebResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResult(String title, String url, String description) {
    }
}
