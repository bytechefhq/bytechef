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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TriggerExecutionOwnershipResolverTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_ID = 7L;
    private static final long TRIGGER_EXECUTION_ID = 33L;

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final TriggerExecutionService triggerExecutionService = mock(TriggerExecutionService.class);
    private final TriggerExecutionOwnershipResolver resolver = new TriggerExecutionOwnershipResolver(
        projectDeploymentService, projectService, triggerExecutionService);

    @Test
    void testResourceTypeMatchesTheTokenTheTriggerExecutionGuardNames() {
        assertThat(resolver.resourceType()).isEqualTo("TriggerExecution");
    }

    @Test
    void testResolveProjectIdOfAKnownTriggerExecution() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, "workflow-uuid", "trigger-1");

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .id(TRIGGER_EXECUTION_ID)
            .workflowExecutionId(workflowExecutionId)
            .build();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(PROJECT_DEPLOYMENT_ID);
        projectDeployment.setProjectId(PROJECT_ID);

        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(9L);

        when(triggerExecutionService.getTriggerExecutions(List.of(TRIGGER_EXECUTION_ID)))
            .thenReturn(List.of(triggerExecution));
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveProjectId(TRIGGER_EXECUTION_ID)).hasValue(PROJECT_ID);
    }

    @Test
    void testEmbeddedTriggerExecutionIsNotResolvedThroughAProjectDeploymentWithTheSameId() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.EMBEDDED, PROJECT_DEPLOYMENT_ID, "workflow-uuid", "trigger-1");

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .id(TRIGGER_EXECUTION_ID)
            .workflowExecutionId(workflowExecutionId)
            .build();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(PROJECT_DEPLOYMENT_ID);
        projectDeployment.setProjectId(PROJECT_ID);

        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(9L);

        when(triggerExecutionService.getTriggerExecutions(List.of(TRIGGER_EXECUTION_ID)))
            .thenReturn(List.of(triggerExecution));
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThat(resolver.resolveProjectId(TRIGGER_EXECUTION_ID)).isEmpty();
        assertThat(resolver.resolveOwner(TRIGGER_EXECUTION_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveProjectIdOfAnUnknownTriggerExecutionIsEmpty() {
        when(triggerExecutionService.getTriggerExecutions(List.of(TRIGGER_EXECUTION_ID))).thenReturn(List.of());

        assertThat(resolver.resolveProjectId(TRIGGER_EXECUTION_ID)).isEmpty();
    }
}
