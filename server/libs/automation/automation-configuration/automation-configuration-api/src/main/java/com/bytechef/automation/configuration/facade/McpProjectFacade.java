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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.McpProject;
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
}
