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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.web.graphql.config.AutomationA2aGraphQlConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.web.graphql.config.AutomationA2aGraphQlTestConfiguration;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
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
    A2aProjectGraphQlController.class
})
@GraphQlTest(
    controllers = A2aProjectGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/"
    })
@AutomationA2aGraphQlConfigurationSharedMocks
class A2aProjectGraphQlControllerIntTest {

    @Autowired
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectService a2aProjectService;

    @Autowired
    private A2aProjectWorkflowService a2aProjectWorkflowService;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Test
    @WithMockUser
    void testA2aProjectsByServerIdResolvesProjectFieldsThroughTheDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(42L);
        projectDeployment.setProjectVersion(3);

        when(a2aProjectService.getA2aServerA2aProjects(7L)).thenReturn(List.of(createA2aProject(11L, 100L, 7L)));
        when(projectDeploymentService.getProjectDeployment(100L)).thenReturn(projectDeployment);

        graphQlTester
            .document("""
                query {
                    a2aProjectsByServerId(a2aServerId: "7") {
                        id
                        projectId
                        projectVersion
                    }
                }
                """)
            .execute()
            .path("a2aProjectsByServerId[0].id")
            .entity(String.class)
            .isEqualTo("11")
            .path("a2aProjectsByServerId[0].projectId")
            .entity(String.class)
            .isEqualTo("42")
            .path("a2aProjectsByServerId[0].projectVersion")
            .entity(Integer.class)
            .isEqualTo(3);

        verify(a2aProjectService).getA2aServerA2aProjects(7L);
    }

    @Test
    @WithMockUser
    void testA2aProjectsByServerIdResolvesWorkflowIdsThroughTheDeploymentWorkflows() {
        when(a2aProjectService.getA2aServerA2aProjects(7L)).thenReturn(List.of(createA2aProject(11L, 100L, 7L)));
        when(a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(11L))
            .thenReturn(List.of(new A2aProjectWorkflow(11L, 201L), new A2aProjectWorkflow(11L, 202L)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(201L))
            .thenReturn(createProjectDeploymentWorkflow("wf-1"));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(202L))
            .thenReturn(createProjectDeploymentWorkflow("wf-2"));

        graphQlTester
            .document("""
                query {
                    a2aProjectsByServerId(a2aServerId: "7") {
                        workflowIds
                    }
                }
                """)
            .execute()
            .path("a2aProjectsByServerId[0].workflowIds")
            .entityList(String.class)
            .containsExactly("wf-1", "wf-2");
    }

    @Test
    @WithMockUser
    void testA2aProjectsByServerIdReturnsNullProjectFieldsWithoutADeployment() {
        A2aProject a2aProject = new A2aProject();

        a2aProject.setId(12L);

        when(a2aProjectService.getA2aServerA2aProjects(7L)).thenReturn(List.of(a2aProject));

        graphQlTester
            .document("""
                query {
                    a2aProjectsByServerId(a2aServerId: "7") {
                        id
                        projectId
                        projectVersion
                    }
                }
                """)
            .execute()
            .path("a2aProjectsByServerId[0].id")
            .entity(String.class)
            .isEqualTo("12")
            .path("a2aProjectsByServerId[0].projectId")
            .valueIsNull()
            .path("a2aProjectsByServerId[0].projectVersion")
            .valueIsNull();

        verifyNoInteractions(projectDeploymentService);
    }

    @Test
    void testA2aProjectsByServerIdRequiresAuthentication() {
        graphQlTester
            .document("""
                query {
                    a2aProjectsByServerId(a2aServerId: "7") {
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
            .path("a2aProjectsByServerId")
            .valueIsNull();

        verifyNoInteractions(a2aProjectService);
    }

    @Test
    void testCreateA2aProjectDelegatesToTheFacade() {
        when(a2aProjectFacade.createA2aProject(7L, 42L, 3, List.of("wf-1")))
            .thenReturn(createA2aProject(11L, 100L, 7L));

        graphQlTester
            .document("""
                mutation {
                    createA2aProject(input: {
                        a2aServerId: "7",
                        projectId: "42",
                        projectVersion: 3,
                        selectedWorkflowIds: ["wf-1"]
                    }) {
                        id
                    }
                }
                """)
            .execute()
            .path("createA2aProject.id")
            .entity(String.class)
            .isEqualTo("11");

        verify(a2aProjectFacade).createA2aProject(7L, 42L, 3, List.of("wf-1"));
    }

    @Test
    void testUpdateA2aProjectDelegatesToTheFacade() {
        when(a2aProjectFacade.updateA2aProject(11L, List.of("wf-1", "wf-2")))
            .thenReturn(createA2aProject(11L, 100L, 7L));

        graphQlTester
            .document("""
                mutation {
                    updateA2aProject(id: "11", input: {selectedWorkflowIds: ["wf-1", "wf-2"]}) {
                        id
                    }
                }
                """)
            .execute()
            .path("updateA2aProject.id")
            .entity(String.class)
            .isEqualTo("11");

        verify(a2aProjectFacade).updateA2aProject(11L, List.of("wf-1", "wf-2"));
    }

    @Test
    void testDeleteA2aProjectDelegatesToTheFacade() {
        graphQlTester
            .document("""
                mutation {
                    deleteA2aProject(id: "11")
                }
                """)
            .execute()
            .path("deleteA2aProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(a2aProjectFacade).deleteA2aProject(11L);
    }

    private static A2aProject createA2aProject(long id, long projectDeploymentId, long a2aServerId) {
        A2aProject a2aProject = new A2aProject(projectDeploymentId, a2aServerId, 42L);

        a2aProject.setId(id);

        return a2aProject;
    }

    private static ProjectDeploymentWorkflow createProjectDeploymentWorkflow(String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }
}
