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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
class TriggerExecutionVisibilityProviderTest {

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final TriggerExecutionService triggerExecutionService = mock(TriggerExecutionService.class);
    private final TriggerExecutionVisibilityProvider triggerExecutionVisibilityProvider =
        new TriggerExecutionVisibilityProvider(projectDeploymentService, projectService, triggerExecutionService);

    @Test
    void testTriggerExecutionInheritsTheProjectRecord() {
        Project project = new Project();

        project.setId(3L);
        project.setVisibility(ResourceVisibility.PRIVATE);

        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(3L);

        givenTriggerExecution(21L, PlatformType.AUTOMATION, 9L);
        when(projectDeploymentService.fetchProjectDeployment(9L)).thenReturn(Optional.of(projectDeployment));
        when(projectService.fetchProject(3L)).thenReturn(Optional.of(project));

        assertThat(triggerExecutionVisibilityProvider.resourceType()).isEqualTo("TriggerExecution");
        assertThat(triggerExecutionVisibilityProvider.visibilityResourceType()).isEqualTo("Project");
        assertThat(triggerExecutionVisibilityProvider.fetchVisibility(21L))
            .contains(new VisibilityRecord(3L, ResourceVisibility.PRIVATE, "ivica"));
    }

    @Test
    void testNonAutomationTriggerExecutionFailsClosed() {
        givenTriggerExecution(22L, PlatformType.EMBEDDED, 9L);

        assertThat(triggerExecutionVisibilityProvider.fetchVisibility(22L)).isEmpty();

        verify(projectDeploymentService, never()).fetchProjectDeployment(9L);
    }

    @Test
    void testUnknownTriggerExecutionFailsClosed() {
        when(triggerExecutionService.getTriggerExecutions(List.of(23L))).thenReturn(List.of());

        assertThat(triggerExecutionVisibilityProvider.fetchVisibility(23L)).isEmpty();
    }

    private void givenTriggerExecution(long id, PlatformType platformType, long jobPrincipalId) {
        TriggerExecution triggerExecution = mock(TriggerExecution.class);

        when(triggerExecution.getWorkflowExecutionId())
            .thenReturn(WorkflowExecutionId.of(platformType, jobPrincipalId, "workflow-uuid", "trigger_1"));
        when(triggerExecutionService.getTriggerExecutions(List.of(id))).thenReturn(List.of(triggerExecution));
    }
}
