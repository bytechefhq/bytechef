/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Jsoup-based implementation of WebScrapeService. Used as fallback when Firecrawl is not configured.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnMissingBean(name = "firecrawlWebScrapeService")
public class JsoupWebScrapeService implements WebScrapeService {

    private static final Logger logger = LoggerFactory.getLogger(JsoupWebScrapeService.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_CRAWL_PAGES = 20;

    private final HttpClient httpClient;

    public JsoupWebScrapeService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    public ScrapeResult scrape(String url) {
        try {
            String content = fetchAndParse(url);

            return ScrapeResult.success(content);
        } catch (Exception exception) {
            logger.error("Failed to scrape URL: {}", url, exception);

            return ScrapeResult.failure(exception.getMessage());
        }
    }

    @Override
    public CrawlResult crawl(String url, int maxPages, List<String> includePatterns) {
        int effectiveMaxPages = maxPages > 0 ? Math.min(maxPages, DEFAULT_MAX_CRAWL_PAGES) : DEFAULT_MAX_CRAWL_PAGES;

        try {
            URI baseUri = URI.create(url);
            String baseHost = baseUri.getHost();

            List<Pattern> patterns = includePatterns.stream()
                .map(pattern -> Pattern.compile(pattern.replace("*", ".*")))
                .toList();

            Set<String> visited = new HashSet<>();
            List<String> crawledUrls = new ArrayList<>();
            StringBuilder combinedContent = new StringBuilder();

            Queue<String> queue = new LinkedList<>();

            queue.add(url);

            while (!queue.isEmpty() && crawledUrls.size() < effectiveMaxPages) {
                String currentUrl = queue.poll();

                if (visited.contains(currentUrl)) {
                    continue;
                }

                visited.add(currentUrl);

                try {
                    String content = fetchAndParseWithLinks(currentUrl, baseHost, patterns, queue, visited);

                    if (content != null && !content.isBlank()) {
                        crawledUrls.add(currentUrl);
                        combinedContent.append("\n\n--- Page: ")
                            .append(currentUrl)
                            .append(" ---\n\n")
                            .append(content);
                    }
                } catch (Exception exception) {
                    logger.warn("Failed to crawl URL: {}", currentUrl, exception);
                }
            }

            return CrawlResult.success(combinedContent.toString(), crawledUrls);
        } catch (Exception exception) {
            logger.error("Failed to crawl starting from URL: {}", url, exception);

            return CrawlResult.failure(exception.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "jsoup";
    }

    private String fetchAndParse(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header("User-Agent", "ByteChef Web Scraper")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }

        Document document = Jsoup.parse(response.body());

        document.select("script, style, nav, footer, header, aside, noscript, iframe")
            .remove();

        return document.body()
            .text();
    }

    private String fetchAndParseWithLinks(
        String url, String baseHost, List<Pattern> patterns, Queue<String> queue, Set<String> visited)
        throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header("User-Agent", "ByteChef Web Scraper")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }

        Document document = Jsoup.parse(response.body(), url);

        Elements links = document.select("a[href]");

        for (Element link : links) {
            String href = link.absUrl("href");

            if (href.isEmpty() || visited.contains(href)) {
                continue;
            }

            try {
                URI linkUri = URI.create(href);

                if (!baseHost.equals(linkUri.getHost())) {
                    continue;
                }

                boolean matchesPattern = patterns.isEmpty() ||
                    patterns.stream()
                        .anyMatch(pattern -> pattern.matcher(href)
                            .matches());

                if (matchesPattern) {
                    queue.add(href);
                }
            } catch (Exception exception) {
                logger.debug("Invalid URL: {}", href);
            }
        }

        document.select("script, style, nav, footer, header, aside, noscript, iframe")
            .remove();

        return document.body()
            .text();
    }
}
