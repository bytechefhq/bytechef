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

import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteMcpServerCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteMcpServerCatalogClient.class);

    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(60);

    private final Clock clock;
    private final ObjectProvider<McpServerCatalogProxyClient> mcpServerCatalogProxyClientProvider;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile List<McpServerCatalogEntry> catalog = List.of();
    private volatile Instant lastRefreshAttempt;
    private volatile boolean lastRefreshFailed;

    @Autowired
    public RemoteMcpServerCatalogClient(
        ObjectProvider<McpServerCatalogProxyClient> mcpServerCatalogProxyClientProvider) {

        this(mcpServerCatalogProxyClientProvider, Clock.systemUTC());
    }

    RemoteMcpServerCatalogClient(
        ObjectProvider<McpServerCatalogProxyClient> mcpServerCatalogProxyClientProvider, Clock clock) {

        this.mcpServerCatalogProxyClientProvider = mcpServerCatalogProxyClientProvider;
        this.clock = clock;
    }

    public List<McpServerCatalogEntry> getMcpServerCatalog() {
        if (isRefreshDue() && refreshLock.tryLock()) {
            try {
                if (isRefreshDue()) {
                    refresh();
                }
            } finally {
                refreshLock.unlock();
            }
        }

        return List.copyOf(catalog);
    }

    private boolean isRefreshDue() {
        Instant attempt = lastRefreshAttempt;

        if (attempt == null) {
            return true;
        }

        Duration elapsed = Duration.between(attempt, clock.instant());

        return elapsed.compareTo(lastRefreshFailed ? RETRY_INTERVAL : REFRESH_INTERVAL) >= 0;
    }

    private void refresh() {
        lastRefreshAttempt = clock.instant();

        try {
            McpServerCatalogProxyClient mcpServerCatalogProxyClient =
                mcpServerCatalogProxyClientProvider.getIfAvailable();

            if (mcpServerCatalogProxyClient == null) {
                lastRefreshFailed = false;

                return;
            }

            List<McpServerCatalogEntry> entries = mcpServerCatalogProxyClient.getMcpServerCatalog();

            catalog = entries.stream()
                .filter(Objects::nonNull)
                .toList();
            lastRefreshFailed = false;
        } catch (RuntimeException runtimeException) {
            lastRefreshFailed = true;

            log.warn(
                "Unable to refresh the MCP server catalog for predefined OAuth apps; keeping {} cached entries: {}",
                catalog.size(), runtimeException.getMessage());
        }
    }
}
