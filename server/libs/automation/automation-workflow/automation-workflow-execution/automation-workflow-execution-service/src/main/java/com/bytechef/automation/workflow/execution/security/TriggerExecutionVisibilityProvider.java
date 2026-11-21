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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * A trigger execution inherits the visibility of the project whose deployment its {@link WorkflowExecutionId} names.
 *
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI")
    public TriggerExecutionVisibilityProvider(
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
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return fetchTriggerExecution(id)
            .map(TriggerExecution::getWorkflowExecutionId)
            .filter(workflowExecutionId -> workflowExecutionId.getType() == PlatformType.AUTOMATION)
            .map(WorkflowExecutionId::getJobPrincipalId)
            .flatMap(projectDeploymentService::fetchProjectDeployment)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectService::fetchProject)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }

    private Optional<TriggerExecution> fetchTriggerExecution(long id) {
        List<TriggerExecution> triggerExecutions = triggerExecutionService.getTriggerExecutions(List.of(id));

        return triggerExecutions.isEmpty() ? Optional.empty() : Optional.of(triggerExecutions.getFirst());
    }
}
