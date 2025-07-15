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

package com.bytechef.mcp.tool;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The ProjectWorkflowTools class provides utility methods and components to facilitate the management and execution of
 * project workflows.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectWorkflowTools {

    private static final Logger logger = LoggerFactory.getLogger(ProjectWorkflowTools.class);

    private final ProjectWorkflowService projectWorkflowService;

    public ProjectWorkflowTools(ProjectWorkflowService projectWorkflowService) {
        this.projectWorkflowService = projectWorkflowService;
    }

    @Tool(
        description = "Create a new workflow in a ByteChef project. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectTools.ProjectWorkflowInfo createProjectWorkflow(
        @ToolParam(description = "The ID of the project to add the workflow to") long projectId,
        @ToolParam(description = "The version of the project (typically 1 for new projects)") int projectVersion,
        @ToolParam(description = "The unique identifier for the workflow") String workflowId,
        @ToolParam(description = "The reference code for the workflow (optional)") String workflowReferenceCode) {

        try {
            ProjectWorkflow projectWorkflow;

            if (workflowReferenceCode != null) {
                workflowReferenceCode = workflowReferenceCode.trim();
            }

            if (workflowReferenceCode != null && !workflowReferenceCode.isEmpty()) {
                projectWorkflow = projectWorkflowService.addWorkflow(
                    projectId, projectVersion, workflowId, workflowReferenceCode);
            } else {
                projectWorkflow = projectWorkflowService.addWorkflow(projectId, projectVersion, workflowId);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Created workflow {} for project {} version {}", workflowId, projectId, projectVersion);
            }

            return new ProjectTools.ProjectWorkflowInfo(
                projectWorkflow.getId(), projectWorkflow.getProjectId(), projectWorkflow.getProjectVersion(),
                projectWorkflow.getWorkflowId(), projectWorkflow.getWorkflowReferenceCode(),
                projectWorkflow.getCreatedDate() != null ? projectWorkflow.getCreatedDate() : null,
                projectWorkflow.getLastModifiedDate() != null ? projectWorkflow.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error(
                "Failed to create workflow {} for project {} version {}", workflowId, projectId, projectVersion, e);

            throw new RuntimeException("Failed to create project workflow: " + e.getMessage(), e);
        }
    }
}
