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
import com.bytechef.config.ApplicationProperties;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Spring AI tool for Firecrawl integration.
 *
 * @author Marko Krišković
 */
@Component
@ConditionalOnAiEnabled
@ConditionalOnProperty(name = "bytechef.ai.firecrawl.enabled")
public class FirecrawlTools {

    private static final Logger logger = LoggerFactory.getLogger(FirecrawlTools.class);

    private final RestClient restClient;

    public FirecrawlTools(ApplicationProperties applicationProperties, RestClient.Builder restClientBuilder) {
        ApplicationProperties.Ai.Firecrawl firecrawl = applicationProperties.getAi()
            .getFirecrawl();

        this.restClient = restClientBuilder
            .baseUrl("https://api.firecrawl.dev/v2")
            .defaultHeader("Authorization", "Bearer " + firecrawl.getApiKey())
            .build();
    }

    @Tool(
        description = "Search the web. Returns web search results with optional scraping of result pages. Use this to find information across the web.")
    public FirecrawlSearchResult webSearch(
        @ToolParam(description = "The search query (max 400 characters, 50 words)") String query,
        @ToolParam(required = false, description = "Number of results to return (1-100, default 5)") Integer limit,
        @ToolParam(
            required = false,
            description = "ISO country code for geo-targeting (e.g., 'US', 'DE', 'JP')") String country) {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Performing Firecrawl search for query: {}", query);
            }

            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("query", query);

            if (limit != null) {
                requestBody.put("limit", Math.min(Math.max(limit, 1), 100));
            }

            if (country != null) {
                requestBody.put("country", country);
            }

            FirecrawlSearchResponse response = restClient.post()
                .uri("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(FirecrawlSearchResponse.class);

            if (response == null) {
                return new FirecrawlSearchResult(query, List.of());
            }

            SearchData data = response.data();

            if (data == null || data.web() == null) {
                return new FirecrawlSearchResult(query, List.of());
            }

            List<SearchResultItem> results = response.data()
                .web()
                .stream()
                .map(result -> new SearchResultItem(
                    result.title() != null ? result.title() : "",
                    result.url() != null ? result.url() : "",
                    result.description() != null ? result.description() : ""))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} search results for query: {}", results.size(), query);
            }

            return new FirecrawlSearchResult(query, results);

        } catch (Exception e) {
            logger.error("Failed to perform Firecrawl search for query: {}", query, e);

            throw new RuntimeException("Failed to perform Firecrawl search: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Scrape a single URL and extract its content. Returns the page content in markdown format. Use this to extract detailed information from a specific webpage.")
    public FirecrawlScrapeResult webpageScrape(
        @ToolParam(description = "The URL to scrape") String url,
        @ToolParam(
            required = false,
            description = "Include only main content (excludes headers, navs, footers). Default: true") Boolean onlyMainContent) {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Scraping URL with Firecrawl: {}", url);
            }

            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("url", url);
            requestBody.put("formats", List.of("markdown"));

            if (onlyMainContent != null) {
                requestBody.put("onlyMainContent", onlyMainContent);
            }

            FirecrawlScrapeResponse response = restClient.post()
                .uri("/scrape")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(FirecrawlScrapeResponse.class);

            if (response == null || response.data() == null) {
                return new FirecrawlScrapeResult(url, "", null, null);
            }

            ScrapeData data = response.data();
            String markdown = data.markdown() != null ? data.markdown() : "";

            ScrapeMetadata metadata = data.metadata();

            String title = metadata != null && metadata.title() != null ? metadata.title() : null;

            String description = metadata != null && metadata.description() != null ? metadata.description() : null;

            if (logger.isDebugEnabled()) {
                logger.debug("Successfully scraped URL: {}, content length: {}", url, markdown.length());
            }

            return new FirecrawlScrapeResult(url, markdown, title, description);
        } catch (Exception e) {
            logger.error("Failed to scrape URL with Firecrawl: {}", url, e);

            throw new RuntimeException("Failed to scrape URL: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Map and discover all URLs from a website. Returns a list of URLs found on the site with optional filtering and search. Use this to explore website structure or find specific pages.")
    public FirecrawlMapResult websiteMap(
        @ToolParam(description = "The base URL to start mapping from") String url,
        @ToolParam(
            required = false,
            description = "Search query to order results by relevance (e.g., 'blog' finds URLs with 'blog')") String search,
        @ToolParam(
            required = false,
            description = "Number of links to return (1-100000, default 5000)") Integer limit,
        @ToolParam(
            required = false,
            description = "Include subdomains of the website (default: true)") Boolean includeSubdomains,
        @ToolParam(
            required = false,
            description = "Exclude URLs with query parameters (default: true)") Boolean ignoreQueryParameters) {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Mapping website URLs for: {}", url);
            }

            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("url", url);

            if (search != null) {
                requestBody.put("search", search);
            }

            if (limit != null) {
                requestBody.put("limit", Math.min(Math.max(limit, 1), 100000));
            }

            if (includeSubdomains != null) {
                requestBody.put("includeSubdomains", includeSubdomains);
            }

            if (ignoreQueryParameters != null) {
                requestBody.put("ignoreQueryParameters", ignoreQueryParameters);
            }

            FirecrawlMapResponse response = restClient.post()
                .uri("/map")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(FirecrawlMapResponse.class);

            if (response == null || response.links() == null) {
                return new FirecrawlMapResult(url, List.of());
            }

            List<MapResultItem> results = response.links()
                .stream()
                .map(link -> new MapResultItem(
                    link.url() != null ? link.url() : "",
                    link.title() != null ? link.title() : "",
                    link.description() != null ? link.description() : ""))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} URLs for website: {}", results.size(), url);
            }

            return new FirecrawlMapResult(url, results);

        } catch (Exception e) {
            logger.error("Failed to map website URLs for: {}", url, e);

            throw new RuntimeException("Failed to map website: " + e.getMessage(), e);
        }
    }

    @SuppressFBWarnings("EI")
    public record FirecrawlSearchResult(
        @JsonProperty("query") @JsonPropertyDescription("The search query that was performed") String query,
        @JsonProperty("results") @JsonPropertyDescription("List of search results with title, URL, and description") List<SearchResultItem> results) {
    }

    @SuppressFBWarnings("EI")
    public record SearchResultItem(
        @JsonProperty("title") @JsonPropertyDescription("Title of the search result") String title,
        @JsonProperty("url") @JsonPropertyDescription("URL of the search result") String url,
        @JsonProperty("description") @JsonPropertyDescription("Description of the search result") String description) {
    }

    @SuppressFBWarnings("EI")
    public record FirecrawlScrapeResult(
        @JsonProperty("url") @JsonPropertyDescription("The URL that was scraped") String url,
        @JsonProperty("markdown") @JsonPropertyDescription("The page content in markdown format") String markdown,
        @JsonProperty("title") @JsonPropertyDescription("The page title") String title,
        @JsonProperty("description") @JsonPropertyDescription("The page description") String description) {
    }

    @SuppressFBWarnings("EI")
    public record FirecrawlMapResult(
        @JsonProperty("url") @JsonPropertyDescription("The base URL that was mapped") String url,
        @JsonProperty("links") @JsonPropertyDescription("List of discovered URLs with title and description") List<MapResultItem> links) {
    }

    @SuppressFBWarnings("EI")
    public record MapResultItem(
        @JsonProperty("url") @JsonPropertyDescription("URL of the discovered page") String url,
        @JsonProperty("title") @JsonPropertyDescription("Title of the page") String title,
        @JsonProperty("description") @JsonPropertyDescription("Description of the page") String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FirecrawlSearchResponse(boolean success, SearchData data, String warning, String id,
        Integer creditsUsed) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchData(List<WebResult> web, List<Object> images, List<Object> news) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResult(String title, String url, String description, String markdown) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FirecrawlScrapeResponse(boolean success, ScrapeData data, String warning) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScrapeData(String markdown, String summary, String html, List<String> links,
        ScrapeMetadata metadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScrapeMetadata(String title, String description, String language, String sourceURL, String keywords,
        Integer statusCode, String error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FirecrawlMapResponse(boolean success, List<MapLink> links) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapLink(String url, String title, String description) {
    }
}
