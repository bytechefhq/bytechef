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

package com.bytechef.automation.workflow.execution.security;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps a trigger-execution id to its owning workspace by traversing trigger execution &rarr; the project deployment its
 * {@link WorkflowExecutionId} names &rarr; owning {@link Project} &rarr; {@code project.workspace_id}.
 *
 * <p>
 * Deliberately not routed through the job the way {@link JobOwnershipResolver} is: the rows this resolver gates are the
 * trigger executions that never produced a job (a trigger that failed before dispatching one), so a job-keyed lookup
 * would fail closed on exactly the rows the endpoint exists to serve.
 *
 * <p>
 * Reads via the services directly (none is {@code @PreAuthorize}-guarded) to avoid recursion, and uses the plural
 * {@code getTriggerExecutions} rather than {@code getTriggerExecution}: the singular throws on an absent id, and an
 * unknown id must be an ordinary {@link ResourceOwner#unknown()} here, not an exception. Catching that throw instead
 * would be control flow through an exception AND would cross the service's {@code @Transactional} proxy, marking this
 * caller's participating transaction rollback-only — the same trap {@code JobOwnershipResolver.fetchProject} documents.
 *
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI")
    public TriggerExecutionOwnershipResolver(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        TriggerExecutionService triggerExecutionService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public String resourceType() {
        return "TriggerExecution";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return fetchTriggerExecution(id)
            .map(TriggerExecution::getWorkflowExecutionId)
            .map(WorkflowExecutionId::getJobPrincipalId)
            .flatMap(projectDeploymentService::fetchProjectDeployment)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectService::fetchProject)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }

    private Optional<TriggerExecution> fetchTriggerExecution(long id) {
        List<TriggerExecution> triggerExecutions = triggerExecutionService.getTriggerExecutions(List.of(id));

        return triggerExecutions.isEmpty() ? Optional.empty() : Optional.of(triggerExecutions.getFirst());
    }
}
