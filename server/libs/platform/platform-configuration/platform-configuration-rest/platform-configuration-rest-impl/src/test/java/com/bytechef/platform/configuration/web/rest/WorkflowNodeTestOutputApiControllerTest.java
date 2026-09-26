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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.web.rest.model.WorkflowNodeTestOutputModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
class WorkflowNodeTestOutputApiControllerTest {

    private static final long ENVIRONMENT_ID = 0L;

    private final ConversionService conversionService = mock(ConversionService.class);
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade = mock(WorkflowNodeTestOutputFacade.class);

    private WorkflowNodeTestOutputApiController workflowNodeTestOutputApiController;

    @BeforeEach
    void setUp() {
        workflowNodeTestOutputApiController = new WorkflowNodeTestOutputApiController(
            conversionService, workflowNodeTestOutputFacade, mock(WorkflowNodeTestOutputService.class));
    }

    @Test
    void testSaveWorkflowNodeTestOutputReturnsTheSavedOutput() {
        WorkflowNodeTestOutput workflowNodeTestOutput = mock(WorkflowNodeTestOutput.class);
        WorkflowNodeTestOutputModel workflowNodeTestOutputModel = new WorkflowNodeTestOutputModel();

        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput("workflow-1", "dataStorage_1", ENVIRONMENT_ID))
            .thenReturn(workflowNodeTestOutput);
        when(conversionService.convert(workflowNodeTestOutput, WorkflowNodeTestOutputModel.class))
            .thenReturn(workflowNodeTestOutputModel);

        ResponseEntity<WorkflowNodeTestOutputModel> responseEntity =
            workflowNodeTestOutputApiController.saveWorkflowNodeTestOutput(
                "workflow-1", "dataStorage_1", ENVIRONMENT_ID);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isSameAs(workflowNodeTestOutputModel);
    }

    @Test
    void testSaveWorkflowNodeTestOutputReturnsNoContentWhenTheTestProducedNoOutput() {
        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            eq("workflow-1"), eq("dataStorage_1"), any(Long.class)))
                .thenReturn(null);

        ResponseEntity<WorkflowNodeTestOutputModel> responseEntity =
            workflowNodeTestOutputApiController.saveWorkflowNodeTestOutput(
                "workflow-1", "dataStorage_1", ENVIRONMENT_ID);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(responseEntity.getBody()).isNull();

        verifyNoInteractions(conversionService);
    }
}
