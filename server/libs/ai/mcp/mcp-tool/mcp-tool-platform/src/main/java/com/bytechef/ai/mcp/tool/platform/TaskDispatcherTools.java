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

package com.bytechef.ai.mcp.tool.platform;

import com.bytechef.ai.mcp.tool.config.ConditionalOnAiEnabled;
import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component
@ConditionalOnAiEnabled
public class TaskDispatcherTools {

    private static final Logger logger = LoggerFactory.getLogger(TaskDispatcherTools.class);

    private static final String FAILED_TO_LIST_TASK_DISPATCHERS = "Failed to list task dispatchers";
    private static final String FAILED_TO_GET_TASK_DISPATCHER = "Failed to get task dispatcher";
    private static final String FAILED_TO_SEARCH_TASK_DISPATCHERS = "Failed to search task dispatchers";

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    private static final String DEFAULT_TASK_DISPATCHER_DEFINITION = """
        {
            "label": "Task Dispatcher Task",
            "name": "{taskDispatcherName}_1",
            "type": "{taskDispatcherName}/v{taskDispatcherVersion}",
            "parameters": {}
        }
        """;

    @SuppressFBWarnings("EI")
    public TaskDispatcherTools(TaskDispatcherDefinitionService taskDispatcherDefinitionService) {
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @Tool(
        description = "List all task dispatchers in the project. Returns a list of task dispatchers with their basic information including name and description")
    public List<TaskDispatcherMinimalInfo> listTaskDispatchers() {
        try {
            List<TaskDispatcherDefinition> taskDispatcherDefinitions =
                taskDispatcherDefinitionService.getTaskDispatcherDefinitions();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} task dispatchers", taskDispatcherDefinitions.size());
            }

            return taskDispatcherDefinitions.stream()
                .map(taskDispatcherDefinition -> new TaskDispatcherMinimalInfo(
                    taskDispatcherDefinition.getName(), taskDispatcherDefinition.getDescription(),
                    taskDispatcherDefinition.getVersion()))
                .toList();
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_TASK_DISPATCHERS, e);

            throw new RuntimeException(FAILED_TO_LIST_TASK_DISPATCHERS, e);
        }
    }

    @Tool(
        description = "Get comprehensive information about a specific task dispatcher. Returns detailed task dispatcher information including: name, description, and properties")
    public TaskDispatcherInfo getTaskDispatcher(
        @ToolParam(description = "The name of the task dispatcher to retrieve") String name,
        @ToolParam(required = false, description = "The version of the task dispatcher") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition =
                taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version);

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved task dispatcher {}", name);
            }

            List<Property> properties = new ArrayList<>(taskDispatcherDefinition.getProperties());

            properties.addAll(taskDispatcherDefinition.getTaskProperties());

            String propertiesJSON = ToolUtils.generateParametersJson(properties);
            String type = taskDispatcherDefinition.getName() + "/v" + taskDispatcherDefinition.getVersion();

            String outputPropertiesJson = null;
            OutputResponse outputResponse =
                taskDispatcherDefinition.isOutputDefined() ? taskDispatcherDefinition.getOutputResponse() : null;

            if (outputResponse != null && outputResponse.outputSchema() != null) {
                outputPropertiesJson = ToolUtils.generateOutputPropertiesJson(outputResponse.outputSchema());
            }

            return new TaskDispatcherInfo(
                taskDispatcherDefinition.getName(), taskDispatcherDefinition.getDescription(),
                taskDispatcherDefinition.getTitle(), type, propertiesJSON, outputPropertiesJson);
        } catch (Exception e) {
            logger.error("Failed to get task dispatcher {}", name, e);

            throw new RuntimeException(FAILED_TO_GET_TASK_DISPATCHER, e);
        }
    }

    @Tool(
        description = "Get the output property of a specific task dispatcher. Returns the structure of the output property")
    public PropertyInfo getTaskDispatcherOutput(
        @ToolParam(description = "The name of the task dispatcher to retrieve output properties for") String name,
        @ToolParam(required = false, description = "The version of the task dispatcher") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition =
                taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version);

            // fix it
            OutputResponse outputResponse = null;

            if (taskDispatcherDefinition.isOutputDefined()) {
                if (taskDispatcherDefinition.isOutputSchemaDefined()) {
                    outputResponse = taskDispatcherDefinition.getOutputResponse();
                } else if (taskDispatcherDefinition.isOutputFunctionDefined()) {
                    outputResponse = taskDispatcherDefinitionService.executeOutput(taskDispatcherDefinition.getName(),
                        taskDispatcherDefinition.getVersion(), Map.of());
                }
            }

            if (outputResponse == null || outputResponse.outputSchema() == null) {
                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved output properties for task dispatcher '{}'", name);
            }

            return ToolUtils.convertToPropertyInfo(outputResponse.outputSchema());
        } catch (Exception e) {
            logger.error("Failed to get output properties for task dispatcher '{}'", name, e);

            throw new RuntimeException("Failed to get output properties", e);
        }
    }

    @Tool(
        description = "Get all properties of a specific task dispatcher. Returns a hierarchical list of properties including nested properties")
    public List<PropertyInfo> getTaskDispatcherProperties(
        @ToolParam(description = "The name of the task dispatcher to retrieve properties for") String name,
        @ToolParam(required = false, description = "The version of the task dispatcher") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition =
                taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version);

            List<? extends BaseProperty> properties = taskDispatcherDefinition.getProperties();
            List<? extends BaseProperty> taskProperties = taskDispatcherDefinition.getTaskProperties();

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved {} properties for task dispatcher '{}'", properties.size(), name);
                logger.debug("Retrieved {} task properties for task dispatcher '{}'", taskProperties.size(), name);
            }

            List<PropertyInfo> propertyInfos =
                new ArrayList<>(ToolUtils.convertToPropertyInfoList(properties));
            propertyInfos.addAll(ToolUtils.convertToPropertyInfoList(taskProperties));

            return propertyInfos;
        } catch (Exception e) {
            logger.error("Failed to get properties for task dispatcher '{}'", name, e);

            throw new RuntimeException("Failed to get properties", e);
        }
    }

    @Tool(
        description = "Full-text search across task dispatchers. Returns a list of task dispatchers matching the search query in name or description.")
    public List<TaskDispatcherMinimalInfo> searchTaskDispatchers(
        @ToolParam(
            description = "The search query to match against task dispatcher names and descriptions") String query) {

        try {
            List<TaskDispatcherDefinition> taskDispatcherDefinitions =
                taskDispatcherDefinitionService.getTaskDispatcherDefinitions();
            String lowerQuery = query.toLowerCase()
                .trim();

            List<TaskDispatcherMinimalInfo> matchingTaskDispatchers = taskDispatcherDefinitions.stream()
                .filter(taskDispatcherDefinition -> ToolUtils.matchesQuery(
                    taskDispatcherDefinition.getName(), taskDispatcherDefinition.getDescription(), null, null,
                    lowerQuery))
                .map(taskDispatcherDefinition -> new TaskDispatcherMinimalInfo(
                    taskDispatcherDefinition.getName(), taskDispatcherDefinition.getDescription(),
                    taskDispatcherDefinition.getVersion()))
                .sorted((td1, td2) -> ToolUtils.compareTasks(
                    td1.name(), td1.description(), null, td2.name(), td2.description(), null, lowerQuery))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} task dispatchers matching query '{}'", matchingTaskDispatchers.size(), query);
            }

            return matchingTaskDispatchers;
        } catch (Exception e) {
            logger.error("Failed to search task dispatchers with query '{}'", query, e);

            throw new RuntimeException(FAILED_TO_SEARCH_TASK_DISPATCHERS, e);
        }
    }

    @Tool(
        description = "Get the task dispatcher definition template for a specific task dispatcher. Returns a structured JSON with template and conditional parameters")
    public String getTaskDispatcherDefinition(
        @ToolParam(description = "The name of the task dispatcher to generate definition for") String name,
        @ToolParam(required = false, description = "The version of the task dispatcher") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition =
                taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version);

            List<Property> properties = new ArrayList<>(taskDispatcherDefinition.getProperties());

            properties.addAll(taskDispatcherDefinition.getTaskProperties());

            // Extract properties for the task dispatche definition
            String parametersJson = ToolUtils.generateParametersJson(properties);

            // Generate the task dispatche definition using the template with actual parameters
            String taskDispatcherDefinitionString = DEFAULT_TASK_DISPATCHER_DEFINITION
                .replace("{taskDispatcherName}", name)
                .replace("{taskDispatcherVersion}", String.valueOf(taskDispatcherDefinition.getVersion()))
                .replace("\"parameters\": {}", "\"parameters\": " + parametersJson);

            if (logger.isDebugEnabled()) {
                logger.debug("Generated task dispatcher definition for {}", name);
            }

            return taskDispatcherDefinitionString;
        } catch (Exception e) {
            logger.error("Failed to generate task dispatcher definition for '{}'", name, e);

            throw new RuntimeException("Failed to generate task dispatcher definition", e);
        }
    }

    public String getTaskDispatcherInstructions(String taskDispatcher) {
        return switch (taskDispatcher) {
            case "condition" ->
                """
                    If 'rawExpression' is true, then fill out 'expression', if false fill out 'conditions'.
                    'expression' is the raw expression that will be evaluated.
                    Example:
                    "expression": "${taskName.numberProperty} >= 7 && !contains({'EMPTY','REGEX'}, ${taskName.stringProperty}) || ${taskName.booleanProperty} != false"

                    'conditions' have 2 nested arrays:
                    - the first array is for OR conditions
                    - the second array is for AND conditions
                    Example:
                    "conditions": [
                      [
                        {
                          "type": "number",
                          "value1": "${taskName.numberProperty}",
                          "operation": "GREATER_EQUALS",
                          "value2": "7"
                        },
                        {
                          "type": "string",
                          "value1": "${taskName.stringProperty}",
                          "operation": "NOT_CONTAINS",
                          "value2": "EMPTY"
                        },
                        {
                          "type": "string",
                          "value1": "${taskName.stringProperty}",
                          "operation": "NOT_CONTAINS",
                          "value2": "REGEX"
                        }
                      ],
                      [
                        {
                          "type": "boolean",
                          "value1": "${taskName.booleanProperty}",
                          "operation": "NOT_EQUALS",
                          "value2": "false"
                        }
                      ]
                    ]

                    There are 2 arrays with 'task' type:
                    - 'caseTrue' - tasks when the conditions evaluate to true
                    - 'caseFalse' - tasks when the conditions evaluate to false
                    """;
            case "loop" ->
                """
                    'items' is an array that contains the items to loop over.
                    'iteratee' is an array that contains the tasks to execute for each item.
                    There is a secret output parameter that can only be used inside iteratee and it represents the item
                    Example:
                    ${taskName.item}
                    ${taskName.item.stringParameter}
                    ${taskName.item[0].numberParameter}
                    """;
            default -> null;
        };
    }

    /**
     * Comprehensive task dispatcher information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TaskDispatcherInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the task dispatcher") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the task dispatcher") String description,
        @JsonProperty("title") @JsonPropertyDescription("The title of the task dispatcher") String title,
        @JsonProperty("type") @JsonPropertyDescription("The type of the task dispatcher in format {name}/v{version}") String type,
        @JsonProperty("properties") @JsonPropertyDescription("The properties of the task dispatcher as JSON string") String properties,
        @JsonProperty("outputProperties") @JsonPropertyDescription("The output properties of the task dispatcher as JSON string") String outputProperties) {
    }

    /**
     * Minimal task dispatcher information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TaskDispatcherMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the task dispatcher") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the task dispatcher") String description,
        @JsonProperty("version") @JsonPropertyDescription("The version of the task dispatcher") int version) {
    }
}
