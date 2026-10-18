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
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A job inherits the visibility of the project its workflow belongs to.
 *
 * @author Ivica Cardic
 */
@Component
public class JobVisibilityProvider implements ResourceVisibilityProvider {

    private static final Logger log = LoggerFactory.getLogger(JobVisibilityProvider.class);

    private final JobService jobService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public JobVisibilityProvider(JobService jobService, ProjectService projectService) {
        this.jobService = jobService;
        this.projectService = projectService;
    }

    @Override
    public String resourceType() {
        return "Job";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return jobService.fetchJob(id)
            .map(Job::getWorkflowId)
            .flatMap(this::fetchProject)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }

    private Optional<Project> fetchProject(String workflowId) {
        try {
            return projectService.fetchWorkflowProject(workflowId);
        } catch (RuntimeException exception) {
            log.error(
                "Denying visibility for workflowId={}: resolving its owning project failed", workflowId, exception);

            return Optional.empty();
        }
    }
}
