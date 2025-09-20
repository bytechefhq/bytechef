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
 * Specialized validator for workflow structure validation. Part of Strategy pattern for different validation types.
 */
public class WorkflowStructureValidator {

    /**
     * Validates the overall structure of a workflow JSON.
     *
     * @param workflow the workflow JSON string to validate
     * @param errors   StringBuilder to collect validation errors
     * @return the errors StringBuilder for method chaining
     */
    public StringBuilder validateStructure(String workflow, StringBuilder errors) {
        JsonNode workflowNode = JsonUtils.parseJsonWithErrorHandling(workflow, errors);
        if (workflowNode == null) {
            return errors;
        }

        if (!JsonUtils.validateNodeIsObject(workflowNode, "Workflow", errors)) {
            return errors;
        }

        // Validate required workflow fields
        FieldValidator.validateRequiredStringField(workflowNode, "label", errors);
        FieldValidator.validateRequiredStringField(workflowNode, "description", errors);
        FieldValidator.validateWorkflowTriggers(workflowNode, errors);
        FieldValidator.validateRequiredArrayField(workflowNode, "tasks", errors);

        return errors;
    }
}
