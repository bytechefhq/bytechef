/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * Service for scraping web content from URLs.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WebScrapeService {

    /**
     * Scrapes content from a single URL.
     *
     * @param url the URL to scrape
     * @return the scraped content as markdown or plain text
     */
    ScrapeResult scrape(String url);

    /**
     * Crawls multiple pages starting from the given URL.
     *
     * @param url             the starting URL
     * @param maxPages        maximum number of pages to crawl
     * @param includePatterns URL patterns to include in the crawl
     * @return the combined content from all crawled pages
     */
    CrawlResult crawl(String url, int maxPages, List<String> includePatterns);

    /**
     * Returns the name of the provider (e.g., "firecrawl" or "jsoup").
     *
     * @return the provider name
     */
    String getProviderName();

    record ScrapeResult(boolean success, String content, String error) {

        public static ScrapeResult success(String content) {
            return new ScrapeResult(true, content, null);
        }

        public static ScrapeResult failure(String error) {
            return new ScrapeResult(false, null, error);
        }
    }

    @SuppressFBWarnings("EI")
    record CrawlResult(boolean success, String content, List<String> crawledUrls, String error) {

        public static CrawlResult success(String content, List<String> crawledUrls) {
            return new CrawlResult(true, content, crawledUrls, null);
        }

        public static CrawlResult failure(String error) {
            return new CrawlResult(false, null, List.of(), error);
        }
    }
}
