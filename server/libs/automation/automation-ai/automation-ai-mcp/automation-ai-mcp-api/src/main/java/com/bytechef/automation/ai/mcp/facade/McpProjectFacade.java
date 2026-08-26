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

package com.bytechef.automation.ai.mcp.facade;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import java.util.List;

/**
 * Facade for managing MCP Project operations that involve multiple services.
 *
 * @author Ivica Cardic
 */
public interface McpProjectFacade {

    /**
     * Creates a new MCP project with workflows and corresponding ProjectDeployment and ProjectDeploymentWorkflows.
     *
     * @param mcpServerId         the MCP server ID
     * @param projectId           the project ID
     * @param projectVersion      the project version
     * @param selectedWorkflowIds the list of selected workflow IDs
     * @return the created MCP project
     */
    McpProject createMcpProject(
        long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds);

    /**
     * Deletes an MCP project identified by its unique ID.
     *
     * @param mcpProjectId the unique identifier of the MCP project to be deleted
     */
    void deleteMcpProject(long mcpProjectId);

    /**
     * Updates the selected workflows of an MCP project by computing a diff against the current state. New workflows are
     * created, deselected workflows are removed, unchanged workflows are preserved.
     *
     * @param mcpProjectId        the unique identifier of the MCP project to update
     * @param selectedWorkflowIds the new list of selected workflow IDs
     * @return the updated MCP project
     */
    McpProject updateMcpProject(long mcpProjectId, List<String> selectedWorkflowIds);

    /**
     * Clones an existing MCP project onto the supplied {@code targetMcpServerId}, reusing the source's projectId,
     * projectVersion, and exposed workflow set. Internally this is equivalent to
     * {@link #createMcpProject(long, long, int, List)} with the same inputs, so a fresh
     * {@link com.bytechef.automation.configuration.domain.ProjectDeployment} is provisioned for the clone — both source
     * and clone reference the same project version but each has its own deployment row.
     *
     * <p>
     * The clone's deployment inherits the TARGET server's environment, so cloning onto a server in another environment
     * does move the project across environments (without connection re-binding — use environment promotion for that).
     * The useful axis is the MCP server: an MCP server is per-tenant scoped, so duplicating an MCP project onto a
     * different server lets the user expose the same workflow set under different MCP authority/auth without having to
     * hand-rebuild the project + workflow bindings.
     * </p>
     *
     * @param mcpProjectId      id of the source MCP project (must exist)
     * @param targetMcpServerId destination MCP server (may equal the source's server for an in-place duplicate)
     * @return the persisted clone
     */
    McpProject cloneMcpProject(long mcpProjectId, long targetMcpServerId);
}
