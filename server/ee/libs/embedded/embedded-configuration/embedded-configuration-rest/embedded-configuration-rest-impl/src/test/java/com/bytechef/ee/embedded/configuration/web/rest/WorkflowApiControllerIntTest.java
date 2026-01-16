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

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestTestConfiguration;
import com.bytechef.ee.embedded.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
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
@ContextConfiguration(classes = EmbeddedConfigurationRestTestConfiguration.class)
@WebMvcTest(WorkflowApiController.class)
@EmbeddedConfigurationRestConfigurationSharedMocks
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
    private AppEventService appEventService;

    @MockitoBean
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private IntegrationFacade integrationFacade;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @MockitoBean
    private IntegrationService integrationService;

    @MockitoBean
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @MockitoBean
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    private WebTestClient webTestClient;

    @MockitoBean
    private WorkflowFacade workflowFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private ComponentConnectionFacade componentConnectionFacade;

    @BeforeEach
    void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testGetWorkflow() {
        try {
            when(integrationWorkflowFacade.getIntegrationWorkflow("1"))
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

        verify(integrationWorkflowFacade).getIntegrationWorkflow("1");
    }

    @Test
    public void testGetIntegrationWorkflows() {
        try {
            IntegrationWorkflow integrationWorkflow = createTestIntegrationWorkflow(1L, "workflow1");

            IntegrationWorkflowDTO workflow = new IntegrationWorkflowDTO(
                new Workflow("workflow1", "{}", Workflow.Format.JSON), integrationWorkflow);

            when(integrationWorkflowFacade.getIntegrationWorkflows(1L))
                .thenReturn(List.of(workflow));

            this.webTestClient
                .get()
                .uri("/internal/integrations/1/workflows")
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

        verify(integrationWorkflowFacade).getIntegrationWorkflows(1L);
    }

    @Test
    public void testPostIntegrationWorkflows() {
        String definition = "{\"description\": \"My description\", \"label\": \"New Workflow\", \"tasks\": []}";

        WorkflowModel workflowModel = new WorkflowModel().definition(definition);
        IntegrationWorkflowDTO integrationWorkflowDTO =
            new IntegrationWorkflowDTO(new Workflow("id", definition, Workflow.Format.JSON),
                createTestIntegrationWorkflow(1L, "id"));

        when(integrationWorkflowFacade.addWorkflow(anyLong(), any()))
            .thenReturn(integrationWorkflowDTO.getIntegrationWorkflowId());

        try {
            this.webTestClient
                .post()
                .uri("/internal/integrations/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workflowModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Long.class)
                .isEqualTo(integrationWorkflowDTO.getIntegrationWorkflowId());
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(integrationWorkflowFacade).addWorkflow(anyLong(), eq(definition));
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

        verify(integrationWorkflowFacade).updateWorkflow("1", DEFINITION, 0);
    }

    private IntegrationWorkflowDTO getWorkflowDTO() {
        Workflow workflow = new Workflow("1", DEFINITION, Workflow.Format.JSON);

        List<WorkflowTask> tasks = workflow.getTasks();

        return new IntegrationWorkflowDTO(
            new com.bytechef.platform.configuration.dto.WorkflowDTO(
                workflow, List.of(new WorkflowTaskDTO(tasks.getFirst(), false, null, List.of())), List.of()),
            createTestIntegrationWorkflow(1L, "1"));
    }

    private IntegrationWorkflow createTestIntegrationWorkflow(Long id, String workflowId) {
        IntegrationWorkflow integrationWorkflow =
            new IntegrationWorkflow(1L, 1, workflowId, java.util.UUID.randomUUID());

        // Use reflection to set the id field since there's no setter
        try {
            java.lang.reflect.Field idField = IntegrationWorkflow.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(integrationWorkflow, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id field", e);
        }

        return integrationWorkflow;
    }
}
