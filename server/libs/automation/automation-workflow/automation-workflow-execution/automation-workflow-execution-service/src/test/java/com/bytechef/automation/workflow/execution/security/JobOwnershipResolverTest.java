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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JobOwnershipResolverTest {

    private static final long JOB_ID = 11L;
    private static final long PROJECT_ID = 7L;
    private static final String WORKFLOW_ID = "workflow-uuid";

    private final JobService jobService = mock(JobService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final JobOwnershipResolver resolver = new JobOwnershipResolver(jobService, projectService);

    @Test
    void testResourceTypeMatchesTheTokenTheJobGuardNames() {
        assertThat(resolver.resourceType()).isEqualTo("Job");
    }

    @Test
    void testResolveProjectIdOfAKnownJob() {
        Job job = new Job(JOB_ID);

        job.setWorkflowId(WORKFLOW_ID);

        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(9L);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject(WORKFLOW_ID)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveProjectId(JOB_ID)).hasValue(PROJECT_ID);
    }

    @Test
    void testResolveProjectIdOfAnUnknownJobIsEmpty() {
        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.empty());

        assertThat(resolver.resolveProjectId(JOB_ID)).isEmpty();
    }
}
