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

package com.bytechef.automation.ai.a2a.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.web.graphql.config.AutomationA2aGraphQlConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.web.graphql.config.AutomationA2aGraphQlTestConfiguration;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationA2aGraphQlTestConfiguration.class,
    A2aProjectWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = A2aProjectWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/"
    })
@AutomationA2aGraphQlConfigurationSharedMocks
class A2aProjectWorkflowGraphQlControllerIntTest {

    @Autowired
    private A2aProjectWorkflowService a2aProjectWorkflowService;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @Test
    @WithMockUser
    void testA2aProjectWorkflowsByA2aProjectIdResolvesFieldsThroughTheDeploymentWorkflow() {
        A2aProjectWorkflow a2aProjectWorkflow = createA2aProjectWorkflow(901L, 11L, 201L);

        a2aProjectWorkflow.setSkillName("Summarize");
        a2aProjectWorkflow.setSkillDescription(" ");

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setWorkflowId("wf-1");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Summarizer");
        when(a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(11L)).thenReturn(List.of(a2aProjectWorkflow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(201L)).thenReturn(projectDeploymentWorkflow);
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);

        graphQlTester
            .document("""
                query {
                    a2aProjectWorkflowsByA2aProjectId(a2aProjectId: "11") {
                        id
                        workflowId
                        workflowLabel
                        skillName
                        skillDescription
                    }
                }
                """)
            .execute()
            .path("a2aProjectWorkflowsByA2aProjectId[0].id")
            .entity(String.class)
            .isEqualTo("901")
            .path("a2aProjectWorkflowsByA2aProjectId[0].workflowId")
            .entity(String.class)
            .isEqualTo("wf-1")
            .path("a2aProjectWorkflowsByA2aProjectId[0].workflowLabel")
            .entity(String.class)
            .isEqualTo("Summarizer")
            .path("a2aProjectWorkflowsByA2aProjectId[0].skillName")
            .entity(String.class)
            .isEqualTo("Summarize")
            .path("a2aProjectWorkflowsByA2aProjectId[0].skillDescription")
            .valueIsNull();

        verify(a2aProjectWorkflowService).getA2aProjectA2aProjectWorkflows(11L);
    }

    @Test
    @WithMockUser
    void testA2aProjectWorkflowsByA2aProjectIdReturnsNullWorkflowFieldsWithoutADeploymentWorkflow() {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow();

        a2aProjectWorkflow.setId(902L);

        when(a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(11L)).thenReturn(List.of(a2aProjectWorkflow));

        graphQlTester
            .document("""
                query {
                    a2aProjectWorkflowsByA2aProjectId(a2aProjectId: "11") {
                        id
                        workflowId
                        workflowLabel
                    }
                }
                """)
            .execute()
            .path("a2aProjectWorkflowsByA2aProjectId[0].id")
            .entity(String.class)
            .isEqualTo("902")
            .path("a2aProjectWorkflowsByA2aProjectId[0].workflowId")
            .valueIsNull()
            .path("a2aProjectWorkflowsByA2aProjectId[0].workflowLabel")
            .valueIsNull();

        verifyNoInteractions(projectDeploymentWorkflowService, workflowService);
    }

    @Test
    void testA2aProjectWorkflowsByA2aProjectIdRequiresAuthentication() {
        graphQlTester
            .document("""
                query {
                    a2aProjectWorkflowsByA2aProjectId(a2aProjectId: "11") {
                        id
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .singleElement()
                .extracting(ResponseError::getErrorType)
                .isEqualTo(ErrorType.UNAUTHORIZED))
            .path("a2aProjectWorkflowsByA2aProjectId")
            .valueIsNull();

        verifyNoInteractions(a2aProjectWorkflowService);
    }

    @Test
    void testUpdateA2aProjectWorkflowParametersPassesOnlyTheProvidedFields() {
        A2aProjectWorkflow updatedA2aProjectWorkflow = createA2aProjectWorkflow(901L, 11L, 201L);

        updatedA2aProjectWorkflow.setSkillName("Summarize");

        when(a2aProjectWorkflowService.updateSkill(901L, "Summarize", null)).thenReturn(updatedA2aProjectWorkflow);

        graphQlTester
            .document("""
                mutation {
                    updateA2aProjectWorkflowParameters(id: "901", input: {skillName: "Summarize"}) {
                        id
                        skillName
                    }
                }
                """)
            .execute()
            .path("updateA2aProjectWorkflowParameters.id")
            .entity(String.class)
            .isEqualTo("901")
            .path("updateA2aProjectWorkflowParameters.skillName")
            .entity(String.class)
            .isEqualTo("Summarize");

        verify(a2aProjectWorkflowService).updateSkill(901L, "Summarize", null);
    }

    @Test
    void testUpdateA2aProjectWorkflowEnabledDisablesTheWorkflow() {
        A2aProjectWorkflow disabledA2aProjectWorkflow = createA2aProjectWorkflow(901L, 11L, 201L);

        disabledA2aProjectWorkflow.setEnabled(false);

        when(a2aProjectWorkflowService.updateEnabled(901L, false)).thenReturn(disabledA2aProjectWorkflow);

        graphQlTester
            .document("""
                mutation {
                    updateA2aProjectWorkflowEnabled(id: "901", enabled: false) {
                        id
                        enabled
                    }
                }
                """)
            .execute()
            .path("updateA2aProjectWorkflowEnabled.id")
            .entity(String.class)
            .isEqualTo("901")
            .path("updateA2aProjectWorkflowEnabled.enabled")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(a2aProjectWorkflowService).updateEnabled(901L, false);
    }

    @Test
    void testUpdateA2aProjectWorkflowEnabledReEnablesTheWorkflow() {
        A2aProjectWorkflow enabledA2aProjectWorkflow = createA2aProjectWorkflow(901L, 11L, 201L);

        when(a2aProjectWorkflowService.updateEnabled(901L, true)).thenReturn(enabledA2aProjectWorkflow);

        graphQlTester
            .document("""
                mutation {
                    updateA2aProjectWorkflowEnabled(id: "901", enabled: true) {
                        enabled
                    }
                }
                """)
            .execute()
            .path("updateA2aProjectWorkflowEnabled.enabled")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(a2aProjectWorkflowService).updateEnabled(901L, true);
    }

    private static A2aProjectWorkflow createA2aProjectWorkflow(
        long id, long a2aProjectId, long projectDeploymentWorkflowId) {

        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(a2aProjectId, projectDeploymentWorkflowId);

        a2aProjectWorkflow.setId(id);

        return a2aProjectWorkflow;
    }
}
