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

package com.bytechef.platform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.web.rest.config.PlatformConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.web.rest.config.WorkflowConfigurationRestTestConfigurationSharedMocks;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = PlatformConfigurationRestTestConfiguration.class)
@WebMvcTest(WorkflowNodeTestOutputApiController.class)
@WorkflowConfigurationRestTestConfigurationSharedMocks
class WorkflowNodeTestOutputApiControllerIntTest {

    private static final long ENVIRONMENT_ID = 0L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testSaveWorkflowNodeTestOutputReturnsTheSavedOutput() {
        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput("workflow-1", "dataStorage_1", ENVIRONMENT_ID))
            .thenReturn(createWorkflowNodeTestOutput(7L, "workflow-1", "dataStorage_1"));

        webTestClient
            .put()
            .uri(
                "/internal/workflows/{id}/workflow-nodes/{workflowNodeName}/test-outputs?environmentId={environmentId}",
                "workflow-1", "dataStorage_1", ENVIRONMENT_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(7)
            .jsonPath("$.workflowId")
            .isEqualTo("workflow-1")
            .jsonPath("$.workflowNodeName")
            .isEqualTo("dataStorage_1");
    }

    @Test
    void testSaveWorkflowNodeTestOutputReturnsNoContentWhenTheTestProducedNoOutput() {
        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(eq("workflow-1"), eq("dataStorage_1"), anyLong()))
            .thenReturn(null);

        webTestClient
            .put()
            .uri(
                "/internal/workflows/{id}/workflow-nodes/{workflowNodeName}/test-outputs?environmentId={environmentId}",
                "workflow-1", "dataStorage_1", ENVIRONMENT_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent()
            .expectBody()
            .isEmpty();
    }

    @Test
    void testUploadWorkflowNodeSampleFilePassesTheFileToTheFacade() throws IOException {
        when(workflowNodeTestOutputFacade.saveWorkflowNodeSampleFile(
            eq("workflow-1"), eq("awsS3_1"), eq("sample.csv"), any(InputStream.class), eq(ENVIRONMENT_ID)))
                .thenReturn(createWorkflowNodeTestOutput(8L, "workflow-1", "awsS3_1"));

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder
            .part(
                "file", new ByteArrayResource("a,b\n1,2".getBytes(StandardCharsets.UTF_8)) {

                    @Override
                    public String getFilename() {
                        return "sample.csv";
                    }
                })
            .contentType(MediaType.parseMediaType("text/csv"));

        webTestClient
            .post()
            .uri(
                "/internal/workflows/{id}/workflow-nodes/{workflowNodeName}/test-outputs/sample-file" +
                    "?environmentId={environmentId}",
                "workflow-1", "awsS3_1", ENVIRONMENT_ID)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(8)
            .jsonPath("$.workflowNodeName")
            .isEqualTo("awsS3_1");

        ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeSampleFile(
            eq("workflow-1"), eq("awsS3_1"), eq("sample.csv"), inputStreamArgumentCaptor.capture(),
            eq(ENVIRONMENT_ID));

        InputStream inputStream = inputStreamArgumentCaptor.getValue();

        assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("a,b\n1,2");
    }

    private static WorkflowNodeTestOutput createWorkflowNodeTestOutput(
        long id, String workflowId, String workflowNodeName) {

        WorkflowNodeTestOutput workflowNodeTestOutput = new WorkflowNodeTestOutput();

        workflowNodeTestOutput.setId(id);
        workflowNodeTestOutput.setWorkflowId(workflowId);
        workflowNodeTestOutput.setWorkflowNodeName(workflowNodeName);

        return workflowNodeTestOutput;
    }
}
