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

import com.bytechef.ai.mcp.tool.platform.ComponentTools.ActionDetailedInfo;
import com.bytechef.ai.mcp.tool.platform.ComponentTools.ActionMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.ComponentTools.TriggerDetailedInfo;
import com.bytechef.ai.mcp.tool.platform.ComponentTools.TriggerMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.FlowTools.FlowInfo;
import com.bytechef.ai.mcp.tool.platform.FlowTools.FlowMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.bytechef.ai.mcp.tool.platform.validator.WorkflowValidator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component
public class GenericTools {

    private static final Logger logger = LoggerFactory.getLogger(GenericTools.class);

    // Error message constants
    private static final String INVALID_TASK_TYPE = "Invalid task type. Must be 'action', 'trigger', or 'flow'";
    private static final String FAILED_TO_GET_TASK = "Failed to get task";
    private static final String FAILED_TO_LIST_TASKS = "Failed to list tasks";
    private static final String FAILED_TO_SEARCH_TASKS = "Failed to search tasks";

    private final ComponentTools componentTools;
    private final FlowTools flowTools;

    @SuppressFBWarnings("EI")
    public GenericTools(ComponentTools componentTools, FlowTools flowTools) {
        this.componentTools = componentTools;
        this.flowTools = flowTools;
    }

    @Tool(
        description = "Get detailed information about a specific task (action, trigger, or flow). Returns comprehensive task information based on the type parameter")
    public TaskInfo getTask(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'flow'") String type,
        @ToolParam(description = "The name of the task to retrieve") String name,
        @ToolParam(description = "For actions/triggers: the component name. Not used for flows") String componentName,
        @ToolParam(description = "The version (optional)") Integer version) {

        try {
            return switch (type.toLowerCase()
                .trim()) {
                case "action" -> {
                    if (componentName == null || componentName.trim()
                        .isEmpty()) {
                        throw new IllegalArgumentException("componentName is required for action type");
                    }
                    ActionDetailedInfo actionInfo = componentTools.getAction(componentName, name, version);
                    yield new TaskInfo(
                        actionInfo.name(),
                        actionInfo.title(),
                        actionInfo.description(),
                        "action",
                        actionInfo.componentName(),
                        actionInfo.properties(),
                        actionInfo.outputProperties());
                }
                case "trigger" -> {
                    if (componentName == null || componentName.trim()
                        .isEmpty()) {
                        throw new IllegalArgumentException("componentName is required for trigger type");
                    }
                    TriggerDetailedInfo triggerInfo = componentTools.getTrigger(componentName, name, version);
                    yield new TaskInfo(
                        triggerInfo.name(),
                        triggerInfo.title(),
                        triggerInfo.description(),
                        "trigger",
                        triggerInfo.componentName(),
                        triggerInfo.properties(),
                        triggerInfo.outputProperties());
                }
                case "flow" -> {
                    FlowInfo flowInfo = flowTools.getFlow(name, version);
                    yield new TaskInfo(
                        flowInfo.name(),
                        flowInfo.title(),
                        flowInfo.description(),
                        "flow",
                        null,
                        flowInfo.properties(),
                        flowInfo.outputProperties());
                }
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get task '{}' of type '{}'", name, type, e);
            throw ToolUtils.createOperationException(FAILED_TO_GET_TASK, e);
        }
    }

    @Tool(
        description = "List tasks in the project. Returns a list with their basic information. Can filter by type and limit results")
    public List<TaskMinimalInfo> listTasks(
        @ToolParam(
            description = "Type filter: 'action', 'trigger', 'flow', or null for all types (optional)") String type,
        @ToolParam(description = "Limit on number of results returned (optional)") Integer limit) {

        try {
            List<TaskMinimalInfo> allTasks = new ArrayList<>();
            String normalizedType = type != null ? type.toLowerCase()
                .trim() : null;

            // Determine which types to include
            boolean includeActions = normalizedType == null || "action".equals(normalizedType);
            boolean includeTriggers = normalizedType == null || "trigger".equals(normalizedType);
            boolean includeFlows = normalizedType == null || "flow".equals(normalizedType);

            int actionsCount = 0;
            int triggersCount = 0;
            int flowsCount = 0;

            // Add actions if requested
            if (includeActions) {
                List<ActionMinimalInfo> actions = componentTools.listActions();
                actionsCount = actions.size();
                for (ActionMinimalInfo action : actions) {
                    allTasks.add(new TaskMinimalInfo(
                        action.name(),
                        action.description(),
                        "action",
                        action.componentName()));
                }
            }

            // Add triggers if requested
            if (includeTriggers) {
                List<TriggerMinimalInfo> triggers = componentTools.listTriggers();
                triggersCount = triggers.size();
                for (TriggerMinimalInfo trigger : triggers) {
                    allTasks.add(new TaskMinimalInfo(
                        trigger.name(),
                        trigger.description(),
                        "trigger",
                        trigger.componentName()));
                }
            }

            // Add flows if requested
            if (includeFlows) {
                List<FlowMinimalInfo> flows = flowTools.listFlows();
                flowsCount = flows.size();
                for (FlowMinimalInfo flow : flows) {
                    allTasks.add(new TaskMinimalInfo(
                        flow.name(),
                        flow.description(),
                        "flow",
                        null));
                }
            }

            // Apply limit if specified
            if (limit != null && limit > 0 && allTasks.size() > limit) {
                allTasks = allTasks.subList(0, limit);
            }

            if (logger.isDebugEnabled()) {
                String typeFilter = normalizedType != null ? " (filtered by type: " + normalizedType + ")" : "";
                String limitInfo = limit != null ? " (limited to " + limit + ")" : "";
                logger.debug("Found {} total tasks{}{} ({} actions, {} triggers, {} flows)",
                    allTasks.size(), typeFilter, limitInfo, actionsCount, triggersCount, flowsCount);
            }

            return allTasks;
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_TASKS, e);
            throw ToolUtils.createOperationException(FAILED_TO_LIST_TASKS, e);
        }
    }

    @Tool(
        description = "Search tasks across types (actions, triggers, and flows). Returns a combined list matching the search query. Can filter by type and limit results")
    public List<TaskMinimalInfo> searchTasks(
        @ToolParam(description = "The search query to match against task names and descriptions") String query,
        @ToolParam(
            description = "Type filter: 'action', 'trigger', 'flow', or null for all types (optional)") String type,
        @ToolParam(description = "Limit on number of results returned (optional, defaults to 30)") Integer limit) {

        try {
            List<TaskMinimalInfo> matchingTasks = new ArrayList<>();
            String normalizedType = type != null ? type.toLowerCase()
                .trim() : null;
            int effectiveLimit = limit != null ? limit : 30;

            // Determine which types to include
            boolean includeActions = normalizedType == null || "action".equals(normalizedType);
            boolean includeTriggers = normalizedType == null || "trigger".equals(normalizedType);
            boolean includeFlows = normalizedType == null || "flow".equals(normalizedType);

            int actionsCount = 0;
            int triggersCount = 0;
            int flowsCount = 0;

            // Search actions if requested
            if (includeActions) {
                List<ActionMinimalInfo> matchingActions = componentTools.searchActions(query);
                actionsCount = matchingActions.size();
                for (ActionMinimalInfo action : matchingActions) {
                    matchingTasks.add(new TaskMinimalInfo(
                        action.name(),
                        action.description(),
                        "action",
                        action.componentName()));
                }
            }

            // Search triggers if requested
            if (includeTriggers) {
                List<TriggerMinimalInfo> matchingTriggers = componentTools.searchTriggers(query);
                triggersCount = matchingTriggers.size();
                for (TriggerMinimalInfo trigger : matchingTriggers) {
                    matchingTasks.add(new TaskMinimalInfo(
                        trigger.name(),
                        trigger.description(),
                        "trigger",
                        trigger.componentName()));
                }
            }

            // Search flows if requested
            if (includeFlows) {
                List<FlowMinimalInfo> matchingFlows = flowTools.searchFlows(query);
                flowsCount = matchingFlows.size();
                for (FlowMinimalInfo flow : matchingFlows) {
                    matchingTasks.add(new TaskMinimalInfo(
                        flow.name(),
                        flow.description(),
                        "flow",
                        null));
                }
            }

            // sort
            if (normalizedType == null) {
                matchingTasks.sort((task1, task2) -> ToolUtils.compareTasks(task1.name, task1.description,
                    task1.componentName, task2.name, task2.description, task2.componentName, query.toLowerCase()));
            }

            // Apply limit (default 20 if not specified)
            if (effectiveLimit > 0 && matchingTasks.size() > effectiveLimit) {
                matchingTasks = matchingTasks.subList(0, effectiveLimit);
            }

            if (logger.isDebugEnabled()) {
                String typeFilter = normalizedType != null ? " (filtered by type: " + normalizedType + ")" : "";
                String limitInfo = " (limited to " + effectiveLimit + ")";
                logger.debug("Found {} tasks matching query '{}'{}{} ({} actions, {} triggers, {} flows)",
                    matchingTasks.size(), query, typeFilter, limitInfo, actionsCount, triggersCount, flowsCount);
            }

            return matchingTasks;
        } catch (Exception e) {
            logger.error("Failed to search tasks with query '{}'", query, e);
            throw ToolUtils.createOperationException(FAILED_TO_SEARCH_TASKS, e);
        }
    }

    @Tool(
        description = "Get all properties of a specific task (action, trigger, or flow). Returns a hierarchical list of properties including nested properties")
    public List<ToolUtils.PropertyInfo> getTaskProperties(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'flow'") String type,
        @ToolParam(description = "The name of the task to retrieve properties for") String name,
        @ToolParam(description = "For actions/triggers: the component name. Not used for flows") String componentName,
        @ToolParam(description = "The version (optional)") Integer version) {

        try {
            return switch (type.toLowerCase()
                .trim()) {
                case "action", "trigger" -> {
                    if (componentName == null || componentName.trim()
                        .isEmpty()) {
                        throw new IllegalArgumentException("componentName is required for " + type + " type");
                    }
                    yield componentTools.getProperties(componentName, name, version);
                }
                case "flow" -> flowTools.getFlowProperties(name, version);
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get properties for task '{}' of type '{}'", name, type, e);
            throw ToolUtils.createOperationException("Failed to get properties", e);
        }
    }

    @Tool(
        description = "Get the output property of a specific task (action, trigger, or flow). Returns the structure of the output property")
    public ToolUtils.PropertyInfo getTaskOutputProperty(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'flow'") String type,
        @ToolParam(description = "The name of the task to retrieve output properties for") String name,
        @ToolParam(
            description = "For actions/triggers: the component name. Not used for flows (optional)") String componentName,
        @ToolParam(description = "The version (optional)") Integer version) {

        try {
            return switch (type.toLowerCase()
                .trim()) {
                case "action", "trigger" -> {
                    if (componentName == null || componentName.trim()
                        .isEmpty()) {
                        throw new IllegalArgumentException("componentName is required for " + type + " type");
                    }
                    yield componentTools.getOutputProperty(componentName, name, version);
                }
                case "flow" -> flowTools.getFlowOutput(name, version);
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get output property for task '{}' of type '{}'", name, type, e);
            throw e;
        }
    }

    @Tool(
        description = "Get the task definition template for a specific task (action, trigger, or flow). Returns a JSON template that can be used to configure the task in workflows")
    public String getTaskDefinition(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'flow'") String type,
        @ToolParam(description = "The name of the task to generate definition for") String name,
        @ToolParam(description = "For actions/triggers: the component name. Not used for flows") String componentName,
        @ToolParam(description = "The version (optional)") Integer version) {

        try {
            return switch (type.toLowerCase()
                .trim()) {
                case "action" -> {
                    if (componentName == null || componentName.trim()
                        .isEmpty()) {
                        throw new IllegalArgumentException("componentName is required for action type");
                    }
                    yield componentTools.getActionDefinition(componentName, name, version);
                }
                case "trigger" -> {
                    if (componentName == null || componentName.trim()
                        .isEmpty()) {
                        throw new IllegalArgumentException("componentName is required for trigger type");
                    }
                    yield componentTools.getTriggerDefinition(componentName, name, version);
                }
                case "flow" -> flowTools.getFlowDefinition(name, version);
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get definition for task '{}' of type '{}'", name, type, e);
            throw ToolUtils.createOperationException("Failed to get task definition", e);
        }
    }

    @Tool(
        description = "Validate a task configuration by checking both its structure and properties against the task definition. Returns validation results with any errors found")
    public TaskValidationResult validateTask(
        @ToolParam(description = "The JSON string of the task to validate") String task,
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'flow'") String type,
        @ToolParam(description = "The name of the task for validation") String name,
        @ToolParam(description = "For actions/triggers: the component name. Not used for flows") String componentName,
        @ToolParam(description = "The version (optional)") Integer version) {

        try {
            StringBuilder errors = new StringBuilder("[");
            StringBuilder warnings = new StringBuilder("[");

            // Create a task definition provider that gets the task properties for validation
            WorkflowValidator.TaskDefinitionProvider taskDefProvider =
                (taskType, kind) -> getTaskProperties(type, name, componentName, version);

            // Use the refactored WorkflowValidator for single task validation
            WorkflowValidator.validateSingleTask(task, taskDefProvider, errors, warnings);

            String errorMessages = errors.append("]")
                .toString()
                .trim();
            String warningMessages = warnings.append("]")
                .toString()
                .trim();
            boolean isValid = errorMessages.equals("[]");

            if (logger.isDebugEnabled()) {
                logger.debug("Validated task '{}' of type '{}'. Valid: {}, Errors: {}, Warnings: {}",
                    name, type, isValid, errorMessages, warningMessages);
            }

            return new TaskValidationResult(isValid, errorMessages, warningMessages);

        } catch (Exception e) {
            logger.error("Failed to validate task '{}' of type '{}'", name, type, e);
            throw ToolUtils.createOperationException("Failed to validate task", e);
        }
    }

    /**
     * Minimal task information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TaskMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the task") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the task") String description,
        @JsonProperty("type") @JsonPropertyDescription("The type of the task: action, trigger, or flow") String type,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component (null for flows)") String componentName) {
    }

    /**
     * Detailed task information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TaskInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the task") String name,
        @JsonProperty("title") @JsonPropertyDescription("The title of the task") String title,
        @JsonProperty("description") @JsonPropertyDescription("The description of the task") String description,
        @JsonProperty("type") @JsonPropertyDescription("The type of the task: action, trigger, or flow") String type,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component (null for flows)") String componentName,
        @JsonProperty("properties") @JsonPropertyDescription("The properties of the task as JSON string") String properties,
        @JsonProperty("outputProperties") @JsonPropertyDescription("The output properties of the task as JSON string") String outputProperties) {
    }

    /**
     * Task validation result record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TaskValidationResult(
        @JsonProperty("valid") @JsonPropertyDescription("Whether the task is valid") boolean valid,
        @JsonProperty("errors") @JsonPropertyDescription("Error details, which need to be fixed before the task can be valid") String errors,
        @JsonProperty("warnings") @JsonPropertyDescription("Warning details that give additional information") String warnings) {
    }

    public List<ToolUtils.PropertyInfo> getTaskProperties(String type, String taskType) {
        String[] split = type.split("/");
        int version = Integer.parseInt(split[1].substring(1));
        if (split.length == 2) {
            return getTaskProperties("flow", split[0], split[0], version);
        } else if (taskType.equals("trigger")) {
            return getTaskProperties(taskType, split[2], split[0], version);
        } else {
            return getTaskProperties("action", split[2], split[0], version);
        }
    }

    public ToolUtils.PropertyInfo getTaskOutputProperty(String type, String taskType, StringBuilder warnings) {
        String[] split = type.split("/");
        int version = Integer.parseInt(split[1].substring(1));

        try {
            if (split.length == 2) {
                return getTaskOutputProperty("flow", split[0], split[0], version);
            } else if (taskType.equals("trigger")) {
                return getTaskOutputProperty(taskType, split[2], split[0], version);
            } else {
                return getTaskOutputProperty("action", split[2], split[0], version);
            }
        } catch (Exception e) {
            warnings.append(e.getMessage());
            return null;
        }
    }
}
