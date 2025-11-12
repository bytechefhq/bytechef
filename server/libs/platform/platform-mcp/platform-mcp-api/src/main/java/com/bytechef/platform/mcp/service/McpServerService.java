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

package com.bytechef.platform.mcp.service;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;

/**
 * Service interface for managing MCP servers within the system. Provides methods to create, update, delete, and query
 * MCP servers.
 */
public interface McpServerService {

    /**
     * Enum for ordering MCP servers.
     */
    enum McpServerOrderBy {

        NAME_ASC,
        NAME_DESC,
        CREATED_DATE_ASC,
        CREATED_DATE_DESC,
        LAST_MODIFIED_DATE_ASC,
        LAST_MODIFIED_DATE_DESC
    }

    /**
     * Creates a new MCP server.
     *
     * @param mcpServer the MCP server to create
     * @return the created MCP server
     */
    McpServer create(McpServer mcpServer);

    /**
     * Creates a new MCP server from the provided input parameters.
     *
     * @param name        the name of the server
     * @param type        the type of the server
     * @param environment the environment of the server
     * @param enabled     whether the server is enabled (can be null for default value)
     * @return the created MCP server
     */
    McpServer create(String name, ModeType type, Environment environment, Boolean enabled);

    /**
     * Deletes an MCP server by ID.
     *
     * @param mcpServerId the ID of the MCP server to delete
     */
    void delete(long mcpServerId);

    /**
     * Retrieves an MCP server by its unique identifier.
     *
     * @param mcpServerId the unique identifier of the MCP server to retrieve
     * @return the MCP server with the specified ID
     */
    McpServer getMcpServer(long mcpServerId);

    /**
     * Retrieves an MCP server by its secret key.
     *
     * @param secretKey the secret key used to identify the MCP server
     * @return the MCP server associated with the specified secret key
     */
    McpServer getMcpServer(String secretKey);

    /**
     * Gets MCP servers filtered by type.
     *
     * @param type the type to filter by
     * @return a list of MCP servers with the given type
     */
    List<McpServer> getMcpServers(ModeType type);

    /**
     * Gets MCP servers filtered by type with ordering.
     *
     * @param type    the type to filter by
     * @param orderBy the ordering criteria (can be null for default ordering)
     * @return a list of MCP servers with the given type, ordered as specified
     */
    List<McpServer> getMcpServers(ModeType type, McpServerOrderBy orderBy);

    /**
     * Updates an existing MCP server.
     *
     * @param mcpServer the MCP server to update
     * @return the updated MCP server
     */
    McpServer update(McpServer mcpServer);

    /**
     * Updates an existing MCP server with the provided input parameters.
     *
     * @param id      the ID of the MCP server to update
     * @param name    the name of the server (can be null if not updating)
     * @param enabled whether the server is enabled (can be null if not updating)
     * @return the updated MCP server
     * @throws IllegalArgumentException if the MCP server with the given ID is not found
     */
    McpServer update(long id, String name, Boolean enabled);

    /**
     * Updates the tags of an MCP server.
     *
     * @param id     the ID of the MCP server to update
     * @param tagIds the list of tag IDs to set
     * @return the updated MCP server
     * @throws IllegalArgumentException if the MCP server with the given ID is not found
     */
    McpServer updateTags(long id, List<Long> tagIds);
}
