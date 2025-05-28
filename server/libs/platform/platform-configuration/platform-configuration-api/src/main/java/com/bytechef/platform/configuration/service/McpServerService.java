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

import com.bytechef.platform.configuration.domain.McpServer;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link McpServer} entities.
 *
 * @author Ivica Cardic
 */
public interface McpServerService {

    /**
     * Creates a new MCP server.
     *
     * @param mcpServer the MCP server to create
     * @return the created MCP server
     */
    McpServer create(McpServer mcpServer);

    /**
     * Updates an existing MCP server.
     *
     * @param mcpServer the MCP server to update
     * @return the updated MCP server
     */
    McpServer update(McpServer mcpServer);

    /**
     * Deletes an MCP server by ID.
     *
     * @param mcpServerId the ID of the MCP server to delete
     */
    void delete(long mcpServerId);

    /**
     * Fetches an MCP server by ID.
     *
     * @param mcpServerId the ID of the MCP server to fetch
     * @return the MCP server, or empty if not found
     */
    Optional<McpServer> fetchMcpServer(long mcpServerId);

    /**
     * Gets all MCP servers.
     *
     * @return a list of all MCP servers
     */
    List<McpServer> getMcpServers();

    /**
     * Creates a new MCP server from the provided input parameters.
     *
     * @param name        the name of the server
     * @param type        the type of the server
     * @param environment the environment of the server
     * @return the created MCP server
     */
    McpServer createFromInput(String name, ModeType type, Environment environment);

    /**
     * Updates an existing MCP server with the provided input parameters.
     *
     * @param id          the ID of the MCP server to update
     * @param name        the name of the server (can be null if not updating)
     * @param type        the type of the server (can be null if not updating)
     * @param environment the environment of the server (can be null if not updating)
     * @return the updated MCP server
     * @throws IllegalArgumentException if the MCP server with the given ID is not found
     */
    McpServer updateFromInput(long id, String name, ModeType type, Environment environment);

    /**
     * Updates the tags of an MCP server.
     *
     * @param id     the ID of the MCP server to update
     * @param tagIds the list of tag IDs to set
     * @return the updated MCP server
     * @throws IllegalArgumentException if the MCP server with the given ID is not found
     */
    McpServer updateTags(long id, List<Long> tagIds);

    /**
     * Gets MCP servers filtered by tag ID.
     *
     * @param tagId the tag ID to filter by
     * @return a list of MCP servers with the given tag
     */
    List<McpServer> getMcpServersByTagId(Long tagId);
}
