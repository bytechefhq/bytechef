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

package com.bytechef.platform.security.web.mcp;

import java.util.Optional;

/**
 * An MCP server endpoint that accepts OAuth2 access tokens. Each module serving an MCP server contributes one, so the
 * platform security and authorization server layers never hardcode the paths, scopes or tenant rules of a particular
 * MCP server.
 *
 * @author Ivica Cardic
 */
public interface McpEndpoint {

    /**
     * @param path the request path within the application, without the context path and query
     * @return whether this MCP server serves the path
     */
    boolean matches(String path);

    /**
     * @return the OAuth2 scope an access token must carry to call this MCP server
     */
    String getScope();

    /**
     * Resolves the tenant a matched path addresses.
     *
     * @param path the request path within the application, without the context path and query
     * @return the tenant named by the path, or empty when the path names none and the tenant comes from the access
     *         token
     * @throws IllegalArgumentException when the path carries no valid tenant key
     */
    Optional<String> resolveTenantId(String path);

    /**
     * Resolves a human-readable name for an MCP server resource URL, shown when listing OAuth2 grants.
     *
     * @return the name, or empty when the resource URL does not belong to this MCP server
     */
    Optional<String> resolveResourceName(String resource);
}
