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

package com.bytechef.automation.data.table.configuration.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AutomationDataTableWorkspaceResolverTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private final AutomationDataTableWorkspaceResolver automationDataTableWorkspaceResolver =
        new AutomationDataTableWorkspaceResolver(projectService, projectWorkflowService);

    @Test
    void testResolveByWorkflowIdReturnsTheProjectWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(7L);

        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(project));

        assertThat(automationDataTableWorkspaceResolver.resolveByWorkflowId("workflow-1"))
            .isEqualTo(OptionalLong.of(7L));
    }

    @Test
    void testResolveByWorkflowIdIsEmptyWithoutAProject() {
        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.empty());

        assertThat(automationDataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).isEmpty();
    }

    @Test
    void testResolveByJobPrincipalIdReturnsTheDeploymentProjectWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(8L);

        when(projectService.getProjectDeploymentProject(1051L)).thenReturn(project);

        assertThat(automationDataTableWorkspaceResolver.resolveByJobPrincipalId(1051L, PlatformType.AUTOMATION))
            .isEqualTo(OptionalLong.of(8L));
    }

    @Test
    void testResolveByJobPrincipalIdIsEmptyForEmbedded() {
        assertThat(automationDataTableWorkspaceResolver.resolveByJobPrincipalId(1051L, PlatformType.EMBEDDED))
            .isEmpty();

        verifyNoInteractions(projectService);
    }

    @Test
    void testResolveByWorkflowUuidResolvesTheLastWorkflowOfTheUuid() {
        Project project = new Project();

        project.setWorkspaceId(9L);

        when(projectWorkflowService.getLastWorkflowId("workflow-uuid")).thenReturn("workflow-2");
        when(projectService.fetchWorkflowProject("workflow-2")).thenReturn(Optional.of(project));

        assertThat(automationDataTableWorkspaceResolver.resolveByWorkflowUuid("workflow-uuid"))
            .isEqualTo(OptionalLong.of(9L));
    }

    @Test
    void testResolveByWorkflowUuidIsEmptyForAnUnknownUuid() {
        when(projectWorkflowService.getLastWorkflowId("workflow-uuid"))
            .thenThrow(new IllegalArgumentException("No workflow found for workflow uuid workflow-uuid"));

        assertThat(automationDataTableWorkspaceResolver.resolveByWorkflowUuid("workflow-uuid")).isEmpty();

        verifyNoInteractions(projectService);
    }
}
