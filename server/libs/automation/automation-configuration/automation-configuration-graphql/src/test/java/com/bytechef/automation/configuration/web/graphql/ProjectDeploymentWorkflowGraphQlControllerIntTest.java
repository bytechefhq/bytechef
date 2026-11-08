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

package com.bytechef.automation.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class,
    ProjectDeploymentWorkflowGraphQlController.class
})
@ExtendWith(ObjectMapperSetupExtension.class)
@GraphQlTest(
    controllers = ProjectDeploymentWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.webhook.url=http://localhost/webhooks",
        "spring.graphql.schema.inspection.enabled=false",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AutomationConfigurationGraphQlConfigurationSharedMocks
class ProjectDeploymentWorkflowGraphQlControllerIntTest {

    private static final UUID PROJECT_WORKFLOW_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ProjectDeploymentService projectDeploymentService;

    @MockitoBean
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @MockitoBean
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WorkflowService workflowService;

    @Test
    void testWorkspaceChatWorkflowsUsesTheEnabledHostedChatTrigger() {
        mockChatWorkflow("""
            {
                "label": "Chat Workflow",
                "triggers": [
                    {"name": "trigger_1", "type": "chat/v1/newChatRequest", "disabled": true},
                    {"name": "trigger_2", "type": "webhook/v1/autoRespondingWithHTTP200"},
                    {"name": "trigger_3", "type": "chat/v1/newChatRequest", "parameters": {"mode": 1}}
                ],
                "tasks": []
            }
            """);

        graphQlTester
            .document("""
                query {
                    workspaceChatWorkflows(workspaceId: 1, environmentId: 2) {
                        workflowExecutionId
                    }
                }
                """)
            .execute()
            .path("workspaceChatWorkflows[*].workflowExecutionId")
            .entityList(String.class)
            .containsExactly(
                WorkflowExecutionId.of(PlatformType.AUTOMATION, 10L, PROJECT_WORKFLOW_UUID.toString(), "trigger_3")
                    .toString());
    }

    @Test
    void testWorkspaceChatWorkflowsLeavesOutWorkflowWithOnlyDisabledChatTrigger() {
        mockChatWorkflow("""
            {
                "label": "Chat Workflow",
                "triggers": [
                    {"name": "trigger_1", "type": "chat/v1/newChatRequest", "disabled": true},
                    {"name": "trigger_2", "type": "webhook/v1/autoRespondingWithHTTP200"}
                ],
                "tasks": []
            }
            """);

        graphQlTester
            .document("""
                query {
                    workspaceChatWorkflows(workspaceId: 1, environmentId: 2) {
                        workflowExecutionId
                    }
                }
                """)
            .execute()
            .path("workspaceChatWorkflows")
            .entityList(Object.class)
            .hasSize(0);
    }

    private void mockChatWorkflow(String definition) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(10L);
        projectDeployment.setName("Chat Deployment");
        projectDeployment.setProjectId(100L);
        projectDeployment.setProjectVersion(1);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setProjectDeploymentId(10L);
        projectDeploymentWorkflow.setWorkflowId("workflow-1");

        Project project = new Project();

        project.setId(100L);
        project.setName("Chat Project");

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getName()).thenReturn("newChatRequest");
        when(triggerDefinition.getType()).thenReturn(TriggerType.STATIC_WEBHOOK);

        when(environmentService.getEnvironment(2L)).thenReturn(Environment.PRODUCTION);
        when(projectDeploymentService.getProjectDeployments(false, Environment.PRODUCTION, null, null, 1L))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(List.of(10L)))
            .thenReturn(List.of(projectDeploymentWorkflow));
        when(workflowService.getWorkflows(List.of("workflow-1")))
            .thenReturn(List.of(new Workflow("workflow-1", definition, Workflow.Format.JSON)));
        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("workflow-1")))
            .thenReturn(List.of(new ProjectWorkflow(100L, 1, "workflow-1", PROJECT_WORKFLOW_UUID)));
        when(projectService.getProjects(List.of(100L))).thenReturn(List.of(project));
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(triggerDefinition);
    }
}
