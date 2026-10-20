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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Pins the shape of the {@code getWorkspaceChatWorkflows} listing and the all-environments lookup built on it. The
 * {@code WORKFLOW_VIEW} gate is the {@code @PreAuthorize} expression on the implementation; a direct call cannot fire
 * an annotation.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ProjectDeploymentFacadeChatWorkflowTest {

    private static final String CHAT_WORKFLOW_DEFINITION =
        "{\"label\":\"%s\",\"triggers\":[{\"name\":\"chat_1\",\"type\":\"chat/v1/newChatMessage\"}],\"tasks\":[]}";

    private static final long ENVIRONMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 10L;
    private static final long PROJECT_WORKFLOW_ID = 1000L;
    private static final String WORKFLOW_ID = "wf-1";
    private static final long WORKSPACE_ID = 1L;

    private final ApplicationProperties applicationProperties = Mockito.mock(ApplicationProperties.class);
    private final EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);
    private final ProjectDeploymentService projectDeploymentService = Mockito.mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        Mockito.mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = Mockito.mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = Mockito.mock(ProjectWorkflowService.class);
    private final TriggerDefinitionService triggerDefinitionService = Mockito.mock(TriggerDefinitionService.class);
    private final WorkflowService workflowService = Mockito.mock(WorkflowService.class);

    @Test
    void testListingCarriesProjectWorkflowIdAndWorkflowId() {
        stubOneChatWorkflow();

        List<ChatWorkflow> chatWorkflows = createProjectDeploymentFacade()
            .getWorkspaceChatWorkflows(WORKSPACE_ID, ENVIRONMENT_ID);

        assertThat(chatWorkflows).hasSize(1);

        ChatWorkflow chatWorkflow = chatWorkflows.getFirst();

        assertThat(chatWorkflow.projectWorkflowId()).isEqualTo(PROJECT_WORKFLOW_ID);
        assertThat(chatWorkflow.workflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(chatWorkflow.projectName()).isEqualTo("Shared");
        assertThat(chatWorkflow.workflowLabel()).isEqualTo("Shared chat");
    }

    @Test
    void testNullEnvironmentListsEveryEnvironment() {
        stubOneChatWorkflow();

        List<ChatWorkflow> chatWorkflows = createProjectDeploymentFacade()
            .getWorkspaceChatWorkflows(WORKSPACE_ID, null);

        assertThat(chatWorkflows).hasSize(1);

        Mockito.verify(environmentService, Mockito.never())
            .getEnvironment(ArgumentMatchers.anyLong());
        Mockito.verify(projectDeploymentService)
            .getProjectDeployments(
                ArgumentMatchers.eq(false), ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(), ArgumentMatchers.eq(WORKSPACE_ID));
    }

    @Test
    void testFindWorkspaceChatWorkflowReturnsTheMatchingListingRow() {
        stubOneChatWorkflow();

        ProjectDeploymentFacadeImpl facade = createProjectDeploymentFacade();

        ChatWorkflow listedChatWorkflow = facade.getWorkspaceChatWorkflows(WORKSPACE_ID, null)
            .getFirst();

        assertThat(facade.findWorkspaceChatWorkflow(WORKSPACE_ID, listedChatWorkflow.workflowExecutionId()))
            .contains(listedChatWorkflow);
        assertThat(listedChatWorkflow.projectDeploymentId()).isEqualTo(PROJECT_DEPLOYMENT_ID);
    }

    @Test
    void testFindWorkspaceChatWorkflowIsEmptyForAnUnknownExecutionId() {
        stubOneChatWorkflow();

        String unknownWorkflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, UUID.randomUUID()
                .toString(),
            "chat_1")
            .toString();

        assertThat(createProjectDeploymentFacade().findWorkspaceChatWorkflow(WORKSPACE_ID, unknownWorkflowExecutionId))
            .isEmpty();
    }

    @Test
    void testFindWorkspaceChatWorkflowIsEmptyForANonIdString() {
        stubOneChatWorkflow();

        assertThat(createProjectDeploymentFacade().findWorkspaceChatWorkflow(WORKSPACE_ID, "not-an-id")).isEmpty();
    }

    @Test
    void testFindWorkspaceChatWorkflowIsEmptyForEmptyListing() {
        Mockito
            .when(
                projectDeploymentService.getProjectDeployments(
                    ArgumentMatchers.eq(false), ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(List.of());

        assertThat(createProjectDeploymentFacade().findWorkspaceChatWorkflow(WORKSPACE_ID, "anything")).isEmpty();
    }

    private StubbedChatWorkflow stubOneChatWorkflow() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setId(PROJECT_DEPLOYMENT_ID);
        projectDeployment.setProjectId(1L);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setId(100L);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);
        projectDeploymentWorkflow.setWorkflowId(WORKFLOW_ID);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(PROJECT_WORKFLOW_ID);

        projectWorkflow.setUuid(UUID.randomUUID());
        projectWorkflow.setWorkflowId(WORKFLOW_ID);

        Project project = new Project();

        project.setId(1L);
        project.setName("Shared");
        project.setWorkspaceId(WORKSPACE_ID);

        Mockito.when(environmentService.getEnvironment(ArgumentMatchers.anyLong()))
            .thenReturn(Environment.PRODUCTION);
        Mockito
            .when(
                projectDeploymentService.getProjectDeployments(
                    ArgumentMatchers.eq(false), ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(List.of(projectDeployment));
        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ArgumentMatchers.anyList()))
            .thenReturn(List.of(projectDeploymentWorkflow));
        Mockito.when(workflowService.getWorkflows(ArgumentMatchers.anyList()))
            .thenReturn(
                List.of(
                    new Workflow(
                        WORKFLOW_ID, CHAT_WORKFLOW_DEFINITION.formatted("Shared chat"), Workflow.Format.JSON)));
        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflows(ArgumentMatchers.anyList()))
            .thenReturn(List.of(projectWorkflow));
        Mockito.when(projectService.getProjects(ArgumentMatchers.anyList()))
            .thenReturn(List.of(project));

        TriggerDefinition triggerDefinition = Mockito.mock(TriggerDefinition.class);

        Mockito.when(triggerDefinition.getType())
            .thenReturn(TriggerType.STATIC_WEBHOOK);
        Mockito.when(triggerDefinition.getName())
            .thenReturn("newChatMessage");
        Mockito
            .when(
                triggerDefinitionService.getTriggerDefinition(
                    ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyString()))
            .thenReturn(triggerDefinition);

        return new StubbedChatWorkflow(projectDeployment, projectDeploymentWorkflow);
    }

    private ProjectDeploymentFacadeImpl createProjectDeploymentFacade() {
        Mockito.when(applicationProperties.getWebhookUrl())
            .thenReturn("http://localhost/webhooks/{id}");

        return new ProjectDeploymentFacadeImpl(
            null, null, environmentService, null, null, null, null, projectDeploymentService,
            projectDeploymentWorkflowService, projectService, projectWorkflowService, null, triggerDefinitionService,
            null, null, applicationProperties, null, workflowService, List.of());
    }

    private record StubbedChatWorkflow(
        ProjectDeployment projectDeployment, ProjectDeploymentWorkflow projectDeploymentWorkflow) {
    }
}
