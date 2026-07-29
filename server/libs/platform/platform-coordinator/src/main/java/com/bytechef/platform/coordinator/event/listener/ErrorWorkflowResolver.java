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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ErrorWorkflowDispatch;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves which workflow handles a failed run: the failing workflow's own override, else the project default, else
 * none. An explicit disable on the workflow beats an inherited project default, which is why the disable flag is
 * separate from the nullable reference.
 * <p>
 * Only calls interface types ({@code ProjectService}, {@code ProjectDeploymentService}, {@code ProjectWorkflowService},
 * {@code WorkflowService}) that live in {@code automation-configuration-api} (and {@code atlas-configuration}), so it
 * can live in {@code platform-coordinator} and be wired identically in both the monolith (real service impls) and
 * distributed EE (remote clients) -- unlike a dependency on {@code automation-configuration-service}, which pulls
 * concrete JDBC-backed {@code @Service} classes onto the datasource-less coordinator's classpath.
 *
 * @author Ivica Cardic
 */
public class ErrorWorkflowResolver {

    private static final String ERROR_TRIGGER_COMPONENT_NAME = "workflow";
    private static final String ERROR_TRIGGER_OPERATION_NAME = "newWorkflowError";

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ErrorWorkflowResolver(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    /**
     * @param jobPrincipalId the failed job's principal id, which for automation is the PROJECT DEPLOYMENT id, not the
     *                       project id — mapped here so the coordinator needs no project lookups of its own
     */
    public Optional<ErrorWorkflowDispatch> resolve(long jobPrincipalId, String failedWorkflowId) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);

        long projectId = projectDeployment.getProjectId();

        ProjectWorkflow failingProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(failedWorkflowId);

        if (failingProjectWorkflow.isErrorWorkflowDisabled()) {
            return Optional.empty();
        }

        Long targetId = failingProjectWorkflow.getErrorProjectWorkflowId();

        if (targetId == null) {
            Project project = projectService.getProject(projectId);

            targetId = project.getErrorProjectWorkflowId();
        }

        if (targetId == null) {
            return Optional.empty();
        }

        // Defensive: configuration-time validation rejects self-reference, but a workflow can be re-pointed
        // afterwards, and a self-referencing handler would fail forever.
        if (targetId.equals(failingProjectWorkflow.getId())) {
            return Optional.empty();
        }

        ProjectWorkflow target = projectWorkflowService.getProjectWorkflow(targetId);

        Workflow handlerWorkflow = workflowService.getWorkflow(target.getWorkflowId());

        // The dispatched payload must be nested under the handler's error-trigger node name (see
        // ErrorWorkflowDispatch's javadoc), so a handler with no such trigger cannot be dispatched into meaningfully.
        // Configuration-time validation (ErrorWorkflowConfigurationValidator) already rejects this at setup time, but
        // the handler workflow can be edited afterwards to remove the trigger, so this is re-checked here too.
        Optional<String> errorTriggerName = WorkflowTrigger.of(handlerWorkflow)
            .stream()
            .filter(ErrorWorkflowResolver::isErrorTrigger)
            .map(WorkflowTrigger::getName)
            .findFirst();

        if (errorTriggerName.isEmpty()) {
            return Optional.empty();
        }

        Workflow failedWorkflow = workflowService.getWorkflow(failedWorkflowId);

        Environment environment = projectDeployment.getEnvironment();

        return Optional.of(
            new ErrorWorkflowDispatch(
                target.getWorkflowId(), projectId, failingProjectWorkflow.getId(), failedWorkflowId,
                failedWorkflow.getLabel(), environment == null ? null : environment.name(),
                errorTriggerName.get()));
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
