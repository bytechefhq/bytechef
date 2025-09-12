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

package com.bytechef.ai.mcp.tool.platform.validator;

import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Template Method pattern for task validation. Defines the skeleton of task validation algorithm while letting
 * subclasses override specific steps of the algorithm without changing its structure.
 */
public class TaskValidator {

    private final NestedTaskProcessor nestedTaskProcessor;

    public TaskValidator() {
        this.nestedTaskProcessor = new NestedTaskProcessor();
    }

    /**
     * Template method defining the validation algorithm for all tasks. Works directly with the legacy StringBuilder
     * approach for full backward compatibility.
     */
    public void validateAllTasks(ValidationContext context) {
        for (JsonNode task : context.getTasks()) {
            WorkflowValidator.validateTaskStructure(task.toString(), context.getErrors());

            List<ToolUtils.PropertyInfo> taskDefinition = validateTaskParameters(context, task);

            nestedTaskProcessor.processNestedTaskValidation(task, context);

            validateDataPills(context, task, taskDefinition);
        }
    }

    private static List<ToolUtils.PropertyInfo> validateTaskParameters(ValidationContext context, JsonNode task) {
        String taskType = task.get("type")
            .asText();
        List<ToolUtils.PropertyInfo> taskDefinition = context.getTaskDefinitions()
            .get(taskType);

        if (taskDefinition != null && !taskDefinition.isEmpty()) {
            String taskParameters = "{}";
            if (task.has("parameters") && task.get("parameters")
                .isObject()) {
                taskParameters = task.get("parameters")
                    .toString();
            }
            WorkflowValidator.validateTaskParameters(taskParameters, taskDefinition,
                context.getErrors(), context.getWarnings());
        }
        return taskDefinition;
    }

    private static void
        validateDataPills(ValidationContext context, JsonNode task, List<ToolUtils.PropertyInfo> taskDefinition) {
        if (taskDefinition != null && !taskDefinition.isEmpty()) {
            WorkflowValidator.validateTaskDataPills(task, context.getTaskOutputs(),
                context.getTaskNames(), context.getTaskNameToTypeMap(),
                context.getErrors(), context.getWarnings(), context.getAllTasksMap(),
                taskDefinition, false, true);
        }
    }
}
