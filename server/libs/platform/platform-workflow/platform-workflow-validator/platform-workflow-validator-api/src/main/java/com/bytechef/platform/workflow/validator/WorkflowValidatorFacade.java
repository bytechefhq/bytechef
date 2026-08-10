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

package com.bytechef.platform.workflow.validator;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.workflow.validator.exception.WorkflowValidatorErrorType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Facade for workflow validation operations.
 *
 * @author Marko Kriskovic
 */
public interface WorkflowValidatorFacade {

    /**
     * Validates a complete workflow JSON string.
     *
     * @param workflow the workflow JSON string to validate
     * @return a {@link WorkflowValidationResult} containing lists of errors and warnings
     */
    WorkflowValidationResult validateWorkflow(String workflow);

    /**
     * Validates a workflow identified by its ID.
     *
     * @param workflowId the ID of the workflow to validate
     * @return a {@link WorkflowValidationResult} containing lists of errors and warnings
     */
    WorkflowValidationResult validateWorkflowById(String workflowId);

    /**
     * Returns the node names (the trigger plus all tasks, including tasks nested inside condition, loop, branch,
     * parallel, each, fork-join and on-error dispatchers) that occur more than once. Node names are global ids, so a
     * duplicate produces two nodes with the same id and a broken graph. Intended as a lightweight save-time guard that
     * does not resolve component/trigger definitions.
     *
     * <p>
     * Malformed workflow JSON that cannot be parsed also yields an empty list: this guard fails open on purpose because
     * structurally invalid JSON is reported by the separate structure validation, not here.
     *
     * @param workflow the workflow JSON string to inspect
     * @return the duplicated node names, or an empty list when all node names are unique (or the JSON cannot be parsed)
     */
    List<String> getDuplicateNodeNames(String workflow);

    /**
     * Save-time guard that rejects a workflow whose node names are not unique. Delegates to
     * {@link #getDuplicateNodeNames(String)} so the guard and the editor's inline validation agree on what counts as a
     * duplicate. Consistent with that method, it fails open on workflow JSON that cannot be parsed (left to structure
     * validation), so it only ever rejects genuine duplicate node names.
     *
     * @param workflow the workflow JSON string to inspect
     * @throws ConfigurationException if any node name occurs more than once
     */
    default void validateNoDuplicateNodeNames(String workflow) {
        List<String> duplicateNodeNames = getDuplicateNodeNames(workflow);

        if (!duplicateNodeNames.isEmpty()) {
            throw new ConfigurationException(
                "Workflow node names must be unique. Duplicate node names: " + String.join(", ", duplicateNodeNames),
                WorkflowValidatorErrorType.DUPLICATE_NODE_NAMES);
        }
    }

    /**
     * Save-time guard that rejects a workflow with an {@code inputs[].name} starting with the reserved {@code __}
     * prefix. Reserved names are seeded by the platform at job-creation time (see
     * {@code com.bytechef.platform.workflow.JobInputConstants#TRIGGER_NAME_INPUT}); a workflow input using that prefix
     * would silently collide with a platform-seeded job input at execution time.
     *
     * <p>
     * Consistent with {@link #validateNoDuplicateNodeNames(String)}, it fails open on workflow JSON that cannot be
     * parsed (left to structure validation), so it only ever rejects genuine reserved input names.
     *
     * @param workflow the workflow JSON string to inspect
     * @throws ConfigurationException if any {@code inputs[].name} starts with {@code __}
     */
    default void validateNoReservedInputNames(String workflow) {
        List<String> reservedInputNames = getReservedInputNames(workflow);

        if (!reservedInputNames.isEmpty()) {
            throw new ConfigurationException(
                "Workflow input names must not start with the reserved '__' prefix. Reserved input names: " +
                    String.join(", ", reservedInputNames),
                WorkflowValidatorErrorType.RESERVED_INPUT_NAME);
        }
    }

    /**
     * Save-time guard that rejects a workflow whose top-level trigger or task node name starts with the reserved
     * {@code __} prefix. Node names double as job-input keys downstream (see
     * {@code WebhookWorkflowExecutorImpl#createJobParameters}), so a {@code __}-prefixed node name could collide with a
     * platform-seeded job input the same way a reserved input name would.
     *
     * <p>
     * Only scans the top-level {@code triggers} and {@code tasks} arrays, not names nested inside task-dispatcher
     * parameters (condition branches, loops, branches, etc.) — those nested node names do not feed the job-parameters
     * map this guard protects. Consistent with {@link #validateNoDuplicateNodeNames(String)}, it fails open on workflow
     * JSON that cannot be parsed.
     *
     * @param workflow the workflow JSON string to inspect
     * @throws ConfigurationException if any top-level trigger/task node name starts with {@code __}
     */
    default void validateNoReservedNodeNames(String workflow) {
        List<String> reservedNodeNames = getReservedNodeNames(workflow);

        if (!reservedNodeNames.isEmpty()) {
            throw new ConfigurationException(
                "Workflow node names must not start with the reserved '__' prefix. Reserved node names: " +
                    String.join(", ", reservedNodeNames),
                WorkflowValidatorErrorType.RESERVED_NODE_NAME);
        }
    }

    /**
     * Returns the {@code inputs[].name} values that begin with the reserved {@code __} prefix, or an empty list when
     * none are found (or the workflow JSON cannot be parsed).
     */
    private List<String> getReservedInputNames(String workflow) {
        List<String> reservedInputNames = new ArrayList<>();

        try {
            JsonNode workflowJsonNode = readWorkflowTree(workflow);
            JsonNode inputsJsonNode = workflowJsonNode.get("inputs");

            if (inputsJsonNode != null && inputsJsonNode.isArray()) {
                for (JsonNode inputJsonNode : inputsJsonNode) {
                    if (!inputJsonNode.isObject()) {
                        continue;
                    }

                    JsonNode nameJsonNode = inputJsonNode.get("name");

                    if (nameJsonNode != null && nameJsonNode.isString() && nameJsonNode.asString()
                        .startsWith("__")) {

                        reservedInputNames.add(nameJsonNode.asString());
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }

        return reservedInputNames;
    }

    /**
     * Returns the top-level trigger/task node names that begin with the reserved {@code __} prefix, or an empty list
     * when none are found (or the workflow JSON cannot be parsed).
     */
    private List<String> getReservedNodeNames(String workflow) {
        List<String> reservedNodeNames = new ArrayList<>();

        try {
            JsonNode workflowJsonNode = readWorkflowTree(workflow);

            collectReservedNodeNames(workflowJsonNode.get("triggers"), reservedNodeNames);
            collectReservedNodeNames(workflowJsonNode.get("tasks"), reservedNodeNames);
        } catch (Exception e) {
            return List.of();
        }

        return reservedNodeNames;
    }

    private static void collectReservedNodeNames(JsonNode nodesJsonNode, List<String> reservedNodeNames) {
        if (nodesJsonNode == null || !nodesJsonNode.isArray()) {
            return;
        }

        for (JsonNode nodeJsonNode : nodesJsonNode) {
            if (!nodeJsonNode.isObject()) {
                continue;
            }

            JsonNode nameJsonNode = nodeJsonNode.get("name");

            if (nameJsonNode != null && nameJsonNode.isString() && nameJsonNode.asString()
                .startsWith("__")) {

                reservedNodeNames.add(nameJsonNode.asString());
            }
        }
    }

    private static JsonNode readWorkflowTree(String workflow) {
        JsonMapper jsonMapper = JsonMapper.builder()
            .build();

        return jsonMapper.readTree(workflow);
    }

    /**
     * Holds the result of a workflow validation, containing lists of error messages and warning messages.
     */
    @SuppressFBWarnings("EI")
    record WorkflowValidationResult(List<String> errors, List<String> warnings) {

        public WorkflowValidationResult(List<String> errors, List<String> warnings) {
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
        }
    }
}
