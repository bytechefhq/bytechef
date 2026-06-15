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

package com.bytechef.platform.workflow.coordinator.trigger.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class TriggerCompletionHandlerTest {

    private static final String TRIGGER_NAME = "trigger1";
    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID =
        WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "workflow-uuid", TRIGGER_NAME);

    @Mock
    private FileEntry fileEntry;

    @Mock
    private JobPrincipalAccessor jobPrincipalAccessor;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private PrincipalJobFacade principalJobFacade;

    @Mock
    private TriggerExecution triggerExecution;

    @Mock
    private TriggerExecutionService triggerExecutionService;

    @Mock
    private TriggerFileStorage triggerFileStorage;

    @Mock
    private TriggerStateService triggerStateService;

    private TriggerCompletionHandler triggerCompletionHandler;

    @BeforeEach
    void setUp() {
        triggerCompletionHandler = new TriggerCompletionHandler(
            jobPrincipalAccessorRegistry, principalJobFacade, triggerExecutionService, triggerFileStorage,
            triggerStateService);

        when(triggerExecution.getId()).thenReturn(1L);
        when(triggerExecution.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        doReturn(Map.of("existingKey", "existingValue"))
            .when(jobPrincipalAccessor)
            .getInputMap(1L, "workflow-uuid");
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenReturn("workflow-id");
        doReturn(Map.of("metaKey", "metaValue"))
            .when(jobPrincipalAccessor)
            .getMetadataMap(1L);
    }

    @Test
    void testHandleNullOutputCreatesSingleJobWithEmptyMap() {
        stubName();
        when(triggerExecution.getOutput()).thenReturn(null);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(1);

        assertEquals(Map.of(), jobParametersDTOs.get(0)
            .getInputs()
            .get(TRIGGER_NAME));
    }

    @Test
    void testHandleNonBatchCollectionFansOutJobPerElement() {
        stubName();
        stubOutput(List.of("first", "second"));
        when(triggerExecution.isBatch()).thenReturn(false);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(2);

        assertEquals("first", triggerValue(jobParametersDTOs.get(0)));
        assertEquals("second", triggerValue(jobParametersDTOs.get(1)));
    }

    @Test
    void testHandleNonBatchNonCollectionCreatesSingleJob() {
        stubName();
        stubOutput("singleValue");
        when(triggerExecution.isBatch()).thenReturn(false);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(1);

        assertEquals("singleValue", triggerValue(jobParametersDTOs.get(0)));
        assertEquals("existingValue", jobParametersDTOs.get(0)
            .getInputs()
            .get("existingKey"));
    }

    @Test
    void testHandleBatchCollectionCreatesSingleJobWithWholeCollection() {
        List<String> output = List.of("first", "second");

        stubName();
        stubOutput(output);
        when(triggerExecution.isBatch()).thenReturn(true);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(1);

        assertEquals(output, triggerValue(jobParametersDTOs.get(0)));
    }

    @Test
    void testHandleBatchEmptyCollectionCreatesNoJob() {
        stubOutput(List.of());
        when(triggerExecution.isBatch()).thenReturn(true);

        triggerCompletionHandler.handle(triggerExecution);

        verify(principalJobFacade, never()).createJob(any(), eq(1L), eq(PlatformType.AUTOMATION));
    }

    @Test
    void testHandleBatchNonCollectionCreatesNoJob() {
        stubOutput("singleValue");
        when(triggerExecution.isBatch()).thenReturn(true);

        triggerCompletionHandler.handle(triggerExecution);

        verify(principalJobFacade, never()).createJob(any(), eq(1L), eq(PlatformType.AUTOMATION));
    }

    private List<JobParametersDTO> captureCreatedJobs(int expectedCount) {
        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(principalJobFacade, times(expectedCount))
            .createJob(captor.capture(), eq(1L), eq(PlatformType.AUTOMATION));

        return captor.getAllValues();
    }

    private void stubCreateJob() {
        when(principalJobFacade.createJob(any(), eq(1L), eq(PlatformType.AUTOMATION)))
            .thenReturn(5L);
    }

    private void stubName() {
        when(triggerExecution.getName()).thenReturn(TRIGGER_NAME);
    }

    private void stubOutput(Object output) {
        when(triggerExecution.getOutput()).thenReturn(fileEntry);
        when(triggerFileStorage.readTriggerExecutionOutput(fileEntry)).thenReturn(output);
    }

    private static Object triggerValue(JobParametersDTO jobParametersDTO) {
        return jobParametersDTO.getInputs()
            .get(TRIGGER_NAME);
    }
}
