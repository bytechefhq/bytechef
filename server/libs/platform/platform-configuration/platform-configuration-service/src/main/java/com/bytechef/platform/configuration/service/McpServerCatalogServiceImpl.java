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

import com.bytechef.platform.configuration.dto.McpServerCatalogAuthenticationDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogHeaderDTO;
import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogHeader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class McpServerCatalogServiceImpl implements McpServerCatalogService {

    private static final Logger log = LoggerFactory.getLogger(McpServerCatalogServiceImpl.class);

    static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private final Clock clock;
    private final @Nullable McpServerCatalogProxyClient mcpServerCatalogProxyClient;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile List<McpServerCatalogEntryDTO> catalog = List.of();
    private volatile Instant lastRefreshAttempt;

    @Autowired
    public McpServerCatalogServiceImpl(
        ObjectProvider<McpServerCatalogProxyClient> mcpServerCatalogProxyClientProvider) {
        this(mcpServerCatalogProxyClientProvider.getIfAvailable(), Clock.systemUTC());
    }

    McpServerCatalogServiceImpl(@Nullable McpServerCatalogProxyClient mcpServerCatalogProxyClient, Clock clock) {
        this.mcpServerCatalogProxyClient = mcpServerCatalogProxyClient;
        this.clock = clock;
    }

    @Override
    @SuppressFBWarnings(value = "EI", justification = "The cached catalog is always an unmodifiable list")
    public List<McpServerCatalogEntryDTO> getMcpServerCatalog() {
        if (mcpServerCatalogProxyClient == null) {
            return catalog;
        }

        if (isRefreshDue() && refreshLock.tryLock()) {
            try {
                if (isRefreshDue()) {
                    refresh();
                }
            } finally {
                refreshLock.unlock();
            }
        }

        return catalog;
    }

    private boolean isRefreshDue() {
        Instant attempt = lastRefreshAttempt;

        if (attempt == null) {
            return true;
        }

        Duration elapsed = Duration.between(attempt, clock.instant());

        return elapsed.compareTo(REFRESH_INTERVAL) >= 0;
    }

    private void refresh() {
        lastRefreshAttempt = clock.instant();

        try {
            List<McpServerCatalogEntry> entries = Objects.requireNonNull(mcpServerCatalogProxyClient)
                .getMcpServerCatalog();

            List<McpServerCatalogEntryDTO> validEntries = entries.stream()
                .filter(McpServerCatalogServiceImpl::isComplete)
                .map(McpServerCatalogServiceImpl::toDTO)
                .toList();

            if (validEntries.size() < entries.size()) {
                log.warn(
                    "Dropped {} MCP server catalog entries missing required fields",
                    entries.size() - validEntries.size());
            }

            catalog = validEntries;
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to refresh the MCP server catalog; keeping {} cached entries: {}", catalog.size(),
                runtimeException.getMessage());
        }
    }

    private static boolean isComplete(McpServerCatalogEntry entry) {
        McpServerCatalogAuthentication authentication = entry.authentication();

        return isPresent(entry.slug()) && isPresent(entry.title()) && isPresent(entry.url()) &&
            isPresent(entry.transportType()) && authentication != null && isPresent(authentication.type());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static McpServerCatalogEntryDTO toDTO(McpServerCatalogEntry entry) {
        return new McpServerCatalogEntryDTO(
            entry.slug(), entry.title(), entry.description(), entry.url(), entry.urlParameters(),
            entry.transportType(), toDTO(entry.authentication()), entry.websiteUrl(), entry.component());
    }

    private static McpServerCatalogAuthenticationDTO toDTO(McpServerCatalogAuthentication authentication) {
        List<McpServerCatalogHeader> headers = authentication.headers();

        List<McpServerCatalogHeaderDTO> headerDTOs = headers == null
            ? null
            : headers.stream()
                .filter(header -> isPresent(header.name()))
                .map(McpServerCatalogServiceImpl::toDTO)
                .toList();

        return new McpServerCatalogAuthenticationDTO(
            authentication.type(), authentication.dynamicClientRegistration(),
            authentication.clientIdMetadataDocument(), authentication.authorizationUrl(), authentication.tokenUrl(),
            authentication.pkce(), authentication.scopes(), headerDTOs);
    }

    private static McpServerCatalogHeaderDTO toDTO(McpServerCatalogHeader header) {
        return new McpServerCatalogHeaderDTO(header.name(), header.description(), header.secret());
    }
}
