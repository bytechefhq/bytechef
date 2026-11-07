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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ErrorWorkflowDispatch;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ErrorWorkflowJobStatusApplicationEventListenerTest {

    @Mock
    private ErrorWorkflowResolver errorWorkflowResolver;

    @Mock
    private JobService jobService;

    @Mock
    private PrincipalJobFacade principalJobFacade;

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Test
    void testDoesNotDispatchForAnErrorHandlerRun() {
        Job job = new Job();

        job.setId(11L);
        job.setMetadata(Map.of("errorHandlerFor", "10"));

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);

        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        Mockito.verifyNoInteractions(principalJobFacade);
        Mockito.verifyNoInteractions(errorWorkflowResolver);
    }

    @Test
    void testIgnoresNonFailedStatuses() {
        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.COMPLETED));

        Mockito.verifyNoInteractions(jobService);
        Mockito.verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testSkipsWhenNoHandlerConfigured() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(3L));
        Mockito.when(errorWorkflowResolver.resolve(Mockito.anyLong(), Mockito.eq("wf-1")))
            .thenReturn(Optional.empty());

        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        Mockito.verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testDispatchFailureIsSwallowed() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenThrow(new IllegalStateException("boom"));

        Assertions.assertDoesNotThrow(
            () -> listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED)));
    }

    @Test
    void testDoesNotDispatchForSubflowChildJob() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setParentTaskExecutionId(99L);
        job.setMetadata(Map.of());

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);

        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        Mockito.verifyNoInteractions(principalJobFacade);
        Mockito.verifyNoInteractions(errorWorkflowResolver);
        Mockito.verifyNoInteractions(principalJobService);
    }

    @Test
    void testUnsupportedOperationDoesNotDispatch() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(3L));
        Mockito.when(errorWorkflowResolver.resolve(Mockito.anyLong(), Mockito.eq("wf-1")))
            .thenThrow(new UnsupportedOperationException("error workflow dispatch not supported"));

        Assertions.assertDoesNotThrow(
            () -> listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED)));

        Mockito.verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testUnsupportedOperationIsLoggedOnlyOnce() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(3L));
        Mockito.when(errorWorkflowResolver.resolve(Mockito.anyLong(), Mockito.eq("wf-1")))
            .thenThrow(new UnsupportedOperationException("error workflow dispatch not supported"));

        ErrorWorkflowJobStatusApplicationEventListener eventListener = listener();

        eventListener.onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        eventListener.onApplicationEvent(new JobStatusApplicationEvent(12L, Job.Status.FAILED));

        Mockito.verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testDispatchUsesLeafFailingTaskExecutionError() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        ErrorWorkflowDispatch dispatch = new ErrorWorkflowDispatch(
            "handler-wf", 1L, 2L, "wf-1", "Failing Workflow", "STAGING", "newWorkflowError_1");

        TaskExecution ancestorTaskExecution = Mockito.mock(TaskExecution.class);

        Mockito.when(ancestorTaskExecution.getStatus())
            .thenReturn(TaskExecution.Status.FAILED);

        TaskExecution leafTaskExecution = Mockito.mock(TaskExecution.class);

        Mockito.when(leafTaskExecution.getStatus())
            .thenReturn(TaskExecution.Status.FAILED);
        Mockito.when(leafTaskExecution.getError())
            .thenReturn(new ExecutionError("leaf failure", List.of("at Leaf.run")));
        Mockito.when(leafTaskExecution.getName())
            .thenReturn("leafNode");

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(3L));
        Mockito.when(errorWorkflowResolver.resolve(3L, "wf-1"))
            .thenReturn(Optional.of(dispatch));
        Mockito.when(taskExecutionService.getJobTaskExecutions(11L))
            .thenReturn(List.of(ancestorTaskExecution, leafTaskExecution));

        listener().onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        Mockito.verify(principalJobFacade)
            .createJob(captor.capture(), Mockito.eq(3L), Mockito.eq(PlatformType.AUTOMATION));

        JobParametersDTO jobParametersDTO = captor.getValue();

        Assertions.assertEquals("handler-wf", jobParametersDTO.getWorkflowId());
        Assertions.assertEquals(
            "11", jobParametersDTO.getMetadata()
                .get(ErrorWorkflowJobStatusApplicationEventListener.ERROR_HANDLER_FOR));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) jobParametersDTO.getInputs()
            .get("newWorkflowError_1");

        Assertions.assertNotNull(payload, "payload must be namespaced under the error trigger's node name");

        @SuppressWarnings("unchecked")
        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) execution.get("error");

        Assertions.assertEquals("leaf failure", error.get("message"));
        Assertions.assertEquals("leafNode", execution.get("lastTaskExecuted"));

        Assertions.assertEquals("STAGING", payload.get("environment"));

        Assertions.assertEquals(
            "newWorkflowError_1", jobParametersDTO.getInputs()
                .get(com.bytechef.platform.constant.JobInputConstants.TRIGGER_NAME_INPUT));
    }

    private ErrorWorkflowJobStatusApplicationEventListener listener() {
        return new ErrorWorkflowJobStatusApplicationEventListener(
            new ErrorWorkflowPayloadFactory("https://app.example.com"), errorWorkflowResolver, jobService,
            principalJobFacade, principalJobService, taskExecutionService, null);
    }
}
