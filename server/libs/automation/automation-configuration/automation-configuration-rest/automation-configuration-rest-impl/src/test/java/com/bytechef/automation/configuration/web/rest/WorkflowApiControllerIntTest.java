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

package com.bytechef.automation.configuration.web.rest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectInstanceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.rest.config.ProjectConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import java.util.List;
import org.apache.commons.lang3.Validate;
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
@ContextConfiguration(classes = ProjectConfigurationRestTestConfiguration.class)
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
    private ProjectInstanceFacade projectInstanceFacade;

    @MockBean
    private ProjectFacade projectFacade;

    @MockBean
    private ProjectService projectService;

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
            when(workflowFacade.getWorkflow("1"))
                .thenReturn(getWorkflowDTO());

            this.webTestClient
                .get()
                .uri("/workflows/1")
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
    public void testGetWorkflows() {
        when(workflowService.getWorkflows(anyInt()))
            .thenReturn(List.of(getWorkflow()));

        try {
            this.webTestClient
                .get()
                .uri("/workflows")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(WorkflowBasicModel.class)
                .hasSize(1);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testPutWorkflow() {
        WorkflowModel workflowModel = new WorkflowModel()
            .definition(DEFINITION)
            .version(0);

        WorkflowDTO workflowDTO = getWorkflowDTO();

        when(projectFacade.updateWorkflow("1", DEFINITION, 0))
            .thenReturn(workflowDTO);

        Workflow.Format format = workflowDTO.format();

        try {
            this.webTestClient
                .put()
                .uri("/workflows/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workflowModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.format")
                .isEqualTo(format.toString())
                .jsonPath("$.id")
                .isEqualTo(Validate.notNull(workflowDTO.id(), "id"))
                .jsonPath("$.label")
                .isEqualTo(workflowDTO.label())
                .jsonPath("$.tasks")
                .isArray()
                .jsonPath("$.tasks[0].name")
                .isEqualTo("airtable")
                .jsonPath("$.tasks[0].type")
                .isEqualTo("airtable/v1/create");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    private Workflow getWorkflow() {
        return new Workflow("1", DEFINITION, Workflow.Format.JSON, 0);
    }

    private WorkflowDTO getWorkflowDTO() {
        Workflow workflow = new Workflow("1", DEFINITION, Workflow.Format.JSON, 0);

        return new WorkflowDTO(
            workflow, List.of(new WorkflowTaskDTO(workflow.getTasks()
                .getFirst(), List.of(), null)),
            List.of());
    }
}
