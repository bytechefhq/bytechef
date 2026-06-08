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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.ai.tool.exception.ScriptToolErrorType;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
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
public class ClusterElementTools {

    private static final Logger log = LoggerFactory.getLogger(ClusterElementTools.class);

    private final ProjectWorkflowFacade projectWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ClusterElementTools(ProjectWorkflowFacade projectWorkflowFacade) {
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    @Tool(
        description = "Update root-level properties of a workflow definition (e.g. label, description, inputs, outputs). "
            +
            "Accepts a JSON object whose keys are merged into the workflow root. Returns the updated workflow.")
    public WorkflowInfo updateWorkflowRootProperties(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(
            description = "A JSON object containing the root-level properties to set, e.g. " +
                "{\"label\": \"My Workflow\", \"description\": \"Does X\"}") String rootPropertiesDefinition) {

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            JsonNode rootPropertiesNode = JsonUtils.readTree(rootPropertiesDefinition);
            JsonNode rootNode = JsonUtils.readTree(projectWorkflowDTO.getDefinition());

            ((ObjectNode) rootNode).setAll((ObjectNode) rootPropertiesNode);

            String updatedWorkflowDefinition = JsonUtils.writeWithDefaultPrettyPrinter(rootNode);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), updatedWorkflowDefinition, projectWorkflowDTO.getVersion());

            if (log.isDebugEnabled()) {
                log.debug("updateWorkflowRootProperties({}): Updated root properties", workflowId);
            }

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), updatedWorkflowDefinition,
                projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate() != null ? projectWorkflowDTO.getCreatedDate() : null,
                projectWorkflowDTO.getLastModifiedDate() != null ? projectWorkflowDTO.getLastModifiedDate() : null);
        } catch (Exception e) {
            log.error("updateWorkflowRootProperties({}): Failed to update root properties in workflow {}",
                workflowId, workflowId, e);

            throw new ExecutionException(
                "Failed to update workflow root properties: " + e.getMessage(), e,
                ScriptToolErrorType.UPDATE_WORKFLOW_ROOT);
        }
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
                if (log.isDebugEnabled()) {
                    log.debug(
                        "updateClusterElementTask({}, {}): Task '{}' not found in workflow '{}'", workflowId, taskName,
                        taskName, projectWorkflowDTO.getId());
                }

                return null;
            }

            JsonNode clusterElementsNode = JsonUtils.readTree(clusterElementsDefinition);
            JsonNode rootNode = JsonUtils.readTree(workflowDefinition);

            JsonNode tasksNode = rootNode.get("tasks");

            for (JsonNode taskNode : tasksNode) {
                JsonNode name = taskNode.get("name");

                if (taskName.equals(name.stringValue())) {
                    ((ObjectNode) taskNode).set("clusterElements", clusterElementsNode);

                    break;
                }
            }

            String updatedWorkflowDefinition = JsonUtils.writeWithDefaultPrettyPrinter(rootNode);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), updatedWorkflowDefinition, projectWorkflowDTO.getVersion());

            if (log.isDebugEnabled()) {
                log.debug(
                    "updateClusterElementTask({}, {}): Updated clusterElements for task '{}'", workflowId, taskName,
                    taskName);
            }

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), updatedWorkflowDefinition,
                projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate() != null ? projectWorkflowDTO.getCreatedDate() : null,
                projectWorkflowDTO.getLastModifiedDate() != null ? projectWorkflowDTO.getLastModifiedDate() : null);
        } catch (Exception e) {
            log.error(
                "updateClusterElementTask({}, {}): Failed to update clusterElements for task '{}' in workflow {}",
                workflowId, taskName, taskName, workflowId, e);

            throw new ExecutionException(
                "Failed to update workflow: " + e.getMessage(), e, ScriptToolErrorType.UPDATE_SCRIPT);
        }
    }
}
