/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.CrawlResult;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.ScrapeResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.function.Function;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

/**
 * Spring AI tool for web scraping. Allows LLM to scrape web pages for documentation analysis.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Description("Scrape content from web pages. Use 'scrape' mode for single pages or 'crawl' mode for multiple linked pages.")
public class WebScrapeTool implements Function<WebScrapeTool.ScrapeRequest, WebScrapeTool.ScrapeResponse> {

    private final WebScrapeService webScrapeService;

    public WebScrapeTool(WebScrapeService webScrapeService) {
        this.webScrapeService = webScrapeService;
    }

    @Override
    public ScrapeResponse apply(ScrapeRequest request) {
        if (request.mode() == ScrapeMode.CRAWL) {
            CrawlResult result = webScrapeService.crawl(
                request.url(),
                request.maxPages() != null ? request.maxPages() : 10,
                request.includePatterns() != null ? request.includePatterns() : List.of());

            return new ScrapeResponse(
                result.success(),
                result.content(),
                result.crawledUrls(),
                webScrapeService.getProviderName(),
                result.error());
        } else {
            ScrapeResult result = webScrapeService.scrape(request.url());

            return new ScrapeResponse(
                result.success(),
                result.content(),
                List.of(request.url()),
                webScrapeService.getProviderName(),
                result.error());
        }
    }

    public enum ScrapeMode {

        SCRAPE, CRAWL
    }

    @SuppressFBWarnings("EI")
    public record ScrapeRequest(
        String url,
        ScrapeMode mode,
        Integer maxPages,
        List<String> includePatterns) {

        public ScrapeRequest(String url) {
            this(url, ScrapeMode.SCRAPE, null, null);
        }
    }

    @SuppressFBWarnings("EI")
    public record ScrapeResponse(
        boolean success,
        String content,
        List<String> crawledUrls,
        String provider,
        String error) {
    }
}
