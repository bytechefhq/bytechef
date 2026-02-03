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

package com.bytechef.automation.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestTestConfiguration;
import com.bytechef.automation.configuration.web.rest.model.CreateProjectWorkflow200ResponseModel;
import com.bytechef.automation.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationConfigurationRestTestConfiguration.class)
@WebMvcTest(WorkflowApiController.class)
@AutomationConfigurationRestConfigurationSharedMocks
public class WorkflowApiControllerIntTest {

    public static final String DEFINITION = """
        {
            "label": "label",
            "tasks": [
                {
                    "name": "airtable",
                    "type": "airtable/v1/create"
                }
            ]
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectCategoryFacade projectCategoryFacade;

    @MockitoBean
    private ProjectDeploymentFacade projectDeploymentFacade;

    @MockitoBean
    private ProjectFacade projectFacade;

    @MockitoBean
    private ProjectTagFacade projectTagFacade;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectWorkflowFacade projectWorkflowFacade;

    private WebTestClient webTestClient;

    @MockitoBean
    private WorkflowFacade workflowFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private ComponentConnectionFacade componentConnectionFacade;

    @MockitoBean
    private WorkspaceFacade workspaceFacade;

    @MockitoBean
    private WorkspaceService workspaceService;

    @BeforeEach
    public void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testGetWorkflow() {
        try {
            when(projectWorkflowFacade.getProjectWorkflow("1"))
                .thenReturn(getWorkflowDTO());

            this.webTestClient
                .get()
                .uri("/internal/workflows/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(WorkflowModel.class);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(projectWorkflowFacade).getProjectWorkflow("1");
    }

    @Test
    public void testGetProjectWorkflows() {
        try {
            ProjectWorkflow projectWorkflow = new ProjectWorkflow(1L, 1, "workflow1", UUID.randomUUID());
            // Use reflection or create a method to set the ID since there's no setter
            try {
                java.lang.reflect.Field idField = ProjectWorkflow.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(projectWorkflow, 1L);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set ID", e);
            }

            ProjectWorkflowDTO workflow = new ProjectWorkflowDTO(
                new Workflow("workflow1", "{}", Workflow.Format.JSON), projectWorkflow, false);

            when(projectWorkflowFacade.getProjectWorkflows(1L))
                .thenReturn(List.of(workflow));

            this.webTestClient
                .get()
                .uri("/internal/projects/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.[0].id")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(projectWorkflowFacade).getProjectWorkflows(1L);
    }

    @Test
    public void testPostProjectWorkflows() {
        String definition = "{\"description\": \"My description\", \"label\": \"New Workflow\", \"tasks\": []}";

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(1L, 1, "workflow1", UUID.randomUUID());
        // Use reflection to set the ID since there's no setter
        try {
            java.lang.reflect.Field idField = ProjectWorkflow.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(projectWorkflow, 1L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID", e);
        }
        WorkflowModel workflowModel = new WorkflowModel().definition(definition);

        when(projectWorkflowFacade.addWorkflow(anyLong(), any()))
            .thenReturn(projectWorkflow);

        this.webTestClient
            .post()
            .uri("/internal/projects/1/workflows")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflowModel)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CreateProjectWorkflow200ResponseModel.class)
            .value(response -> Assertions.assertEquals(1L, response.getProjectWorkflowId()));

        verify(projectWorkflowFacade).addWorkflow(anyLong(), any());
    }

    @Test
    public void testPutWorkflow() {
        WorkflowModel workflowModel = new WorkflowModel()
            .definition(DEFINITION)
            .version(0);

        try {
            this.webTestClient
                .put()
                .uri("/internal/workflows/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workflowModel)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(projectWorkflowFacade).updateWorkflow("1", DEFINITION, 0);
    }

    private ProjectWorkflowDTO getWorkflowDTO() {
        Workflow workflow = new Workflow("1", DEFINITION, Workflow.Format.JSON);

        List<WorkflowTask> tasks = workflow.getTasks();

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(1L, 1, "1", UUID.randomUUID());
        // Use reflection to set the ID since there's no setter
        try {
            java.lang.reflect.Field idField = ProjectWorkflow.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(projectWorkflow, 1L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID", e);
        }

        return new ProjectWorkflowDTO(
            new com.bytechef.platform.configuration.dto.WorkflowDTO(
                workflow, List.of(new WorkflowTaskDTO(tasks.getFirst(), false, null, List.of())), List.of()),
            projectWorkflow, false);
    }
}
