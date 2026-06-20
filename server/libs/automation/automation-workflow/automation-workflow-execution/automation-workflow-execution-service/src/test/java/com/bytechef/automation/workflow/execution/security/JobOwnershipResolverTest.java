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

class JobOwnershipResolverTest {

    private final JobService jobService = mock(JobService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final JobOwnershipResolver resolver = new JobOwnershipResolver(jobService, projectService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("Job");
    }

    @Test
    void testResolvesWorkspaceViaWorkflowProject() {
        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("wf-1");
        when(jobService.fetchJob(1L)).thenReturn(Optional.of(job));

        Project project = mock(Project.class);

        when(project.getWorkspaceId()).thenReturn(42L);
        when(projectService.getWorkflowProject("wf-1")).thenReturn(project);

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
    }

    @Test
    void testUnknownJobIsUnknown() {
        when(jobService.fetchJob(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testNonProjectWorkflowFailsClosed() {
        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("platform-wf");
        when(jobService.fetchJob(2L)).thenReturn(Optional.of(job));
        when(projectService.getWorkflowProject("platform-wf"))
            .thenThrow(new IllegalArgumentException("not a project workflow"));

        assertThat(resolver.resolveOwner(2L)
            .workspaceId()).isEmpty();
    }
}
