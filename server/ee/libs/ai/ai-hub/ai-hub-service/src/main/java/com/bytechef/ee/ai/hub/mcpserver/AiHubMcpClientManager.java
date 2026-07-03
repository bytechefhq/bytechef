/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Caches one initialized {@link McpSyncClient} per registered MCP server so the agent can resolve a server's tools on
 * every turn without paying a fresh connect+initialize handshake each time. The cache is keyed by server id plus a hash
 * of (url, token) so editing a server's URL or token rebuilds its client; entries expire after inactivity and are
 * closed on eviction.
 *
 * <p>
 * Lifecycle note: the {@link org.springframework.ai.tool.ToolCallback}s synthesized from a client hold a live reference
 * to it and are invoked LATER during the agent turn — so the client must stay open across the turn. Caching (rather
 * than connect-per-turn-then-close) is what makes that safe; the removal listener closes clients only once they fall
 * out of the cache.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AiHubMcpClientManager {

    private static final Logger log = LoggerFactory.getLogger(AiHubMcpClientManager.class);

    private final Cache<String, McpSyncClient> clientCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterAccess(Duration.ofMinutes(10))
        .<String, McpSyncClient>removalListener((key, client, cause) -> closeQuietly(client))
        .build();

    private final AiHubMcpServerUrlGuard urlGuard;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubMcpClientManager(AiHubMcpServerUrlGuard urlGuard) {
        this.urlGuard = urlGuard;
    }

    /**
     * Returns an initialized client for the server, creating + initializing it on first use. Throws (propagating the
     * connect/initialize failure) when the server is unreachable — callers skip that server for the turn.
     */
    public McpSyncClient getOrCreateClient(long serverId, String url, @Nullable String bearerToken) {
        return clientCache.get(cacheKey(serverId, url, bearerToken), key -> createClient(url, bearerToken));
    }

    /**
     * Lists the server's tools (name + description) by connecting via the cached client. Throws when the server is
     * unreachable — the caller decides whether to surface an empty list or an error.
     */
    public List<McpToolInfo> listTools(long serverId, String url, @Nullable String bearerToken) {
        McpSyncClient client = getOrCreateClient(serverId, url, bearerToken);

        return client.listTools()
            .tools()
            .stream()
            .map(tool -> new McpToolInfo(tool.name(), tool.description()))
            .toList();
    }

    /**
     * Drop (and close) the cached client for a server config — used to force a rebuild after a transient failure.
     */
    public void invalidate(long serverId, String url, @Nullable String bearerToken) {
        clientCache.invalidate(cacheKey(serverId, url, bearerToken));
    }

    /**
     * A discovered MCP tool — its name and (optional) description.
     */
    public record McpToolInfo(String name, @Nullable String description) {
    }

    private static String cacheKey(long serverId, String url, @Nullable String bearerToken) {
        return serverId + "#" + Objects.hash(url, bearerToken);
    }

    private McpSyncClient createClient(String url, @Nullable String bearerToken) {
        // Re-validate at connect time (not just at registration) so a host that resolved to a public address when
        // the server was added can't be re-pointed at an internal/cloud-metadata address via DNS rebinding before
        // the agent dials it. Throws UrlValidationException (unchecked) — the caller skips this server for the turn.
        urlGuard.validate(url);

        URI serverUri = URI.create(url);

        String baseUrl = serverUri.getScheme() + "://" + serverUri.getAuthority();
        String endpoint = serverUri.getRawPath();

        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(baseUrl)
            .endpoint(endpoint);

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.httpRequestCustomizer(
                (httpRequestBuilder, method, uri, body, transportContext) -> httpRequestBuilder.header(
                    "Authorization", "Bearer " + bearerToken));
        }

        McpClientTransport transport = builder.build();

        McpSyncClient client = McpClient.sync(transport)
            .build();

        client.initialize();

        return client;
    }

    private static void closeQuietly(@Nullable McpSyncClient client) {
        if (client == null) {
            return;
        }

        try {
            client.close();
        } catch (RuntimeException exception) {
            log.debug("Failed to close MCP client cleanly", exception);
        }
    }
}
