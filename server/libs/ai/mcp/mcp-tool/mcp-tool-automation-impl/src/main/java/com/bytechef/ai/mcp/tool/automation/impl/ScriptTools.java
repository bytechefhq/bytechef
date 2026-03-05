package com.bytechef.ai.mcp.tool.automation.impl;

import com.bytechef.ai.mcp.tool.automation.api.WorkflowInfo;
import com.bytechef.ai.mcp.tool.config.ConditionalOnAiEnabled;
import com.bytechef.ai.mcp.tool.platform.ComponentTools;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.commons.util.JsonUtils;
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
public class ScriptTools {
    private static final Logger logger = LoggerFactory.getLogger(ScriptTools.class);

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
                if (logger.isDebugEnabled()) {
                    logger.debug("Script component '{}' not found in workflow '{}'", scriptName, projectWorkflowDTO.getId());
                }

                return null;
            }

            JsonNode rootNode = JsonUtils.readTree(workflowDefinition);
            JsonNode tasksNode = rootNode.get("tasks");

            for (JsonNode taskNode : tasksNode) {
                if (scriptName.equals(taskNode.get("name").stringValue())) {
                    ObjectNode parametersNode = (ObjectNode) taskNode.get("parameters");

                    parametersNode.put("script", code);

                    break;
                }
            }

            String updatedWorkflowDefinition = JsonUtils.writeWithDefaultPrettyPrinter(rootNode);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), updatedWorkflowDefinition, projectWorkflowDTO.getVersion());

            if (logger.isDebugEnabled()) {
                logger.debug("Updated script component '{}'", scriptName);
            }

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), updatedWorkflowDefinition,
                projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate() != null ? projectWorkflowDTO.getCreatedDate() : null,
                projectWorkflowDTO.getLastModifiedDate() != null ? projectWorkflowDTO.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to update Script component in workflow {}", workflowId, e);

            throw new RuntimeException("Failed to update workflow: " + e.getMessage(), e);
        }
    }
}
