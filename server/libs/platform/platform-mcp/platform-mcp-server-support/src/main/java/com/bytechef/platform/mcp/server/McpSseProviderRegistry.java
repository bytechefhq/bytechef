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

package com.bytechef.platform.mcp.server;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcSseServerTransportProvider;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Resolves one {@link WebMvcSseServerTransportProvider} per secret key.
 *
 * <p>
 * The MCP HTTP+SSE transport advertises its message endpoint to clients verbatim (no path-variable substitution) and
 * derives the transport context only from the {@code /message} request. A single shared provider therefore cannot carry
 * a {@code {secretKey}} path variable, so each secret gets its own provider built with concrete endpoints. Providers
 * are created lazily and held in a bounded, idle-expiring cache; evicted providers are shut down gracefully.
 *
 * @author Ivica Cardic
 */
public class McpSseProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(McpSseProviderRegistry.class);

    private static final int MAX_PROVIDERS = 1000;
    private static final Duration IDLE_TTL = Duration.ofMinutes(30);

    private final Cache<String, WebMvcSseServerTransportProvider> providerCache;
    private final Function<String, WebMvcSseServerTransportProvider> providerFactory;

    @SuppressFBWarnings("EI")
    public McpSseProviderRegistry(Function<String, WebMvcSseServerTransportProvider> providerFactory) {
        this.providerFactory = providerFactory;
        this.providerCache = Caffeine.newBuilder()
            .maximumSize(MAX_PROVIDERS)
            .expireAfterAccess(IDLE_TTL)
            .<String, WebMvcSseServerTransportProvider>removalListener((secretKey, provider, cause) -> {
                if (provider != null) {
                    provider.closeGracefully()
                        .subscribe();
                }
            })
            .build();
    }

    /**
     * Routes an SSE or message request to the provider that owns {@code secretKey}, creating it on first use. Returns
     * 404 when the resolved provider does not match the request (should not happen, since the dispatcher matches the
     * same concrete paths the provider registers).
     */
    public ServerResponse route(ServerRequest request, String secretKey) {
        WebMvcSseServerTransportProvider provider = providerCache.get(secretKey, providerFactory);

        return provider.getRouterFunction()
            .route(request)
            .map(handlerFunction -> handle(handlerFunction, request))
            .orElseGet(() -> ServerResponse.notFound()
                .build());
    }

    private static ServerResponse handle(HandlerFunction<ServerResponse> handlerFunction, ServerRequest request) {
        try {
            return handlerFunction.handle(request);
        } catch (Exception exception) {
            log.error("Failed to handle MCP SSE request", exception);

            throw new IllegalStateException(exception);
        }
    }
}
