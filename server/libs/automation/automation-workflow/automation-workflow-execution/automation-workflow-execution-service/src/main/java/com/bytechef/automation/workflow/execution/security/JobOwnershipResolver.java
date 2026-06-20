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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an automation job (workflow-execution) id to its owning workspace by traversing job &rarr; workflowId &rarr;
 * owning {@link Project} &rarr; {@code project.workspace_id}. Reads via {@link JobService}/{@link ProjectService}
 * (neither is {@code @PreAuthorize}-guarded) to avoid recursion. Fails closed when the job is unknown or its workflow
 * is not a project workflow (e.g. a platform/embedded job).
 *
 * @author Ivica Cardic
 */
@Component
public class JobOwnershipResolver implements ResourceOwnershipResolver {

    private final JobService jobService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public JobOwnershipResolver(JobService jobService, ProjectService projectService) {
        this.jobService = jobService;
        this.projectService = projectService;
    }

    @Override
    public String resourceType() {
        return "Job";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return jobService.fetchJob(id)
            .map(Job::getWorkflowId)
            .map(this::fetchWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }

    private Long fetchWorkspaceId(String workflowId) {
        try {
            Project project = projectService.getWorkflowProject(workflowId);

            return project == null ? null : project.getWorkspaceId();
        } catch (RuntimeException exception) {
            // Not a project workflow (platform/embedded job) — fail closed.
            return null;
        }
    }
}
