/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.CrawlResult;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.ScrapeResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
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

    @Test
    void testScrapeAllowsAllowlistedPrivateHostThroughToFirecrawl() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Security security = new ApplicationProperties.Security();

        security.getSsrf()
            .setAllowedHosts(List.of("10.0.0.1"));

        applicationProperties.setSecurity(security);

        when(objectMapper.writeValueAsString(any())).thenThrow(new IllegalStateException("request reached Firecrawl"));

        FirecrawlWebScrapeService allowlistFirecrawlWebScrapeService = new FirecrawlWebScrapeService(
            applicationProperties, objectMapper);

        ScrapeResult scrapeResult = allowlistFirecrawlWebScrapeService.scrape("http://10.0.0.1/docs");

        assertThat(scrapeResult.success()).isFalse();
        assertThat(scrapeResult.error()).isEqualTo("request reached Firecrawl");

        verify(objectMapper).writeValueAsString(any());
    }

    @Nested
    class FetchedUrlValidation {

        private static final String CRAWL_STATUS = """
            {"status": "completed", "data": [
              {"markdown": "public docs",
               "metadata": {"sourceURL": "https://1.1.1.1/docs", "url": "https://1.1.1.1/docs"}},
              {"markdown": "internal secret",
               "metadata": {"sourceURL": "https://1.1.1.1/admin", "url": "http://10.0.0.5/admin"}}
            ]}
            """;

        @Test
        void testScrapeRejectsContentFirecrawlFetchedFromALinkLocalAddress() throws IOException {
            HttpServer httpServer = startFirecrawlServer("""
                {"success": true, "data": {"markdown": "instance credentials",
                 "metadata": {"sourceURL": "https://1.1.1.1/docs", "url": "http://169.254.169.254/latest/meta-data"}}}
                """);

            try {
                ScrapeResult scrapeResult = createFirecrawlWebScrapeService(httpServer).scrape("https://1.1.1.1/docs");

                assertThat(scrapeResult.success()).isFalse();
                assertThat(scrapeResult.content()).isNull();
                assertThat(scrapeResult.error()).contains("link-local address");
            } finally {
                httpServer.stop(0);
            }
        }

        @Test
        void testScrapeReturnsContentFirecrawlFetchedFromAPublicAddress() throws IOException {
            HttpServer httpServer = startFirecrawlServer("""
                {"success": true, "data": {"markdown": "public docs",
                 "metadata": {"sourceURL": "https://1.1.1.1/docs", "url": "https://1.1.1.1/docs"}}}
                """);

            try {
                ScrapeResult scrapeResult = createFirecrawlWebScrapeService(httpServer).scrape("https://1.1.1.1/docs");

                assertThat(scrapeResult.success()).isTrue();
                assertThat(scrapeResult.content()).isEqualTo("public docs");
            } finally {
                httpServer.stop(0);
            }
        }

        @Test
        void testCrawlDropsPagesFirecrawlFetchedFromAPrivateAddress() throws IOException {
            HttpServer httpServer = startFirecrawlServer("{\"id\": \"crawl-1\"}");

            try {
                CrawlResult crawlResult = createFirecrawlWebScrapeService(httpServer).crawl(
                    "https://1.1.1.1/docs", 5, List.of());

                assertThat(crawlResult.success()).isTrue();
                assertThat(crawlResult.crawledUrls()).containsExactly("https://1.1.1.1/docs");
                assertThat(crawlResult.content()).contains("public docs")
                    .doesNotContain("internal secret");
            } finally {
                httpServer.stop(0);
            }
        }

        private FirecrawlWebScrapeService createFirecrawlWebScrapeService(HttpServer httpServer) {
            ApplicationProperties applicationProperties = new ApplicationProperties();

            ApplicationProperties.Ai.Firecrawl firecrawl = applicationProperties.getAi()
                .getFirecrawl();

            InetSocketAddress serverAddress = httpServer.getAddress();

            firecrawl.setApiKey("test-key");
            firecrawl.setBaseUrl("http://127.0.0.1:" + serverAddress.getPort());

            return new FirecrawlWebScrapeService(applicationProperties, new ObjectMapper());
        }

        private HttpServer startFirecrawlServer(String startResponse) throws IOException {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

            httpServer.createContext("/scrape", exchange -> respond(exchange, startResponse));
            httpServer.createContext("/crawl", exchange -> respond(
                exchange, "POST".equals(exchange.getRequestMethod()) ? startResponse : CRAWL_STATUS));

            httpServer.start();

            return httpServer;
        }

        private static void respond(HttpExchange exchange, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");

            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }
}
