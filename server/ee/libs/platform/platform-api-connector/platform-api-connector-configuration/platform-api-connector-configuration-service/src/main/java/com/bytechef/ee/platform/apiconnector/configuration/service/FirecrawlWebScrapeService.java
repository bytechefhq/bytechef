/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Firecrawl-based implementation of WebScrapeService. Primary scraping service when Firecrawl is enabled.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service("firecrawlWebScrapeService")
@ConditionalOnEEVersion
@ConditionalOnProperty(name = "bytechef.ai.firecrawl.enabled", havingValue = "true")
public class FirecrawlWebScrapeService implements WebScrapeService {

    private static final Logger logger = LoggerFactory.getLogger(FirecrawlWebScrapeService.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int POLL_INTERVAL_MS = 2000;
    private static final int MAX_POLL_ATTEMPTS = 60;

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public FirecrawlWebScrapeService(ApplicationProperties applicationProperties, ObjectMapper objectMapper) {
        ApplicationProperties.Ai.Firecrawl firecrawl = applicationProperties.getAi()
            .getFirecrawl();

        this.apiKey = firecrawl.getApiKey();
        this.baseUrl = firecrawl.getBaseUrl();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public ScrapeResult scrape(String url) {
        try {
            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("url", url);
            requestBody.put("formats", List.of("markdown"));

            String responseBody = sendRequest("/scrape", requestBody);

            JsonNode response = objectMapper.readTree(responseBody);

            if (response.has("success") && response.get("success")
                .asBoolean()) {

                String content = response.path("data")
                    .path("markdown")
                    .asText("");

                return ScrapeResult.success(content);
            } else {
                String error = response.path("error")
                    .asText("Unknown error");

                return ScrapeResult.failure(error);
            }
        } catch (Exception exception) {
            logger.error("Failed to scrape URL via Firecrawl: {}", url, exception);

            return ScrapeResult.failure(exception.getMessage());
        }
    }

    @Override
    public CrawlResult crawl(String url, int maxPages, List<String> includePatterns) {
        try {
            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("url", url);
            requestBody.put("limit", maxPages > 0 ? maxPages : 10);

            if (includePatterns != null && !includePatterns.isEmpty()) {
                requestBody.put("includePaths", includePatterns);
            }

            String responseBody = sendRequest("/crawl", requestBody);

            JsonNode response = objectMapper.readTree(responseBody);

            if (!response.has("id")) {
                String error = response.path("error")
                    .asText("Failed to start crawl");

                return CrawlResult.failure(error);
            }

            String crawlId = response.get("id")
                .asText();

            return pollCrawlStatus(crawlId);
        } catch (Exception exception) {
            logger.error("Failed to crawl URL via Firecrawl: {}", url, exception);

            return CrawlResult.failure(exception.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "firecrawl";
    }

    /**
     * Polls the Firecrawl API for crawl status until completion or timeout.
     *
     * <p>
     * <strong>Note:</strong> This method uses Thread.sleep() for polling intervals and may block for up to 2 minutes
     * (MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS). It should only be called from async/background threads, not from web
     * request threads. The calling code in {@link ApiConnectorAiService} runs this in an async context.
     */
    private CrawlResult pollCrawlStatus(String crawlId) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/crawl/" + crawlId))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode statusResponse = objectMapper.readTree(response.body());

            String status = statusResponse.path("status")
                .asText();

            if ("completed".equals(status)) {
                List<String> crawledUrls = new ArrayList<>();
                StringBuilder combinedContent = new StringBuilder();

                JsonNode dataArray = statusResponse.path("data");

                if (dataArray.isArray()) {
                    for (JsonNode item : dataArray) {
                        String pageUrl = item.path("metadata")
                            .path("sourceURL")
                            .asText("");
                        String markdown = item.path("markdown")
                            .asText("");

                        if (!pageUrl.isEmpty()) {
                            crawledUrls.add(pageUrl);
                        }

                        if (!markdown.isEmpty()) {
                            combinedContent.append("\n\n--- Page: ")
                                .append(pageUrl)
                                .append(" ---\n\n")
                                .append(markdown);
                        }
                    }
                }

                return CrawlResult.success(combinedContent.toString(), crawledUrls);
            } else if ("failed".equals(status)) {
                String error = statusResponse.path("error")
                    .asText("Crawl failed");

                return CrawlResult.failure(error);
            }

            Thread.sleep(POLL_INTERVAL_MS);
        }

        return CrawlResult.failure("Crawl timed out");
    }

    private String sendRequest(String endpoint, Map<String, Object> requestBody)
        throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + endpoint))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("Firecrawl API error: HTTP " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }
}
