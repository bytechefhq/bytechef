/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.CrawlResult;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.ScrapeResult;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class JsoupWebScrapeServiceTest {

    private final JsoupWebScrapeService jsoupWebScrapeService = new JsoupWebScrapeService();

    @Nested
    class ValidateUrl {

        @ParameterizedTest
        @ValueSource(strings = {
            "http://127.0.0.1/docs", "http://[::1]/docs", "http://10.0.0.1/docs", "http://172.16.0.1/docs",
            "http://192.168.1.1/docs", "http://169.254.169.254/latest/meta-data", "http://0.0.0.0/docs",
            "http://100.64.0.1/docs"
        })
        void testValidateUrlRejectsInternalTargets(String url) {
            assertThatThrownBy(() -> JsoupWebScrapeService.validateUrl(url))
                .isInstanceOf(UrlValidationException.class)
                .hasMessageContaining("not allowed");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "file:///etc/passwd", "ftp://1.1.1.1/docs", "gopher://1.1.1.1/docs", "jar:file:/tmp/a.jar!/"
        })
        void testValidateUrlRejectsNonHttpSchemes(String url) {
            assertThatThrownBy(() -> JsoupWebScrapeService.validateUrl(url))
                .isInstanceOf(UrlValidationException.class)
                .hasMessageContaining("Only HTTP and HTTPS are permitted");
        }

        @Test
        void testValidateUrlAllowsPublicAddress() {
            URI uri = JsoupWebScrapeService.validateUrl("https://1.1.1.1/docs");

            assertThat(uri.getHost()).isEqualTo("1.1.1.1");
        }
    }

    @Nested
    class ResolveRedirectTarget {

        @Test
        void testResolveRedirectTargetRejectsRedirectToMetadataAddress() {
            URI currentUri = URI.create("https://1.1.1.1/docs");

            assertThatThrownBy(
                () -> JsoupWebScrapeService.resolveRedirectTarget(currentUri, "http://169.254.169.254/latest"))
                    .isInstanceOf(UrlValidationException.class)
                    .hasMessageContaining("link-local");
        }

        @Test
        void testResolveRedirectTargetRejectsRedirectToLoopback() {
            URI currentUri = URI.create("https://1.1.1.1/docs");

            assertThatThrownBy(
                () -> JsoupWebScrapeService.resolveRedirectTarget(currentUri, "http://127.0.0.1:8080/admin"))
                    .isInstanceOf(UrlValidationException.class)
                    .hasMessageContaining("loopback");
        }

        @Test
        void testResolveRedirectTargetResolvesRelativeLocationOnPublicHost() {
            URI currentUri = URI.create("https://1.1.1.1/docs/index.html");

            assertThat(JsoupWebScrapeService.resolveRedirectTarget(currentUri, "../api/reference"))
                .isEqualTo("https://1.1.1.1/api/reference");
        }
    }

    @Nested
    class Scrape {

        @Test
        void testScrapeRejectsLoopbackWithoutFetching() {
            ScrapeResult scrapeResult = jsoupWebScrapeService.scrape("http://127.0.0.1:1/docs");

            assertThat(scrapeResult.success()).isFalse();
            assertThat(scrapeResult.error()).contains("private or loopback address");
        }

        @Test
        void testScrapeRejectsMetadataAddressWithoutFetching() {
            ScrapeResult scrapeResult = jsoupWebScrapeService.scrape("http://169.254.169.254/latest/meta-data");

            assertThat(scrapeResult.success()).isFalse();
            assertThat(scrapeResult.error()).contains("link-local address");
        }
    }

    @Nested
    class Crawl {

        @Test
        void testCrawlRejectsPrivateSeedUrl() {
            CrawlResult crawlResult = jsoupWebScrapeService.crawl("http://192.168.1.1/docs", 5, List.of());

            assertThat(crawlResult.success()).isFalse();
            assertThat(crawlResult.error()).contains("private or loopback address");
        }

        @Test
        void testCrawlRejectsNonHttpSeedUrl() {
            CrawlResult crawlResult = jsoupWebScrapeService.crawl("file:///etc/passwd", 5, List.of());

            assertThat(crawlResult.success()).isFalse();
            assertThat(crawlResult.error()).contains("Only HTTP and HTTPS are permitted");
        }
    }
}
