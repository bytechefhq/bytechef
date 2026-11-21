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

package com.bytechef.platform.githubproxy.client.internal;

import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalog;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
public final class RestMcpServerCatalogProxyClient implements McpServerCatalogProxyClient {

    private final RestClient restClient;

    public RestMcpServerCatalogProxyClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
    }

    @Override
    public List<McpServerCatalogEntry> getMcpServerCatalog() {
        McpServerCatalog mcpServerCatalog = restClient.get()
            .uri("/mcp-servers")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(McpServerCatalog.class);

        if (mcpServerCatalog == null || mcpServerCatalog.servers() == null) {
            throw new IllegalStateException("Empty response from the MCP server catalog proxy");
        }

        return mcpServerCatalog.servers();
    }
}
