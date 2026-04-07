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

package com.bytechef.ai.mcp.tool.automation.impl;

import com.bytechef.ai.mcp.tool.automation.api.WorkflowInfo;
import com.bytechef.ai.mcp.tool.config.ConditionalOnAiEnabled;
import com.bytechef.ai.mcp.tool.platform.exception.ScriptToolErrorType;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Marko Kriskovic
 */
@Component
@ConditionalOnAiEnabled
public class ClusterElementTools {
    private static final Logger logger = LoggerFactory.getLogger(ClusterElementTools.class);

    private final ProjectWorkflowFacade projectWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ClusterElementTools(ProjectWorkflowFacade projectWorkflowFacade) {
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    @Tool(
        description = "Update the clusterElements field of a specific task in the workflow definition. Returns the updated workflow")
    public WorkflowInfo updateClusterElementTask(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(description = "The name of the task whose clusterElements should be updated") String taskName,
        @ToolParam(description = "The cluster elements definition as a JSON string") String clusterElementsDefinition) {

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            String workflowDefinition = projectWorkflowDTO.getDefinition();

            if (!workflowDefinition.contains(taskName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Task '{}' not found in workflow '{}'", taskName, projectWorkflowDTO.getId());
                }

                return null;
            }

            JsonNode rootNode = JsonUtils.readTree(workflowDefinition);
            JsonNode tasksNode = rootNode.get("tasks");
            JsonNode clusterElementsNode = JsonUtils.readTree(clusterElementsDefinition);

            for (JsonNode taskNode : tasksNode) {
                if (taskName.equals(taskNode.get("name")
                    .stringValue())) {
                    ((ObjectNode) taskNode).set("clusterElements", clusterElementsNode);

                    break;
                }
            }

            String updatedWorkflowDefinition = JsonUtils.writeWithDefaultPrettyPrinter(rootNode);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), updatedWorkflowDefinition, projectWorkflowDTO.getVersion());

            if (logger.isDebugEnabled()) {
                logger.debug("Updated clusterElements for task '{}'", taskName);
            }

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), updatedWorkflowDefinition,
                projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate() != null ? projectWorkflowDTO.getCreatedDate() : null,
                projectWorkflowDTO.getLastModifiedDate() != null ? projectWorkflowDTO.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to update clusterElements for task '{}' in workflow {}", taskName, workflowId, e);

            throw new ExecutionException("Failed to update workflow: " + e.getMessage(), e,
                ScriptToolErrorType.UPDATE_SCRIPT);
        }
    }
}
