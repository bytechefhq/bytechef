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

package com.bytechef.automation.configuration.instance.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalSummary;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ProjectDeploymentJobPrincipalAccessorTest {

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectDeploymentJobPrincipalAccessor projectDeploymentJobPrincipalAccessor =
        new ProjectDeploymentJobPrincipalAccessor(
            projectDeploymentService, mock(ProjectDeploymentWorkflowService.class), projectService,
            mock(ProjectWorkflowService.class));

    @Disabled
    @Test
    public void createJob() {
        // TODO
    }

    @Disabled
    @Test
    public void getInputs() {
        // TODO
    }

    @Test
    public void testGetJobPrincipalSummaryDescribesTheDeployedProject() {
        when(projectDeploymentService.getProjectDeployment(7L)).thenReturn(projectDeployment(1L));
        when(projectService.fetchProject(1L)).thenReturn(Optional.of(project("CRM")));

        assertEquals(
            new JobPrincipalSummary(Environment.PRODUCTION.ordinal(), 1L, "CRM", 3),
            projectDeploymentJobPrincipalAccessor.getJobPrincipalSummary(7L));
    }

    @Test
    public void testGetJobPrincipalSummaryOmitsTheNameOfAMissingProject() {
        when(projectDeploymentService.getProjectDeployment(7L)).thenReturn(projectDeployment(1L));
        when(projectService.fetchProject(1L)).thenReturn(Optional.empty());

        assertEquals(
            new JobPrincipalSummary(Environment.PRODUCTION.ordinal(), 1L, null, 3),
            projectDeploymentJobPrincipalAccessor.getJobPrincipalSummary(7L));
    }

    private static Project project(String name) {
        Project project = new Project();

        project.setName(name);

        return project;
    }

    private static ProjectDeployment projectDeployment(long projectId) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(3);

        return projectDeployment;
    }
}
