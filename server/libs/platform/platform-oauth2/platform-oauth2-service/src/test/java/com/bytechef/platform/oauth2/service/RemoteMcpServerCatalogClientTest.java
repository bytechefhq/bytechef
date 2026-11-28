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

package com.bytechef.platform.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class RemoteMcpServerCatalogClientTest {

    private static final McpServerCatalogEntry LINEAR = new McpServerCatalogEntry(
        "linear", "Linear", null, "https://mcp.linear.app/mcp", null, "httpStreamable",
        new McpServerCatalogAuthentication(
            "oauth2", false, null, "https://linear.app/oauth/authorize", "https://api.linear.app/oauth/token", true,
            null, null),
        null, "linear");

    private final MutableClock clock = new MutableClock();
    private final McpServerCatalogProxyClient mcpServerCatalogProxyClient = mock(McpServerCatalogProxyClient.class);
    private final ObjectProvider<McpServerCatalogProxyClient> objectProvider = mock(ObjectProvider.class);

    @BeforeEach
    void beforeEach() {
        when(objectProvider.getIfAvailable()).thenReturn(mcpServerCatalogProxyClient);
    }

    @Test
    void testNoClientMeansEmpty() {
        when(objectProvider.getIfAvailable()).thenReturn(null);

        RemoteMcpServerCatalogClient remoteMcpServerCatalogClient =
            new RemoteMcpServerCatalogClient(objectProvider, clock);

        assertThat(remoteMcpServerCatalogClient.getMcpServerCatalog()).isEmpty();
    }

    @Test
    void testCachesForOneHour() {
        when(mcpServerCatalogProxyClient.getMcpServerCatalog()).thenReturn(List.of(LINEAR));

        RemoteMcpServerCatalogClient remoteMcpServerCatalogClient =
            new RemoteMcpServerCatalogClient(objectProvider, clock);

        assertThat(remoteMcpServerCatalogClient.getMcpServerCatalog()).containsExactly(LINEAR);

        clock.advanceSeconds(3599);

        remoteMcpServerCatalogClient.getMcpServerCatalog();

        verify(mcpServerCatalogProxyClient, times(1)).getMcpServerCatalog();

        clock.advanceSeconds(1);

        remoteMcpServerCatalogClient.getMcpServerCatalog();

        verify(mcpServerCatalogProxyClient, times(2)).getMcpServerCatalog();
    }

    @Test
    void testFailureKeepsLastGoodCopyAndRetriesAfterOneMinute() {
        when(mcpServerCatalogProxyClient.getMcpServerCatalog())
            .thenReturn(List.of(LINEAR))
            .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        RemoteMcpServerCatalogClient remoteMcpServerCatalogClient =
            new RemoteMcpServerCatalogClient(objectProvider, clock);

        remoteMcpServerCatalogClient.getMcpServerCatalog();

        clock.advanceSeconds(3600);

        assertThat(remoteMcpServerCatalogClient.getMcpServerCatalog()).containsExactly(LINEAR);

        clock.advanceSeconds(59);

        remoteMcpServerCatalogClient.getMcpServerCatalog();

        verify(mcpServerCatalogProxyClient, times(2)).getMcpServerCatalog();

        clock.advanceSeconds(1);

        remoteMcpServerCatalogClient.getMcpServerCatalog();

        verify(mcpServerCatalogProxyClient, times(3)).getMcpServerCatalog();
    }

    @Test
    void testDropsNullEntries() {
        when(mcpServerCatalogProxyClient.getMcpServerCatalog()).thenReturn(Arrays.asList(LINEAR, null));

        RemoteMcpServerCatalogClient remoteMcpServerCatalogClient =
            new RemoteMcpServerCatalogClient(objectProvider, clock);

        assertThat(remoteMcpServerCatalogClient.getMcpServerCatalog()).containsExactly(LINEAR);
    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
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
