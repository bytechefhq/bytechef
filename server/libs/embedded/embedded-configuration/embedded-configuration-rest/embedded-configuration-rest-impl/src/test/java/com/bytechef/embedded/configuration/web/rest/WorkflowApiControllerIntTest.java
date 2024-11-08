/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.configuration.web.rest;

import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.embedded.configuration.service.AppEventService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.configuration.web.rest.config.IntegrationConfigurationRestTestConfiguration;
import com.bytechef.embedded.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = IntegrationConfigurationRestTestConfiguration.class)
@WebMvcTest(WorkflowApiController.class)
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

    @MockBean
    private AppEventService appEventService;

    @MockBean
    private IntegrationFacade integrationFacade;

    @MockBean
    private IntegrationService integrationService;

    @MockBean
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    private WebTestClient webTestClient;

    @MockBean
    private WorkflowFacade workflowFacade;

    @MockBean
    private WorkflowService workflowService;

    @MockBean
    private WorkflowConnectionFacade workflowConnectionFacade;

    @BeforeEach
    public void setup() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testGetWorkflow() {
        try {
            when(integrationFacade.getIntegrationWorkflow("1"))
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
    }

    private IntegrationWorkflowDTO getWorkflowDTO() {
        Workflow workflow = new Workflow("1", DEFINITION, Workflow.Format.JSON);

        List<WorkflowTask> tasks = workflow.getTasks();

        return new IntegrationWorkflowDTO(
            new com.bytechef.platform.configuration.dto.WorkflowDTO(
                workflow, List.of(new WorkflowTaskDTO(tasks.getFirst(), List.of(), null)), List.of()),
            new IntegrationWorkflow(1));
    }
}
