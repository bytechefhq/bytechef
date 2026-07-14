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
import java.util.Optional;
import java.util.function.Function;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcSseServerTransportProvider;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
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

    private static final String SECRET_KEY = "secretKey";

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
     * Builds a {@link RouterFunction} that resolves the per-secret provider for a matching request and returns
     * <em>its</em> handler function to Spring.
     *
     * <p>
     * The handler is handed back to Spring rather than invoked here, so Spring MVC owns the async dispatch — this is
     * what keeps the provider's streaming {@code ServerResponse.sse(...)} response flowing to the client. The endpoint
     * patterns must carry a {@code {secretKey}} path variable.
     *
     * @param ssePathPattern     the SSE stream endpoint pattern, e.g. {@code /api/automation/{secretKey}/sse}
     * @param messagePathPattern the message endpoint pattern, e.g. {@code /api/automation/{secretKey}/message}
     */
    public RouterFunction<ServerResponse> toRouterFunction(String ssePathPattern, String messagePathPattern) {
        RequestPredicate requestPredicate = RequestPredicates.GET(ssePathPattern)
            .or(RequestPredicates.POST(messagePathPattern));

        return request -> {
            if (!requestPredicate.test(request)) {
                return Optional.empty();
            }

            WebMvcSseServerTransportProvider transportProvider = providerCache.get(
                request.pathVariable(SECRET_KEY), providerFactory);

            return transportProvider.getRouterFunction()
                .route(request);
        };
    }
}
