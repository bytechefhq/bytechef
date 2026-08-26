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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

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
    McpServer create(String name, PlatformType type, Environment environment, Boolean enabled);

    /**
     * Deletes an MCP server by ID.
     *
     * @param mcpServerId the ID of the MCP server to delete
     */
    void delete(long mcpServerId);

    /**
     * Reports whether an MCP server named {@code name} already exists in {@code environment}.
     *
     * <p>
     * <b>The scope of this check is the {@code (name, environment)} unique constraint on {@code mcp_server}, and
     * nothing narrower.</b> That constraint spans every workspace and both {@link PlatformType}s, so a {@code true}
     * here may be caused by a server the caller cannot see, or by an {@code EMBEDDED} server when the caller is
     * creating an {@code AUTOMATION} one. It is deliberately NOT workspace-scoped: a workspace-scoped answer would
     * report {@code false} for names that still cannot be inserted.
     * </p>
     *
     * @param name        the server name to test
     * @param environment the environment to test it in
     * @param excludeUuid a cross-environment lineage identifier to exclude from the answer, so a caller re-promoting a
     *                    server does not see its own counterpart as a conflict; {@code null} excludes nothing
     * @return {@code true} when a server of that name already exists in that environment
     */
    boolean existsByNameAndEnvironment(String name, Environment environment, @Nullable UUID excludeUuid);

    /**
     * Retrieves an MCP server by its unique identifier.
     *
     * @param mcpServerId the unique identifier of the MCP server to retrieve
     * @return the MCP server with the specified ID
     */
    McpServer getMcpServer(long mcpServerId);

    /**
     * Returns the MCP server's secret key. The secret authenticates inbound MCP traffic, so this is restricted to
     * tenant admins (a workspace member who can otherwise view the server must not read the secret).
     *
     * @param mcpServerId the unique identifier of the MCP server
     * @return the secret key
     */
    String getMcpServerSecretKey(long mcpServerId);

    /**
     * Retrieves an MCP server by its secret key.
     *
     * @param secretKey the secret key used to identify the MCP server
     * @return the MCP server associated with the specified secret key
     */
    McpServer getMcpServer(String secretKey);

    /**
     * Retrieves an MCP server by its cross-environment lineage identifier and environment.
     *
     * @param uuid        the cross-environment lineage identifier shared by the server's counterparts in other
     *                    environments
     * @param environment the environment to look the server up in
     * @return the MCP server matching the given uuid and environment, if one exists
     */
    Optional<McpServer> fetchMcpServer(UUID uuid, Environment environment);

    /**
     * Gets MCP servers filtered by type.
     *
     * @param type the type to filter by
     * @return a list of MCP servers with the given type
     */
    List<McpServer> getMcpServers(PlatformType type);

    /**
     * Gets MCP servers filtered by type with ordering.
     *
     * @param type    the type to filter by
     * @param orderBy the ordering criteria (can be null for default ordering)
     * @return a list of MCP servers with the given type, ordered as specified
     */
    List<McpServer> getMcpServers(PlatformType type, McpServerOrderBy orderBy);

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
     * <p>
     * When {@code enabled} is {@code true}, every registered {@link McpServerEnablementValidator} is asked to validate
     * the server first; a validator that throws prevents the server from being enabled. Disabling a server (or leaving
     * {@code enabled} unset) never invokes the validators — taking a broken server offline must always succeed.
     * </p>
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
