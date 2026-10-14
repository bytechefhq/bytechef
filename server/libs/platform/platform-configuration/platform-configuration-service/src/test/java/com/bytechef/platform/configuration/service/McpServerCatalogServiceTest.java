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

package com.bytechef.platform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.dto.McpServerCatalogAuthenticationDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogHeaderDTO;
import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogHeader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpServerCatalogServiceTest {

    private static final McpServerCatalogEntry NOTION = new McpServerCatalogEntry(
        "notion", "Notion", "Official Notion MCP server", "https://mcp.notion.com/mcp", null, "httpStreamable",
        new McpServerCatalogAuthentication("oauth2", true, null, null, null, null, null, null),
        "https://developers.notion.com/docs/mcp", "notion");

    private McpServerCatalogProxyClient proxyClient;
    private MutableClock clock;
    private McpServerCatalogServiceImpl service;

    @BeforeEach
    void beforeEach() {
        proxyClient = mock(McpServerCatalogProxyClient.class);
        clock = new MutableClock(Instant.parse("2026-10-14T10:00:00Z"));
        service = new McpServerCatalogServiceImpl(proxyClient, clock);
    }

    @Test
    void testCatalogIsEmptyWithoutAProxyClient() {
        McpServerCatalogServiceImpl serviceWithoutProxyClient = new McpServerCatalogServiceImpl(null, clock);

        assertThat(serviceWithoutProxyClient.getMcpServerCatalog()).isEmpty();
    }

    @Test
    void testFirstCallFetchesAndMapsTheCatalog() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of(NOTION));

        List<McpServerCatalogEntryDTO> entries = service.getMcpServerCatalog();

        assertThat(entries).hasSize(1);

        McpServerCatalogEntryDTO entry = entries.getFirst();

        assertThat(entry.slug()).isEqualTo("notion");

        McpServerCatalogAuthenticationDTO authentication = entry.authentication();

        assertThat(authentication.dynamicClientRegistration()).isTrue();
    }

    @Test
    void testSecondCallWithinTheHourDoesNotFetchAgain() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of(NOTION));

        service.getMcpServerCatalog();
        clock.advance(Duration.ofMinutes(59));
        service.getMcpServerCatalog();

        verify(proxyClient, times(1)).getMcpServerCatalog();
    }

    @Test
    void testCatalogIsRefreshedAfterAnHour() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of(NOTION));

        service.getMcpServerCatalog();
        clock.advance(Duration.ofMinutes(61));
        service.getMcpServerCatalog();

        verify(proxyClient, times(2)).getMcpServerCatalog();
    }

    @Test
    void testEmptyCatalogIsCachedLikeAnyOther() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of());

        assertThat(service.getMcpServerCatalog()).isEmpty();
        assertThat(service.getMcpServerCatalog()).isEmpty();

        verify(proxyClient, times(1)).getMcpServerCatalog();
    }

    @Test
    void testFailureKeepsPreviousCatalogAndDoesNotRetryWithinTheHour() {
        when(proxyClient.getMcpServerCatalog())
            .thenReturn(List.of(NOTION))
            .thenThrow(new IllegalStateException("proxy down"));

        service.getMcpServerCatalog();
        clock.advance(Duration.ofMinutes(61));

        assertThat(service.getMcpServerCatalog()).hasSize(1);

        clock.advance(Duration.ofMinutes(30));

        assertThat(service.getMcpServerCatalog()).hasSize(1);

        verify(proxyClient, times(2)).getMcpServerCatalog();
    }

    @Test
    void testFirstCallFailureReturnsEmptyAndDoesNotRetryWithinTheHour() {
        when(proxyClient.getMcpServerCatalog()).thenThrow(new IllegalStateException("proxy down"));

        assertThat(service.getMcpServerCatalog()).isEmpty();

        clock.advance(Duration.ofMinutes(30));

        assertThat(service.getMcpServerCatalog()).isEmpty();

        verify(proxyClient, times(1)).getMcpServerCatalog();
    }

    @Test
    void testEntriesMissingRequiredFieldsAreDropped() {
        McpServerCatalogEntry missingAuthentication = new McpServerCatalogEntry(
            "broken", "Broken", null, "https://broken.example.com/mcp", null, "httpStreamable", null, null, null);
        McpServerCatalogEntry missingSlug = new McpServerCatalogEntry(
            null, "No slug", null, "https://x.example.com/mcp", null, "sse",
            new McpServerCatalogAuthentication("none", null, null, null, null, null, null, null), null, null);
        McpServerCatalogEntry headerWithoutName = new McpServerCatalogEntry(
            "api-key", "Api key", null, "https://key.example.com/mcp", null, "httpStreamable",
            new McpServerCatalogAuthentication(
                "header", null, null, null, null, null, null,
                List.of(new McpServerCatalogHeader(null, "nameless", true),
                    new McpServerCatalogHeader("Authorization", "API key", true))),
            null, null);

        when(proxyClient.getMcpServerCatalog())
            .thenReturn(List.of(NOTION, missingAuthentication, missingSlug, headerWithoutName));

        List<McpServerCatalogEntryDTO> entries = service.getMcpServerCatalog();

        assertThat(entries).extracting(McpServerCatalogEntryDTO::slug)
            .containsExactly("notion", "api-key");

        McpServerCatalogEntryDTO apiKeyEntry = entries.get(1);

        McpServerCatalogAuthenticationDTO authentication = apiKeyEntry.authentication();

        assertThat(authentication.headers()).extracting(McpServerCatalogHeaderDTO::name)
            .containsExactly("Authorization");
    }

    @Test
    void testConcurrentCallerDuringRefreshGetsCurrentCopyWithoutWaiting() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch releaseRefresh = new CountDownLatch(1);

        when(proxyClient.getMcpServerCatalog()).thenAnswer(invocation -> {
            refreshStarted.countDown();

            if (!releaseRefresh.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The refresh was never released");
            }

            return List.of(NOTION);
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<List<McpServerCatalogEntryDTO>> refreshing = executorService.submit(service::getMcpServerCatalog);

            assertThat(refreshStarted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<List<McpServerCatalogEntryDTO>> concurrent = executorService.submit(service::getMcpServerCatalog);

            assertThat(concurrent.get(2, TimeUnit.SECONDS)).isEmpty();

            releaseRefresh.countDown();

            assertThat(refreshing.get(5, TimeUnit.SECONDS)).hasSize(1);
        } finally {
            releaseRefresh.countDown();
            executorService.shutdownNow();
        }

        verify(proxyClient, times(1)).getMcpServerCatalog();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
