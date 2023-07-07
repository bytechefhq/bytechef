
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.web.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.helios.configuration.web.rest.config.ProjectConfigurationRestTestConfiguration;
import com.bytechef.helios.configuration.web.rest.model.WorkflowModel;
import com.bytechef.hermes.configuration.dto.WorkflowDTO;
import com.bytechef.hermes.configuration.facade.WorkflowFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ProjectConfigurationRestTestConfiguration.class)
@WebMvcTest(ProjectWorkflowController.class)
public class ProjectWorkflowControllerIntTest {

    public static final String DEFINITION = """
        {
            "label": "label",
            "tasks": [
                {
                    "name": "name",
                    "type": "type"
                }
            ]
        }
        """;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @Autowired
    private WorkflowFacade workflowFacade;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    public void setup() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testDeleteWorkflow() {
        try {
            this.webTestClient
                .delete()
                .uri("/core/workflows/1")
                .exchange()
                .expectStatus()
                .isEqualTo(204);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        verify(workflowService).delete(argument.capture());

        Assertions.assertEquals("1", argument.getValue());
    }

    @Test
    public void testGetWorkflow() {
        try {
            when(workflowFacade.getWorkflow("1")).thenReturn(new WorkflowDTO(Collections.emptyList(), getWorkflow()));

            this.webTestClient
                .get()
                .uri("/core/workflows/1")
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
    public void testGetWorkflows() throws JsonProcessingException {
        when(workflowFacade.getWorkflows()).thenReturn(
            List.of(new WorkflowDTO(Collections.emptyList(), getWorkflow())));

        try {
            this.webTestClient
                .get()
                .uri("/core/workflows")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(WorkflowModel.class)
                .hasSize(1);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutWorkflow() throws JsonProcessingException {
        Workflow workflow = getWorkflow();

        WorkflowModel workflowModel = new WorkflowModel()
            .definition(DEFINITION);

        when(workflowFacade.update("1", DEFINITION)).thenReturn(new WorkflowDTO(Collections.emptyList(), workflow));

        Workflow.Format format = workflow.getFormat();

        try {
            this.webTestClient
                .put()
                .uri("/core/workflows/1")
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
                .isEqualTo(workflow.getId())
                .jsonPath("$.label")
                .isEqualTo(workflow.getLabel())
                .jsonPath("$.tasks")
                .isArray()
                .jsonPath("$.tasks[0].name")
                .isEqualTo("name")
                .jsonPath("$.tasks[0].type")
                .isEqualTo("type");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    private static Workflow getWorkflow() throws JsonProcessingException {
        return new Workflow(
            DEFINITION, Workflow.Format.JSON, "1",
            OBJECT_MAPPER.readValue(DEFINITION, new TypeReference<>() {}), Map.of());
    }
}
