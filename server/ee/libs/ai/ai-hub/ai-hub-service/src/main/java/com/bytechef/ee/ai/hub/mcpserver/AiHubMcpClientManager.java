/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
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

    public McpSyncClient getOrCreateClient(long serverId, String url, @Nullable String bearerToken) {
        return clientCache.get(cacheKey(serverId, url, bearerToken), key -> createClient(url, bearerToken));
    }

    public List<McpToolInfo> listTools(long serverId, String url, @Nullable String bearerToken) {
        McpSyncClient client = getOrCreateClient(serverId, url, bearerToken);

        return client.listTools()
            .tools()
            .stream()
            .map(tool -> new McpToolInfo(tool.name(), tool.description()))
            .toList();
    }

    public void invalidate(long serverId, String url, @Nullable String bearerToken) {
        clientCache.invalidate(cacheKey(serverId, url, bearerToken));
    }

    public record McpToolInfo(String name, @Nullable String description) {
    }

    private static String cacheKey(long serverId, String url, @Nullable String bearerToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            digest.update(url.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);

            if (bearerToken != null) {
                digest.update(bearerToken.getBytes(StandardCharsets.UTF_8));
            }

            return serverId + "#" + HexFormat.of()
                .formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private McpSyncClient createClient(String url, @Nullable String bearerToken) {
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
