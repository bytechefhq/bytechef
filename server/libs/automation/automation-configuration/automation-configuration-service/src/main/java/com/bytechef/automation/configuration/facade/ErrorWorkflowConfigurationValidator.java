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

package com.bytechef.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Validates an error-workflow reference at the moment it is configured, rather than at failure time. A broken reference
 * discovered while handling a failure would surface as a second failure while the first is being handled.
 *
 * @author Ivica Cardic
 */
@Component
public class ErrorWorkflowConfigurationValidator {

    private static final String ERROR_TRIGGER_COMPONENT_NAME = "workflow";
    private static final String ERROR_TRIGGER_OPERATION_NAME = "newWorkflowError";
    private static final String ERROR_TRIGGER_TYPE = ERROR_TRIGGER_COMPONENT_NAME + "/" + ERROR_TRIGGER_OPERATION_NAME;

    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ErrorWorkflowConfigurationValidator(
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    /**
     * Validates that the workflow referenced by {@code targetProjectWorkflowId} is eligible to act as the error
     * workflow for the workflow identified by {@code configuredOnProjectWorkflowId}.
     *
     * @param projectId                     the project the workflow being configured belongs to
     * @param targetProjectWorkflowId       the project workflow id of the candidate error workflow
     * @param configuredOnProjectWorkflowId the project workflow id the error workflow is being configured on, or
     *                                      {@code null} when not yet known
     * @throws IllegalArgumentException if the target belongs to a different project, does not contain a
     *                                  {@code workflow/newWorkflowError} trigger, or is the workflow being configured
     */
    public void validate(
        long projectId, long targetProjectWorkflowId, @Nullable Long configuredOnProjectWorkflowId) {

        if (Long.valueOf(targetProjectWorkflowId)
            .equals(configuredOnProjectWorkflowId)) {

            throw new IllegalArgumentException("A workflow cannot be its own error workflow");
        }

        ProjectWorkflow target = projectWorkflowService.getProjectWorkflow(targetProjectWorkflowId);

        if (target.getProjectId() != projectId) {
            throw new IllegalArgumentException(
                "The error workflow must belong to the same project as the workflow it handles");
        }

        Workflow workflow = workflowService.getWorkflow(target.getWorkflowId());

        boolean hasErrorTrigger = WorkflowTrigger.of(workflow)
            .stream()
            .anyMatch(ErrorWorkflowConfigurationValidator::isErrorTrigger);

        if (!hasErrorTrigger) {
            throw new IllegalArgumentException(
                "The error workflow must contain a " + ERROR_TRIGGER_TYPE + " trigger");
        }
    }

    /**
     * Trigger types stored in workflow definitions are version-qualified (e.g. {@code workflow/v1/newWorkflowError}),
     * so the component name and operation name must be parsed out and compared individually rather than matching the
     * raw type string against an unqualified literal.
     */
    private static boolean isErrorTrigger(WorkflowTrigger trigger) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(trigger.getType());

        return Objects.equals(workflowNodeType.name(), ERROR_TRIGGER_COMPONENT_NAME) &&
            Objects.equals(workflowNodeType.operation(), ERROR_TRIGGER_OPERATION_NAME);
    }
}
