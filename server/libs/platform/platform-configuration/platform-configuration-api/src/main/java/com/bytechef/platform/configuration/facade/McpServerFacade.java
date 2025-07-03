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

package com.bytechef.platform.configuration.facade;

import com.bytechef.platform.configuration.domain.McpComponent;
import com.bytechef.platform.configuration.domain.McpServer;
import com.bytechef.platform.configuration.domain.McpTool;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * Facade for managing MCP Server operations that involve multiple services.
 *
 * @author Ivica Cardic
 */
public interface McpServerFacade {

    /**
     * Creates a new MCP component with associated tools.
     *
     * @param mcpComponent the MCP component to create
     * @param mcpTools     the list of MCP tools to create for this component
     * @return the created MCP component
     */
    McpComponent create(McpComponent mcpComponent, List<McpTool> mcpTools);

    /**
     * Deletes the MCP component identified by the specified ID.
     *
     * @param mcpComponentId the unique identifier of the MCP component to be deleted
     */
    void deleteMcpComponent(long mcpComponentId);

    /**
     * Deletes the MCP server identified by the specified ID.
     *
     * @param mcpServerId the unique identifier of the MCP server to be deleted
     */
    void deleteMcpServer(long mcpServerId);

    /**
     * Gets MCP components for multiple MCP servers in batch.
     *
     * @param mcpServers the list of MCP servers
     * @return a map of MCP servers to their components
     */
    Map<McpServer, List<McpComponent>> getMcpServerMcpComponents(List<McpServer> mcpServers);

    /**
     * Gets tags for multiple MCP servers in batch.
     *
     * @param mcpServers the list of MCP servers
     * @return a map of MCP servers to their tags
     */
    Map<McpServer, List<Tag>> getMcpServerTags(List<McpServer> mcpServers);

    /**
     * Updates an existing MCP component with associated tools.
     *
     * @param mcpComponent the MCP component to update (must have ID set)
     * @param mcpTools     the list of MCP tools to replace existing tools
     * @return the updated MCP component
     */
    McpComponent update(McpComponent mcpComponent, List<McpTool> mcpTools);

    /**
     * Updates MCP server tags, handling tag validation and persistence.
     *
     * @param id   the MCP server ID
     * @param tags the list of tags to update
     * @return the validated and saved tags
     */
    List<Tag> updateMcpServerTags(long id, List<Tag> tags);

}
