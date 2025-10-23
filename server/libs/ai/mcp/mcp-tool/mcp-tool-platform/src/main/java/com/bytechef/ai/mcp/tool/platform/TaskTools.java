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
import com.bytechef.ai.mcp.tool.platform.TaskDispatcherTools.TaskDispatcherInfo;
import com.bytechef.ai.mcp.tool.platform.TaskDispatcherTools.TaskDispatcherMinimalInfo;
import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.bytechef.platform.workflow.validator.WorkflowValidator;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component
public class TaskTools {

    private static final Logger logger = LoggerFactory.getLogger(TaskTools.class);

    private static final String INVALID_TASK_TYPE =
        "Invalid task type. Must be 'action', 'trigger', or 'taskDispatcher'";
    private static final String FAILED_TO_GET_TASK = "Failed to get task";
    private static final String FAILED_TO_LIST_TASKS = "Failed to list tasks";
    private static final String FAILED_TO_SEARCH_TASKS = "Failed to search tasks";

    private final ComponentTools componentTools;
    private final TaskDispatcherTools taskDispatcherTools;

    @SuppressFBWarnings("EI")
    public TaskTools(ComponentTools componentTools, TaskDispatcherTools taskDispatcherTools) {
        this.componentTools = componentTools;
        this.taskDispatcherTools = taskDispatcherTools;
    }

    @Tool(
        description = "Get detailed information about a specific task (action, trigger, or taskDispatcher). Returns comprehensive task information based on the type parameter")
    public TaskInfo getTask(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'taskDispatcher'") String type,
        @ToolParam(description = "The name of the task to retrieve") String name,
        @ToolParam(
            description = "For actions/triggers: the component name. Not used for taskDispatchers") String componentName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            return switch (type.toLowerCase()
                .trim()) {
                case "action" -> {
                    if (componentName == null || StringUtils.isBlank(componentName)) {
                        throw new IllegalArgumentException("componentName is required for action type");
                    }

                    ActionDetailedInfo actionDetailedInfo = componentTools.getAction(componentName, name, version);

                    yield new TaskInfo(
                        actionDetailedInfo.name(), actionDetailedInfo.title(), actionDetailedInfo.description(),
                        "action", actionDetailedInfo.componentName(), actionDetailedInfo.properties(),
                        actionDetailedInfo.outputProperties());
                }
                case "trigger" -> {
                    if (StringUtils.isBlank(componentName)) {
                        throw new IllegalArgumentException("componentName is required for trigger type");
                    }

                    TriggerDetailedInfo triggerDetailedInfo = componentTools.getTrigger(componentName, name, version);

                    yield new TaskInfo(
                        triggerDetailedInfo.name(), triggerDetailedInfo.title(), triggerDetailedInfo.description(),
                        "trigger", triggerDetailedInfo.componentName(), triggerDetailedInfo.properties(),
                        triggerDetailedInfo.outputProperties());
                }
                case "taskdispatcher" -> {
                    TaskDispatcherInfo taskDispatcherInfo = taskDispatcherTools.getTaskDispatcher(name, version);

                    yield new TaskInfo(
                        taskDispatcherInfo.name(), taskDispatcherInfo.title(), taskDispatcherInfo.description(),
                        "taskDispatcher", null, taskDispatcherInfo.properties(), taskDispatcherInfo.outputProperties());
                }
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get task '{}' of type '{}'", name, type, e);

            throw new RuntimeException(FAILED_TO_GET_TASK, e);
        }
    }

    @Tool(
        description = "Get all properties of a specific task (action, trigger, or taskDispatcher). Returns a hierarchical list of properties including nested properties")
    public List<PropertyInfo> getTaskProperties(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'taskDispatcher'") String type,
        @ToolParam(description = "The name of the task to retrieve properties for") String name,
        @ToolParam(
            description = "For actions/triggers: the component name. Not used for taskDispatchers") String componentName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            return switch (StringUtils.trim(type.toLowerCase())) {
                case "action", "trigger" -> {
                    if (componentName == null || StringUtils.isBlank(componentName)) {
                        throw new IllegalArgumentException("componentName is required for " + type + " type");
                    }
                    yield componentTools.getProperties(componentName, name, version);
                }
                case "taskdispatcher" -> taskDispatcherTools.getTaskDispatcherProperties(name, version);
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get properties for task '{}' of type '{}'", name, type, e);

            throw new RuntimeException("Failed to get properties", e);
        }
    }

    @Tool(
        description = "Get the output property of a specific task (action, trigger, or taskDispatcher). Returns the structure of the output property")
    public PropertyInfo getTaskOutputProperty(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'taskDispatcher'") String type,
        @ToolParam(description = "The name of the task to retrieve output properties for") String name,
        @ToolParam(
            required = false,
            description = "For actions/triggers: the component name. Not used for taskDispatchers") String componentName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            return switch (StringUtils.trim(type.toLowerCase())) {
                case "action", "trigger" -> {
                    if (componentName == null || StringUtils.isBlank(componentName)) {
                        throw new IllegalArgumentException("componentName is required for " + type + " type");
                    }
                    yield componentTools.getOutputProperty(componentName, name, version);
                }
                case "taskdispatcher" -> taskDispatcherTools.getTaskDispatcherOutput(name, version);
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get output property for task '{}' of type '{}'", name, type, e);
            throw e;
        }
    }

    @Tool(
        description = "Get the task definition template for a specific task (action, trigger, or taskDispatcher). Returns a JSON template that can be used to configure the task in workflows")
    public String getTaskDefinition(
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'taskDispatcher'") String type,
        @ToolParam(description = "The name of the task to generate definition for") String name,
        @ToolParam(
            description = "For actions/triggers: the component name. Not used for taskDispatchers") String componentName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            return switch (StringUtils.trim(type.toLowerCase())) {
                case "action" -> {
                    if (componentName == null || StringUtils.isBlank(componentName)) {
                        throw new IllegalArgumentException("componentName is required for action type");
                    }
                    yield componentTools.getActionDefinition(componentName, name, version);
                }
                case "trigger" -> {
                    if (componentName == null || StringUtils.isBlank(componentName)) {
                        throw new IllegalArgumentException("componentName is required for trigger type");
                    }
                    yield componentTools.getTriggerDefinition(componentName, name, version);
                }
                case "taskdispatcher" -> taskDispatcherTools.getTaskDispatcherDefinition(name, version);
                default -> throw new IllegalArgumentException(INVALID_TASK_TYPE);
            };
        } catch (Exception e) {
            logger.error("Failed to get definition for task '{}' of type '{}'", name, type, e);

            throw new RuntimeException("Failed to get task definition", e);
        }
    }

    @Tool(
        description = "Instructions for building with task dispatchers")
    public String getTaskDispatcherBuildInstructions(
        @ToolParam(description = "The name of the task dispatcher you want instructions for") String taskDispatcher) {

        StringBuilder builder = new StringBuilder();

        String flowString = taskDispatcherTools.getTaskDispatcherInstructions(taskDispatcher);

        if (flowString != null) {
            builder.append("""
                Whenever you see an array with 'task' type, you can put as may tasks in that array.
                """);
            builder.append("\n");
            builder.append(flowString);
        } else {
            builder.append("Task dispatcher with that name does not exist.");
        }

        return builder.toString();
    }

    @Tool(
        description = "List tasks in the project. Returns a list with their basic information. Can filter by type and limit results")
    public List<TaskMinimalInfo> listTasks(
        @ToolParam(
            required = false,
            description = "Type filter: 'action', 'trigger', 'taskDispatcher', or null for all types") String type,
        @ToolParam(required = false, description = "Limit on number of results returned") Integer limit) {

        try {
            List<TaskMinimalInfo> allTasks = new ArrayList<>();
            String normalizedType = type != null ? StringUtils.trim(type.toLowerCase()) : null;

            boolean includeActions = normalizedType == null || "action".equals(normalizedType);
            boolean includeTriggers = normalizedType == null || "trigger".equals(normalizedType);
            boolean includeTaskDispatchers = normalizedType == null || "taskdispatcher".equals(normalizedType);

            int actionsCount = 0;
            int triggersCount = 0;
            int taskDispatchersCount = 0;

            if (includeActions) {
                List<ActionMinimalInfo> actions = componentTools.listActions();

                actionsCount = actions.size();

                for (ActionMinimalInfo action : actions) {
                    allTasks.add(
                        new TaskMinimalInfo(action.name(), action.description(), "action", action.componentName()));
                }
            }

            if (includeTriggers) {
                List<TriggerMinimalInfo> triggers = componentTools.listTriggers();

                triggersCount = triggers.size();

                for (TriggerMinimalInfo trigger : triggers) {
                    allTasks.add(
                        new TaskMinimalInfo(trigger.name(), trigger.description(), "trigger", trigger.componentName()));
                }
            }

            if (includeTaskDispatchers) {
                List<TaskDispatcherMinimalInfo> taskDispatcherMinimalInfos = taskDispatcherTools.listTaskDispatchers();
                taskDispatchersCount = taskDispatcherMinimalInfos.size();

                for (TaskDispatcherMinimalInfo taskDispatcherMinimalInfo : taskDispatcherMinimalInfos) {
                    allTasks.add(
                        new TaskMinimalInfo(
                            taskDispatcherMinimalInfo.name(), taskDispatcherMinimalInfo.description(), "taskDispatcher",
                            null));
                }
            }

            if (limit != null && limit > 0 && allTasks.size() > limit) {
                allTasks = allTasks.subList(0, limit);
            }

            if (logger.isDebugEnabled()) {
                String typeFilter = normalizedType != null ? " (filtered by type: " + normalizedType + ")" : "";
                String limitInfo = limit != null ? " (limited to " + limit + ")" : "";

                logger.debug(
                    "Found {} total tasks{}{} ({} actions, {} triggers, {} taskDispatchers)", allTasks.size(),
                    typeFilter,
                    limitInfo, actionsCount, triggersCount, taskDispatchersCount);
            }

            return allTasks;
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_TASKS, e);

            throw new RuntimeException(FAILED_TO_LIST_TASKS, e);
        }
    }

    @Tool(
        description = "Search tasks across types (actions, triggers, and taskDispatchers). Returns a combined list matching the search query. Can filter by type and limit results")
    public List<TaskMinimalInfo> searchTasks(
        @ToolParam(description = "The search query to match against task names and descriptions") String query,
        @ToolParam(
            required = false,
            description = "Type filter: 'action', 'trigger', 'taskDispatcher', or null for all types") String type,
        @ToolParam(
            required = false, description = "Limit on number of results returned (defaults to 30)") Integer limit) {

        try {
            List<TaskMinimalInfo> matchingTasks = new ArrayList<>();
            String normalizedType = type != null ? StringUtils.trim(type.toLowerCase()) : null;

            boolean includeActions = normalizedType == null || "action".equals(normalizedType);
            boolean includeTriggers = normalizedType == null || "trigger".equals(normalizedType);
            boolean includeTaskDispatchers = normalizedType == null || "taskdispatcher".equals(normalizedType);

            int effectiveLimit = limit != null ? limit : 30;
            int actionsCount = 0;
            int triggersCount = 0;
            int taskDispatchersCount = 0;

            if (includeActions) {
                List<ActionMinimalInfo> matchingActions = componentTools.searchActions(query);

                actionsCount = matchingActions.size();

                for (ActionMinimalInfo action : matchingActions) {
                    matchingTasks.add(
                        new TaskMinimalInfo(action.name(), action.description(), "action", action.componentName()));
                }
            }

            if (includeTriggers) {
                List<TriggerMinimalInfo> matchingTriggers = componentTools.searchTriggers(query);

                triggersCount = matchingTriggers.size();

                for (TriggerMinimalInfo trigger : matchingTriggers) {
                    matchingTasks.add(
                        new TaskMinimalInfo(trigger.name(), trigger.description(), "trigger", trigger.componentName()));
                }
            }

            if (includeTaskDispatchers) {
                List<TaskDispatcherMinimalInfo> matchingTaskDispatchers = taskDispatcherTools.searchTaskDispatchers(
                    query);

                taskDispatchersCount = matchingTaskDispatchers.size();

                for (TaskDispatcherMinimalInfo taskDispatcherMinimalInfo : matchingTaskDispatchers) {
                    matchingTasks.add(new TaskMinimalInfo(
                        taskDispatcherMinimalInfo.name(), taskDispatcherMinimalInfo.description(), "taskDispatcher",
                        null));
                }
            }

            if (normalizedType == null) {
                matchingTasks.sort((task1, task2) -> ToolUtils.compareTasks(task1.name, task1.description,
                    task1.componentName, task2.name, task2.description, task2.componentName, query.toLowerCase()));
            }

            if (effectiveLimit > 0 && matchingTasks.size() > effectiveLimit) {
                matchingTasks = matchingTasks.subList(0, effectiveLimit);
            }

            if (logger.isDebugEnabled()) {
                String typeFilter = normalizedType != null ? " (filtered by type: " + normalizedType + ")" : "";
                String limitInfo = " (limited to " + effectiveLimit + ")";

                logger.debug(
                    "Found {} tasks matching query '{}'{}{} ({} actions, {} triggers, {} taskDispatchers)",
                    matchingTasks.size(), query, typeFilter, limitInfo, actionsCount, triggersCount,
                    taskDispatchersCount);
            }

            return matchingTasks;
        } catch (Exception e) {
            logger.error("Failed to search tasks with query '{}'", query, e);

            throw new RuntimeException(FAILED_TO_SEARCH_TASKS, e);
        }
    }

    @Tool(
        description = "Validate a task configuration by checking both its structure and properties against the task definition. Returns validation results with any errors found")
    public TaskValidationResult validateTask(
        @ToolParam(description = "The JSON string of the task to validate") String task,
        @ToolParam(description = "The type of task: 'action', 'trigger', or 'taskDispatcher'") String type,
        @ToolParam(description = "The name of the task for validation") String name,
        @ToolParam(
            description = "For actions/triggers: the component name. Not used for taskDispatchers") String componentName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

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

            throw new RuntimeException("Failed to validate task", e);
        }
    }

    /**
     * Detailed task information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TaskInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the task") String name,
        @JsonProperty("title") @JsonPropertyDescription("The title of the task") String title,
        @JsonProperty("description") @JsonPropertyDescription("The description of the task") String description,
        @JsonProperty("type") @JsonPropertyDescription("The type of the task: action, trigger, or taskDispatcher") String type,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component (null for taskDispatchers)") String componentName,
        @JsonProperty("properties") @JsonPropertyDescription("The properties of the task as JSON string") String properties,
        @JsonProperty("outputProperties") @JsonPropertyDescription("The output properties of the task as JSON string") String outputProperties) {
    }

    /**
     * Minimal task information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TaskMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the task") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the task") String description,
        @JsonProperty("type") @JsonPropertyDescription("The type of the task: action, trigger, or taskDispatcher") String type,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component (null for taskDispatchers)") String componentName) {

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
}
