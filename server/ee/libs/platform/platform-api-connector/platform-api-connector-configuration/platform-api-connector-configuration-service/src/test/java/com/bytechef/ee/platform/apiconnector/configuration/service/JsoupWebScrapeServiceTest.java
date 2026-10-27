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
import com.bytechef.commons.util.UrlValidatorUtils.HostResolver;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.apiconnector.configuration.service.JsoupWebScrapeService.ResolvedTarget;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.CrawlResult;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.ScrapeResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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

    private final JsoupWebScrapeService jsoupWebScrapeService = new JsoupWebScrapeService(
        new ApplicationProperties());

    @Nested
    class ResolveTarget {

        @ParameterizedTest
        @ValueSource(strings = {
            "http://127.0.0.1/docs", "http://[::1]/docs", "http://10.0.0.1/docs", "http://172.16.0.1/docs",
            "http://192.168.1.1/docs", "http://169.254.169.254/latest/meta-data", "http://0.0.0.0/docs",
            "http://100.64.0.1/docs"
        })
        void testResolveTargetRejectsInternalTargets(String url) {
            assertThatThrownBy(() -> jsoupWebScrapeService.resolveTarget(url))
                .isInstanceOf(UrlValidationException.class)
                .hasMessageContaining("not allowed");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "file:///etc/passwd", "ftp://1.1.1.1/docs", "gopher://1.1.1.1/docs", "jar:file:/tmp/a.jar!/"
        })
        void testResolveTargetRejectsNonHttpSchemes(String url) {
            assertThatThrownBy(() -> jsoupWebScrapeService.resolveTarget(url))
                .isInstanceOf(UrlValidationException.class)
                .hasMessageContaining("Only HTTP and HTTPS are permitted");
        }

        @Test
        void testResolveTargetAllowsPublicAddress() {
            ResolvedTarget resolvedTarget = jsoupWebScrapeService.resolveTarget("https://1.1.1.1/docs");

            URI uri = resolvedTarget.uri();

            assertThat(uri.getHost()).isEqualTo("1.1.1.1");
            assertThat(resolvedTarget.addresses()).extracting(InetAddress::getHostAddress)
                .containsExactly("1.1.1.1");
        }
    }

    @Nested
    class ResolveRedirectTarget {

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

    @Nested
    class DnsRebinding {

        @Test
        void testScrapeConnectsOnlyToTheAddressThatPassedValidation() throws IOException {
            AtomicInteger requestCount = new AtomicInteger();

            HttpServer httpServer = startInternalServer(requestCount);

            try {
                AtomicInteger lookupCount = new AtomicInteger();
                InetAddress publicAddress = InetAddress.getByAddress("localhost", new byte[] {
                    (byte) 192, 0, 2, 1
                });

                HostResolver rebindingHostResolver = host -> lookupCount.getAndIncrement() == 0
                    ? new InetAddress[] {
                        publicAddress
                    }
                    : new InetAddress[] {
                        InetAddress.getLoopbackAddress()
                    };

                JsoupWebScrapeService rebindingJsoupWebScrapeService = new JsoupWebScrapeService(
                    Set.of(), rebindingHostResolver, Duration.ofMillis(500));

                InetSocketAddress serverAddress = httpServer.getAddress();

                ScrapeResult scrapeResult = rebindingJsoupWebScrapeService.scrape(
                    "http://localhost:" + serverAddress.getPort() + "/docs");

                assertThat(requestCount.get()).isZero();
                assertThat(scrapeResult.success()).isFalse();
                assertThat(scrapeResult.content()).isNull();
            } finally {
                httpServer.stop(0);
            }
        }

        private HttpServer startInternalServer(AtomicInteger requestCount) throws IOException {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

            httpServer.createContext("/", exchange -> {
                requestCount.incrementAndGet();

                byte[] body = "<html><body>internal secret</body></html>".getBytes(StandardCharsets.UTF_8);

                exchange.sendResponseHeaders(200, body.length);

                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
            });

            httpServer.start();

            return httpServer;
        }
    }

    @Nested
    class AllowedHosts {

        private final AtomicInteger requestCount = new AtomicInteger();
        private final HostResolver loopbackHostResolver = host -> new InetAddress[] {
            InetAddress.getLoopbackAddress()
        };

        @Test
        void testScrapeFetchesAllowlistedLoopbackHost() throws IOException {
            HttpServer httpServer = startServer();

            try {
                JsoupWebScrapeService allowlistJsoupWebScrapeService = new JsoupWebScrapeService(
                    Set.of("docs.internal"), loopbackHostResolver, Duration.ofSeconds(5));

                ScrapeResult scrapeResult = allowlistJsoupWebScrapeService.scrape(
                    "http://docs.internal:" + getPort(httpServer) + "/docs");

                assertThat(scrapeResult.success()).isTrue();
                assertThat(scrapeResult.content()).isEqualTo("internal docs");
                assertThat(requestCount.get()).isEqualTo(1);
            } finally {
                httpServer.stop(0);
            }
        }

        @Test
        void testScrapeRejectsLoopbackHostThatIsNotAllowlisted() throws IOException {
            HttpServer httpServer = startServer();

            try {
                JsoupWebScrapeService allowlistJsoupWebScrapeService = new JsoupWebScrapeService(
                    Set.of("other.internal"), loopbackHostResolver, Duration.ofSeconds(5));

                ScrapeResult scrapeResult = allowlistJsoupWebScrapeService.scrape(
                    "http://docs.internal:" + getPort(httpServer) + "/docs");

                assertThat(scrapeResult.success()).isFalse();
                assertThat(scrapeResult.error()).contains("private or loopback address");
                assertThat(requestCount.get()).isZero();
            } finally {
                httpServer.stop(0);
            }
        }

        @Test
        void testScrapeRejectsRedirectFromAllowlistedHostToMetadataAddress() throws IOException {
            HttpServer httpServer = startServer();

            try {
                JsoupWebScrapeService allowlistJsoupWebScrapeService = new JsoupWebScrapeService(
                    Set.of("docs.internal"), host -> host.equals("docs.internal")
                        ? new InetAddress[] {
                            InetAddress.getLoopbackAddress()
                        }
                        : InetAddress.getAllByName(host),
                    Duration.ofSeconds(5));

                ScrapeResult scrapeResult = allowlistJsoupWebScrapeService.scrape(
                    "http://docs.internal:" + getPort(httpServer) + "/redirect");

                assertThat(scrapeResult.success()).isFalse();
                assertThat(scrapeResult.error()).contains("link-local address");
                assertThat(requestCount.get()).isEqualTo(1);
            } finally {
                httpServer.stop(0);
            }
        }

        @Test
        void testGetAllowedHostsReadsTheSecuritySsrfAllowlist() {
            ApplicationProperties applicationProperties = new ApplicationProperties();
            ApplicationProperties.Security security = new ApplicationProperties.Security();

            security.getSsrf()
                .setAllowedHosts(List.of("docs.internal", "10.0.0.5"));

            applicationProperties.setSecurity(security);

            assertThat(JsoupWebScrapeService.getAllowedHosts(applicationProperties))
                .containsExactlyInAnyOrder("docs.internal", "10.0.0.5");
        }

        @Test
        void testGetAllowedHostsIsEmptyWhenNotConfigured() {
            assertThat(JsoupWebScrapeService.getAllowedHosts(new ApplicationProperties())).isEmpty();
        }

        private HttpServer startServer() throws IOException {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

            httpServer.createContext("/docs", exchange -> {
                requestCount.incrementAndGet();

                byte[] body = "<html><body><p>internal docs</p></body></html>".getBytes(StandardCharsets.UTF_8);

                exchange.sendResponseHeaders(200, body.length);

                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
            });
            httpServer.createContext("/redirect", exchange -> {
                requestCount.incrementAndGet();

                exchange.getResponseHeaders()
                    .add("Location", "http://169.254.169.254/latest/meta-data");

                exchange.sendResponseHeaders(302, -1);

                exchange.close();
            });

            httpServer.start();

            return httpServer;
        }

        private int getPort(HttpServer httpServer) {
            InetSocketAddress serverAddress = httpServer.getAddress();

            return serverAddress.getPort();
        }
    }
}
