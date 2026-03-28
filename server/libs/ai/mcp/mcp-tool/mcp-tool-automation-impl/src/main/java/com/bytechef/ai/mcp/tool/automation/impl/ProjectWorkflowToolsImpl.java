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

import com.bytechef.ai.mcp.tool.automation.api.ProjectWorkflowInfo;
import com.bytechef.ai.mcp.tool.automation.api.ProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.api.WorkflowInfo;
import com.bytechef.ai.mcp.tool.automation.api.WorkflowValidationResult;
import com.bytechef.ai.mcp.tool.config.ConditionalOnAiEnabled;
import com.bytechef.ai.mcp.tool.platform.TaskTools;
import com.bytechef.ai.mcp.tool.platform.exception.ProjectWorkflowToolErrorType;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.validator.WorkflowValidator;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
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
 * @author Marko Kriskovic
 */
@Component
@ConditionalOnAiEnabled
public class ProjectWorkflowToolsImpl implements ProjectWorkflowTools {

    private static final Logger logger = LoggerFactory.getLogger(ProjectWorkflowToolsImpl.class);

    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final TaskTools taskTools;

    private static final String DEFAULT_DEFINITION = """
        {
            "label": "workflowName",
            "description": "workflowDescription",
            "inputs": [],
            "triggers": [
                {
                    "label": "Manual",
                    "name": "trigger_1",
                    "type": "manual/v1/manual"
                }
            ],
            "tasks": []
        }
        """;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowToolsImpl(ProjectWorkflowFacade projectWorkflowFacade, TaskTools taskTools) {
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.taskTools = taskTools;
    }

    @Override
    @SuppressFBWarnings("VA")
    @Tool(description = "Instructions for writing custom code in Script component")
    public String getScriptCodeInstructions() {
        return """
            ## Script Component Runtime Environment

            Scripts run inside a GraalVM Polyglot engine with a **synchronous, sandboxed** execution model.
            There is NO event loop, NO browser APIs, and NO Node.js APIs available.

            ## Entry Point

            The main function is `perform(input, context)` and must use this exact signature:
            - JavaScript: `function perform(input, context) { ... }`
            - Python: `def perform(input, context):`
            - Ruby: `def perform(input, context) ... end`

            The function MUST always return a value. It MUST be synchronous (not async).

            ## Parameters

            - `input` - holds all input parameters defined in the Script component's 'input' property.
              Parameters keep their original types. Access via `input.parameterName`.
            - `context` - provides access to ByteChef component actions. Use it to call any installed
              ByteChef component action:
              `context.component.componentName.actionName({'parameterName': parameterValue})`
              If the action requires a specific connection, pass the connection name as a second argument:
              `context.component.componentName.actionName({'parameterName': parameterValue}, 'connectionName')`
              ONLY use `context.component` when you need to invoke ByteChef component actions.

            ## Available Language Features

            Only standard language features from the respective language specifications are available:
            - JavaScript: Full ECMAScript (ES2024+) — variables, functions, classes, destructuring,
              template literals, Array/Object/String/Math/JSON/Date built-ins, regular expressions,
              Map, Set, Symbol, Proxy, Reflect, WeakMap, WeakSet, iterators, generators, Intl.
            - Python: Standard Python language features, built-in functions, data structures.
            - Ruby: Standard Ruby language features, built-in classes and methods.

            ## NOT Available (all languages)

            The following APIs and features are NOT available in the GraalVM runtime:
            - `fetch`, `XMLHttpRequest`, `Request`, `Response` — no HTTP client APIs
            - `async`, `await`, `Promise` — no asynchronous execution support
            - `setTimeout`, `setInterval`, `setImmediate` — no timers (no event loop)
            - `require`, `import` (ES modules) — no module system
            - File system APIs (`fs`, `File.open`, `open()`) — no file I/O
            - Network/socket APIs — no direct network access
            - Browser APIs (`window`, `document`, `localStorage`) — not a browser
            - Node.js APIs (`process`, `Buffer`, `child_process`) — not Node.js

            ## How to Perform Common Operations via ByteChef Components

            Since direct APIs are not available, use `context.component` to call ByteChef actions instead:

            ### HTTP Requests
            Use the `httpClient` component instead of fetch/XMLHttpRequest:
            - JavaScript:
              `var response = context.component.httpClient.get({uri: 'https://api.example.com/data'});`
            - Python:
              `response = context.component.httpClient.get({"uri": "https://api.example.com/data"})`
            - Ruby:
              `response = context.component.httpClient.get({"uri" => "https://api.example.com/data"})`

            Available HTTP methods: `get`, `post`, `put`, `patch`, `delete`, `head`.
            Common parameters: `uri` (required), `headers` (object with array values),
            `queryParameters` (object with array values), `bodyContent` (for POST/PUT/PATCH),
            `responseType` (default: JSON).

            ### Logging
            Use the `logger` component:
            `context.component.logger.info({text: 'Log message here'});`

            ### Other Operations
            Use `searchActions(query)` to discover available ByteChef component actions for any
            operation the user needs (e.g., sending emails, accessing databases, file operations).

            ## Code Style

            - If the language supports `;` at the end of the line, always add `;`.
            - Use `var` instead of `const`/`let` in JavaScript for maximum GraalVM compatibility.
            - Keep code synchronous — all `context.component` calls block and return results directly.
            """;
    }

    @Override
    @Tool(
        description = "Get comprehensive information about a specific workflow. Returns detailed project information including id, name, description, version, definition, project workflow id, created date, last modified date.")
    public WorkflowInfo getWorkflow(
        @ToolParam(description = "The ID of the workflow to retrieve") String workflowId) {

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved workflow {}", projectWorkflowDTO.getProjectWorkflowId());
            }

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), projectWorkflowDTO.getDefinition(),
                projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate() != null ? projectWorkflowDTO.getCreatedDate() : null,
                projectWorkflowDTO.getLastModifiedDate() != null ? projectWorkflowDTO.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to get workflow {}", workflowId, e);

            throw new ExecutionException("Failed to get workflow: " + e.getMessage(), e,
                ProjectWorkflowToolErrorType.GET_WORKFLOW);
        }
    }

    @Override
    @SuppressFBWarnings("VA")
    @Tool(description = "Instructions for building workflows")
    public String getWorkflowBuildInstructions() {
        return """
            The workflow needs to be in JSON format similar to:
            %s

            - Every workflow must only have one trigger, but as many actions or task dispatchers as needed

            Output properties:
            Some tasks have output properties that you get with getTaskOutputProperty() tool. Output property is a response to the task.
            For example: the action getMail will probably have the mail parameters as an output.
            You can reference the output property in a format similar to:
            ${taskName.property}
            ${taskName.arrayProperty[1]}
            ${taskName.objectProperty.property}}
            """
            .formatted(DEFAULT_DEFINITION);
    }

    @Override
    @Tool(
        description = "List all workflows in a project. Returns a list of workflows with their basic information including id, name and description")
    public List<WorkflowInfo> listWorkflows(
        @ToolParam(description = "The ID of the project") long projectId) {

        try {
            List<ProjectWorkflowDTO> workflows = projectWorkflowFacade.getProjectWorkflows(projectId);

            List<WorkflowInfo> workflowInfos = workflows.stream()
                .map(workflow -> new WorkflowInfo(workflow.getId(),
                    workflow.getProjectWorkflowId(),
                    workflow.getWorkflowUuid(),
                    workflow.getLabel(), workflow.getDescription(), workflow.getDefinition(), workflow.getVersion(),
                    workflow.getCreatedDate() != null ? workflow.getCreatedDate() : null,
                    workflow.getLastModifiedDate() != null ? workflow.getLastModifiedDate() : null))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} projects", workflowInfos.size());
            }

            return workflowInfos;
        } catch (Exception e) {
            logger.error("Failed to list project workflows", e);

            throw new ExecutionException("Failed to list project workflows: " + e.getMessage(), e,
                ProjectWorkflowToolErrorType.LIST_WORKFLOWS);
        }
    }

    @Override
    @Tool(
        description = "Full-text search across workflows in projects. Returns a list of workflows matching the search query in name or description.")
    public List<WorkflowInfo> searchWorkflows(
        @ToolParam(description = "The search query to match against workflow names and descriptions") String query,
        @ToolParam(required = false, description = "The ID of the project") Long projectId) {

        try {
            List<ProjectWorkflowDTO> allWorkflows =
                projectId != null ? projectWorkflowFacade.getProjectWorkflows(projectId)
                    : projectWorkflowFacade.getProjectWorkflows();

            String lowerQuery = StringUtils.trim(query.toLowerCase());

            List<WorkflowInfo> matchingWorkflow = allWorkflows.stream()
                .filter(workflow -> {
                    String name = workflow.getLabel();

                    name = name != null ? name.toLowerCase() : "";

                    String description = workflow.getDescription();

                    description = description != null ? description.toLowerCase() : "";

                    return name.contains(lowerQuery) || description.contains(lowerQuery);
                })
                .map(workflow -> new WorkflowInfo(
                    workflow.getId(), workflow.getProjectWorkflowId(), workflow.getWorkflowUuid(), workflow.getLabel(),
                    workflow.getDescription(), workflow.getDefinition(), workflow.getVersion(),
                    workflow.getCreatedDate() != null ? workflow.getCreatedDate() : null,
                    workflow.getLastModifiedDate() != null ? workflow.getLastModifiedDate() : null))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} workflows matching query '{}'", matchingWorkflow.size(), query);
            }

            return matchingWorkflow;
        } catch (Exception e) {
            logger.error("Failed to search workflows with query '{}'", query, e);
            throw new ExecutionException("Failed to search workflows: " + e.getMessage(), e,
                ProjectWorkflowToolErrorType.SEARCH_WORKFLOWS);
        }
    }

    @Override
    @Tool(
        description = "Validate a workflow configuration by checking its structure, properties and outputs against the task definitions. Returns validation results with any errors found")
    public WorkflowValidationResult validateWorkflow(
        @ToolParam(description = "The JSON string of the workflow to validate") String workflow) {

        try {
            StringBuilder errors = new StringBuilder("[");
            StringBuilder warnings = new StringBuilder("[");

            WorkflowValidator.validateWorkflow(
                workflow, this::getTaskProperties, this::getTaskOutputProperty, null, new HashMap<>(),
                new HashMap<>(), new HashMap<>(), errors, warnings);

            errors.append("]");

            String errorMessages = StringUtils.trim(errors.toString());

            boolean isValid = errorMessages.equals("[]");

            warnings.append("]");

            String warningMessages = StringUtils.trim(warnings.toString());

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Validated workflow. Valid: {}, Errors: {}, Warnings: {}", isValid, errorMessages, warningMessages);
            }

            return new WorkflowValidationResult(isValid, errorMessages, warningMessages);
        } catch (Exception e) {
            logger.error("Failed to validate workflow", e);

            throw new ExecutionException("Failed to validate workflow", e,
                ProjectWorkflowToolErrorType.VALIDATE_WORKFLOW);
        }
    }

    @Override
    @Tool(
        description = "Create a new workflow in a ByteChef project. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectWorkflowInfo createProjectWorkflow(
        @ToolParam(description = "The ID of the project to add the workflow to") long projectId,
        @ToolParam(
            description = "The definition for the workflow. Needs to be in JSON format similar to "
                + DEFAULT_DEFINITION) String definition) {

        try {
            ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(projectId, definition);

            if (logger.isDebugEnabled()) {
                logger.debug("Created workflow {} for project {}", definition, projectId);
            }

            return new ProjectWorkflowInfo(
                projectWorkflow.getId(), projectWorkflow.getProjectId(), projectWorkflow.getProjectVersion(),
                projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString(),
                projectWorkflow.getCreatedDate() != null ? projectWorkflow.getCreatedDate() : null,
                projectWorkflow.getLastModifiedDate() != null ? projectWorkflow.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error(
                "Failed to create workflow {} for project {}", definition, projectId, e);

            throw new ExecutionException("Failed to create project workflow: " + e.getMessage(), e,
                ProjectWorkflowToolErrorType.CREATE_WORKFLOW);
        }
    }

    @Override
    @Tool(description = "Delete a workflow. Returns a confirmation message.")
    public String deleteWorkflow(
        @ToolParam(description = "The ID of the workflow to delete") String workflowId) {
        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            String workflowName = projectWorkflowDTO.getLabel();

            projectWorkflowFacade.deleteWorkflow(projectWorkflowDTO.getId());

            if (logger.isDebugEnabled()) {
                logger.debug("Deleted workflow {} with name '{}'", workflowId, workflowName);
            }

            return "Workflow '" + workflowName + "' (ID: " + workflowId + ") has been successfully deleted.";
        } catch (Exception e) {
            logger.error("Failed to delete workflow {}", workflowId, e);

            throw new ExecutionException("Failed to delete workflow: " + e.getMessage(), e,
                ProjectWorkflowToolErrorType.DELETE_WORKFLOW);
        }
    }

    @Override
    @Tool(
        description = "Update the workflow definition. Returns the updated workflow id, name and definition.")
    public WorkflowInfo updateWorkflow(
        @ToolParam(description = "The ID of the workflow to update") String workflowId,
        @ToolParam(
            description = "The new definition of the workflow. Needs to be in JSON format similar to " +
                DEFAULT_DEFINITION) String definition) {

        try {
            ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(workflowId);

            projectWorkflowFacade.updateWorkflow(
                projectWorkflowDTO.getId(), definition, projectWorkflowDTO.getVersion());

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Updated workflow {} with name '{}'", projectWorkflowDTO.getId(), projectWorkflowDTO.getLabel());
            }

            return new WorkflowInfo(
                projectWorkflowDTO.getId(), projectWorkflowDTO.getProjectWorkflowId(),
                projectWorkflowDTO.getWorkflowUuid(), projectWorkflowDTO.getLabel(),
                projectWorkflowDTO.getDescription(), definition,
                projectWorkflowDTO.getVersion(),
                projectWorkflowDTO.getCreatedDate() != null ? projectWorkflowDTO.getCreatedDate() : null,
                projectWorkflowDTO.getLastModifiedDate() != null ? projectWorkflowDTO.getLastModifiedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to update workflow {}", workflowId, e);

            throw new ExecutionException("Failed to update workflow: " + e.getMessage(), e,
                ProjectWorkflowToolErrorType.UPDATE_WORKFLOW);
        }
    }

    protected PropertyInfo getTaskOutputProperty(String type, String taskType, StringBuilder warnings) {
        String[] split = type.split("/");

        int version = Integer.parseInt(split[1].substring(1));

        try {
            if (split.length == 2) {
                return taskTools.getTaskOutputProperty("taskDispatcher", split[0], split[0], version);
            } else if (taskType.equals("trigger")) {
                return taskTools.getTaskOutputProperty(taskType, split[2], split[0], version);
            } else {
                return taskTools.getTaskOutputProperty("action", split[2], split[0], version);
            }
        } catch (Exception e) {
            warnings.append(e.getMessage());

            return null;
        }
    }

    protected List<PropertyInfo> getTaskProperties(String type, String taskType) {
        String[] split = type.split("/");

        int version = Integer.parseInt(split[1].substring(1));

        if (split.length == 2) {
            return taskTools.getTaskProperties("taskDispatcher", split[0], split[0], version);
        } else if (taskType.equals("trigger")) {
            return taskTools.getTaskProperties(taskType, split[2], split[0], version);
        } else {
            return taskTools.getTaskProperties("action", split[2], split[0], version);
        }
    }
}
