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

package com.bytechef.ai.mcp.server.config;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for bridging a remote MCP server into the local ByteChef MCP server.
 *
 * <p>When {@code bytechef.ai.mcp.client.remote.url} is set, the ByteChef MCP server acts as an
 * aggregator: it exposes its own built-in tools <em>and</em> transparently proxies every tool
 * advertised by the configured remote MCP server. This is the Spring Boot equivalent of
 * <a href="https://github.com/geelen/mcp-remote">mcp-remote</a>.
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties("bytechef.ai.mcp.client.remote")
public class McpRemoteBridgeProperties {

    /**
     * URL of the remote MCP server to bridge. When set, tools from this server are proxied through
     * the local ByteChef MCP server. Supports both Streamable HTTP ({@code /mcp}) and SSE
     * ({@code /sse}) endpoints.
     */
    private @Nullable String url;

    /**
     * Bearer token sent in the {@code Authorization} header when connecting to the remote server.
     * Leave empty if the remote server does not require authentication.
     */
    private @Nullable String authToken;

    /**
     * Maximum time to wait when establishing the initial connection to the remote MCP server.
     * Defaults to {@code 10s}.
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Maximum time to wait for individual MCP requests (tool listing, tool calls, etc.).
     * Defaults to {@code 30s}.
     */
    private Duration requestTimeout = Duration.ofSeconds(30);

    public @Nullable String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    public @Nullable String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(@Nullable String authToken) {
        this.authToken = authToken;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
