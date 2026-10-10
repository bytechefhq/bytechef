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
public class ScriptTools {
    private static final Logger log = LoggerFactory.getLogger(ScriptTools.class);

    private final ProjectWorkflowFacade projectWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ScriptTools(ProjectWorkflowFacade projectWorkflowFacade) {
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    @Tool(
        description = "Update only the script component in the workflow definition. Returns the updated workflow")
    public WorkflowInfo updateScriptComponentCode(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(description = "The new code") String code,
        @ToolParam(description = "The name of the script node") String scriptName) {

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            String workflowDefinition = projectWorkflowDTO.getDefinition();

            if (!workflowDefinition.contains(scriptName)) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "updateScriptComponentCode({}, {}): Script component '{}' not found in workflow '{}'",
                        workflowId, scriptName, scriptName, projectWorkflowDTO.getId());
                }

                return null;
            }

            JsonNode rootNode = JsonUtils.readTree(workflowDefinition);
            JsonNode tasksNode = rootNode.get("tasks");

            for (JsonNode taskNode : tasksNode) {
                if (scriptName.equals(taskNode.get("name")
                    .stringValue())) {
                    ObjectNode parametersNode = (ObjectNode) taskNode.get("parameters");

                    parametersNode.put("script", code);

                    break;
                }
            }

            String updatedWorkflowDefinition = JsonUtils.writeWithDefaultPrettyPrinter(rootNode);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), updatedWorkflowDefinition, projectWorkflowDTO.getVersion());

            if (log.isDebugEnabled()) {
                log.debug(
                    "updateScriptComponentCode({}, {}): Updated script component '{}'", workflowId, scriptName,
                    scriptName);
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
                "updateScriptComponentCode({}, {}): Failed to update Script component in workflow {}", workflowId,
                scriptName, workflowId, e);

            throw new ExecutionException(
                "Failed to update workflow: " + e.getMessage(), e, ScriptToolErrorType.UPDATE_SCRIPT);
        }
    }
}
