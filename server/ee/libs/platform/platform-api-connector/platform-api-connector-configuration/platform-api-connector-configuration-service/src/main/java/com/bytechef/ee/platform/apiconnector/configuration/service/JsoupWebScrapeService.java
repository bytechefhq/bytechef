/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.commons.util.UrlValidatorUtils.HostResolver;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.apiconnector.configuration.service.PinnedAddressHttpClient.Response;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import javax.net.ssl.SSLSocketFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger(JsoupWebScrapeService.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_CRAWL_PAGES = 20;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_RESPONSE_BODY_BYTES = 10 * 1024 * 1024;
    private static final Map<String, String> REQUEST_HEADERS = Map.of(
        "User-Agent", "ByteChef Web Scraper",
        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

    private final Set<String> allowedHosts;
    private final HostResolver hostResolver;
    private final PinnedAddressHttpClient pinnedAddressHttpClient;

    @Autowired
    public JsoupWebScrapeService(ApplicationProperties applicationProperties) {
        this(
            getAllowedHosts(applicationProperties), InetAddress::getAllByName,
            Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
    }

    JsoupWebScrapeService(Set<String> allowedHosts, HostResolver hostResolver, Duration connectTimeout) {
        this.allowedHosts = Set.copyOf(allowedHosts);
        this.hostResolver = hostResolver;
        this.pinnedAddressHttpClient = new PinnedAddressHttpClient(
            connectTimeout, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS), MAX_RESPONSE_BODY_BYTES,
            (SSLSocketFactory) SSLSocketFactory.getDefault());
    }

    @Override
    public ScrapeResult scrape(String url) {
        try {
            String content = fetchAndParse(url);

            return ScrapeResult.success(content);
        } catch (Exception exception) {
            log.error("Failed to scrape URL: {}", url, exception);

            return ScrapeResult.failure(exception.getMessage());
        }
    }

    @Override
    public CrawlResult crawl(String url, int maxPages, List<String> includePatterns) {
        int effectiveMaxPages = maxPages > 0 ? Math.min(maxPages, DEFAULT_MAX_CRAWL_PAGES) : DEFAULT_MAX_CRAWL_PAGES;

        try {
            ResolvedTarget resolvedTarget = resolveTarget(url);

            URI baseUri = resolvedTarget.uri();
            String baseHost = baseUri.getHost();

            List<Pattern> patterns = includePatterns.stream()
                .map(JsoupWebScrapeService::convertGlobToRegex)
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
                    log.warn("Failed to crawl URL: {}", currentUrl, exception);
                }
            }

            return CrawlResult.success(combinedContent.toString(), crawledUrls);
        } catch (Exception exception) {
            log.error("Failed to crawl starting from URL: {}", url, exception);

            return CrawlResult.failure(exception.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "jsoup";
    }

    private static Pattern convertGlobToRegex(String glob) {
        StringBuilder regex = new StringBuilder();

        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);

            switch (c) {
                case '*':
                    regex.append(".*");

                    break;
                case '?':
                    regex.append(".");

                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                case '{':
                case '}':
                case '[':
                case ']':
                case '\\':
                    regex.append("\\");
                    regex.append(c);

                    break;
                default:
                    regex.append(c);
            }
        }

        return Pattern.compile(regex.toString());
    }

    private String fetchAndParse(String url) throws IOException {
        FetchedPage fetchedPage = fetch(url);

        Document document = Jsoup.parse(fetchedPage.body());

        document.select("script, style, nav, footer, header, aside, noscript, iframe")
            .remove();

        return document.body()
            .text();
    }

    private String fetchAndParseWithLinks(
        String url, String baseHost, List<Pattern> patterns, Queue<String> queue, Set<String> visited)
        throws IOException {

        FetchedPage fetchedPage = fetch(url);

        Document document = Jsoup.parse(fetchedPage.body(), fetchedPage.url());

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
                log.debug("Invalid URL: {}", href);
            }
        }

        document.select("script, style, nav, footer, header, aside, noscript, iframe")
            .remove();

        return document.body()
            .text();
    }

    ResolvedTarget resolveTarget(String url) {
        List<InetAddress> addresses = UrlValidatorUtils.resolveValidatedAddresses(url, allowedHosts, hostResolver);

        return new ResolvedTarget(URI.create(url), addresses);
    }

    static Set<String> getAllowedHosts(ApplicationProperties applicationProperties) {
        ApplicationProperties.Security security = applicationProperties.getSecurity();

        if (security == null || security.getSsrf() == null) {
            return Set.of();
        }

        ApplicationProperties.Security.Ssrf ssrf = security.getSsrf();

        List<String> allowedHosts = ssrf.getAllowedHosts();

        if (allowedHosts == null) {
            return Set.of();
        }

        return Set.copyOf(allowedHosts);
    }

    static String resolveRedirectTarget(URI currentUri, String location) {
        URI targetUri = currentUri.resolve(location);

        return targetUri.toString();
    }

    private FetchedPage fetch(String url) throws IOException {
        String currentUrl = url;

        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            ResolvedTarget resolvedTarget = resolveTarget(currentUrl);

            Response response = pinnedAddressHttpClient.get(
                resolvedTarget.uri(), resolvedTarget.addresses(), REQUEST_HEADERS);

            int statusCode = response.statusCode();

            if (!isRedirect(statusCode)) {
                if (statusCode != 200) {
                    throw new IOException("HTTP " + statusCode);
                }

                return new FetchedPage(currentUrl, response.body());
            }

            String location = response.location();

            if (location == null) {
                throw new IOException("HTTP " + statusCode + " without a Location header");
            }

            currentUrl = resolveRedirectTarget(resolvedTarget.uri(), location);
        }

        throw new IOException("Too many redirects while fetching " + url);
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    private record FetchedPage(String url, String body) {
    }

    record ResolvedTarget(URI uri, List<InetAddress> addresses) {
    }
}
