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
import java.util.List;

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
     * Returns the workflow input names that the expression evaluator cannot resolve. An input is referenced from other
     * nodes as <code>${inputName}</code>, and the evaluator only accepts an accessor whose root matches
     * <code>[a-zA-Z_][a-zA-Z0-9_]*</code>. A name outside that set leaves the literal <code>${...}</code> text in place
     * instead of the input's value, so the workflow runs and silently feeds the wrong data downstream.
     *
     * <p>
     * Blank names are left to the required-field validation, and workflow JSON that cannot be parsed yields an empty
     * list: like {@link #getDuplicateNodeNames(String)}, this guard fails open so it only ever rejects genuinely
     * unresolvable input names.
     *
     * @param workflow the workflow JSON string to inspect
     * @return the invalid input names, or an empty list when all input names are resolvable (or the JSON cannot be
     *         parsed)
     */
    List<String> getInvalidInputNames(String workflow);

    /**
     * Save-time guard that rejects a workflow whose input names the expression evaluator cannot resolve. Delegates to
     * {@link #getInvalidInputNames(String)} so the guard and the editor's inline validation agree on what counts as a
     * valid name.
     *
     * @param workflow the workflow JSON string to inspect
     * @throws ConfigurationException if any input name is not resolvable as an expression accessor
     */
    default void validateInputNames(String workflow) {
        List<String> invalidInputNames = getInvalidInputNames(workflow);

        if (!invalidInputNames.isEmpty()) {
            throw new ConfigurationException(
                "Workflow input names must start with a letter or underscore and contain only letters, digits and " +
                    "underscores. Invalid input names: " + String.join(", ", invalidInputNames),
                WorkflowValidatorErrorType.INVALID_INPUT_NAME);
        }
    }

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
