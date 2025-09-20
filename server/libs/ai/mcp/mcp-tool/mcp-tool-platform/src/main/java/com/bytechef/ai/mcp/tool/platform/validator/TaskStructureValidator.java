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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Specialized validator for task structure validation. Part of Strategy pattern for different validation types.
 */
public class TaskStructureValidator {

    /**
     * Validates the structure of a single task JSON.
     *
     * @param task   the task JSON string to validate
     * @param errors StringBuilder to collect validation errors
     * @return the errors StringBuilder for method chaining
     */
    public StringBuilder validate(String task, StringBuilder errors) {
        JsonNode taskNode = JsonUtils.parseJsonWithErrorHandling(task, errors);
        if (taskNode == null) {
            return errors;
        }

        if (!JsonUtils.validateNodeIsObject(taskNode, "Task", errors)) {
            return errors;
        }

        // Validate required task fields
        FieldValidator.validateRequiredStringField(taskNode, "label", errors);
        FieldValidator.validateRequiredStringField(taskNode, "name", errors);
        FieldValidator.validateTaskType(taskNode, errors);
        FieldValidator.validateRequiredObjectField(taskNode, "parameters", errors);

        return errors;
    }
}
