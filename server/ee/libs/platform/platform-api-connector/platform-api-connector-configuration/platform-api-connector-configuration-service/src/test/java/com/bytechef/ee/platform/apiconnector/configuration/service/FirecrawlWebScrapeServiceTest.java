/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.CrawlResult;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.ScrapeResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class FirecrawlWebScrapeServiceTest {

    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private final FirecrawlWebScrapeService firecrawlWebScrapeService = new FirecrawlWebScrapeService(
        new ApplicationProperties(), objectMapper);

    @Test
    void testScrapeRejectsMetadataAddressBeforeCallingFirecrawl() {
        ScrapeResult scrapeResult = firecrawlWebScrapeService.scrape("http://169.254.169.254/latest/meta-data");

        assertThat(scrapeResult.success()).isFalse();
        assertThat(scrapeResult.error()).contains("link-local address");

        verifyNoInteractions(objectMapper);
    }

    @Test
    void testCrawlRejectsPrivateAddressBeforeCallingFirecrawl() {
        CrawlResult crawlResult = firecrawlWebScrapeService.crawl("http://10.0.0.1/docs", 5, List.of());

        assertThat(crawlResult.success()).isFalse();
        assertThat(crawlResult.error()).contains("private or loopback address");

        verifyNoInteractions(objectMapper);
    }

    @Test
    void testScrapeRejectsNonHttpSchemeBeforeCallingFirecrawl() {
        ScrapeResult scrapeResult = firecrawlWebScrapeService.scrape("file:///etc/passwd");

        assertThat(scrapeResult.success()).isFalse();
        assertThat(scrapeResult.error()).contains("Only HTTP and HTTPS are permitted");

        verifyNoInteractions(objectMapper);
    }
}
