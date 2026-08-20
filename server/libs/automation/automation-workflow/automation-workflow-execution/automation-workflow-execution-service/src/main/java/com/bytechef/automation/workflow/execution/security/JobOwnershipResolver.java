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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JobOwnershipResolver.class);

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
            .flatMap(this::fetchProject)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }

    /**
     * Mirrors {@code JobVisibilityProvider.fetchProject} deliberately — the two must agree on which jobs are reachable,
     * so they are changed together or not at all.
     */
    private Optional<Project> fetchProject(String workflowId) {
        try {
            // fetchWorkflowProject, not getWorkflowProject: "this workflow belongs to no project" (a platform or
            // embedded job) is an ordinary answer here, not an error. Catching getWorkflowProject's throw is doubly
            // wrong — it is control flow through an exception, and the throw crosses ProjectServiceImpl's
            // @Transactional proxy, marking THIS caller's participating transaction rollback-only, so the catch block
            // below would run inside a transaction already doomed to fail at commit.
            return projectService.fetchWorkflowProject(workflowId);
        } catch (RuntimeException exception) {
            // Anything reaching here is an infrastructure failure, not an absent project. Fail closed, but say so: a
            // silent null denies every permission check on this job indistinguishably from "not your workspace",
            // leaving an operator nothing to look at.
            log.error(
                "Denying ownership for workflowId={}: resolving its owning project failed", workflowId, exception);

            return Optional.empty();
        }
    }
}
