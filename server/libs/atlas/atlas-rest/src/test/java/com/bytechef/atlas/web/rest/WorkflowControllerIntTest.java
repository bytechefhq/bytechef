
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

package com.bytechef.atlas.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.web.rest.config.WorkflowRestTestConfiguration;
import com.bytechef.atlas.web.rest.model.WorkflowModel;
import com.bytechef.atlas.workflow.WorkflowFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    WorkflowRestTestConfiguration.class
})
@WebFluxTest(WorkflowController.class)
public class WorkflowControllerIntTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testDeleteWorkflow() {
        try {
            this.webTestClient
                .delete()
                .uri("/workflows/1")
                .exchange()
                .expectStatus()
                .isOk();
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
            Workflow workflow = getWorkflow();

            when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

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
        Workflow workflow = getWorkflow();

        when(workflowService.getWorkflows()).thenReturn(List.of(workflow));

        try {
            this.webTestClient
                .get()
                .uri("/workflows")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(WorkflowModel.class);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostWorkflow() {
        Workflow workflow = getWorkflow();
        WorkflowModel workflowModel = new WorkflowModel()
            .definition("""
                {
                    "tasks": []
                }
                """)
            .format(WorkflowModel.FormatEnum.JSON);

        when(workflowService.add(any())).thenReturn(workflow);

        try {
            assert workflow.getId() != null;
            this.webTestClient
                .post()
                .uri("/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workflowModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.definition")
                .isEqualTo(workflow.getDefinition())
                .jsonPath("$.format")
                .isEqualTo(workflow.getFormat()
                    .toString())
                .jsonPath("$.id")
                .isEqualTo(workflow.getId());
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutWorkflow() {
        Workflow workflow = getWorkflow();
        WorkflowModel workflowModel = new WorkflowModel()
            .id("1")
            .definition(
                """
                    {
                        "label": "label",
                        "tasks": []
                    }
                    """);

        workflow.setDefinition(
            """
                {
                    "label": "label",
                    "tasks": []
                }
                """);

        when(workflowService.update(workflow)).thenReturn(workflow);

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
                .jsonPath("$.definition")
                .isEqualTo(workflow.getDefinition())
                .jsonPath("$.format")
                .isEqualTo(workflow.getFormat()
                    .toString())
                .jsonPath("$.id")
                .isEqualTo(workflow.getId());
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    private static Workflow getWorkflow() {
        Workflow workflow = new Workflow();

        workflow.setId("1");
        workflow.setDefinition("""
            {
                "tasks": []
            }
            """);
        workflow.setFormat(WorkflowFormat.JSON);

        return workflow;
    }
}
