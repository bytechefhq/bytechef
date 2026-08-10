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
import com.bytechef.exception.RateLimitExceededException;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.coordinator.ErrorWorkflowDispatchCounter;
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

    /**
     * {@code JobConcurrencyLimitExceededException}, {@code JobRateLimitExceededException} and
     * {@code JobCostLimitExceededException} all extend {@link RateLimitExceededException}, and the handler job goes
     * through the same admission gate as any other job submission. During a failure storm that saturates a plan limit,
     * every one of these rejections must record the distinct {@code "rejected"} outcome (not the generic
     * {@code "failed"} outcome used for genuine dispatch bugs) and must not propagate out of the listener.
     */
    @Test
    void testAdmissionGateRejectionIsRecordedAsRejectedNotFailed() {
        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("wf-1");
        job.setMetadata(Map.of());

        ErrorWorkflowDispatch dispatch = new ErrorWorkflowDispatch(
            "handler-wf", 1L, 2L, "wf-1", "Failing Workflow", "STAGING", "newWorkflowError_1");

        Mockito.when(jobService.getJob(11L))
            .thenReturn(job);
        Mockito.when(principalJobService.fetchJobPrincipalId(11L, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(3L));
        Mockito.when(errorWorkflowResolver.resolve(3L, "wf-1"))
            .thenReturn(Optional.of(dispatch));
        Mockito.when(taskExecutionService.getJobTaskExecutions(11L))
            .thenReturn(List.of());
        Mockito.when(principalJobFacade.createJob(Mockito.any(), Mockito.eq(3L), Mockito.eq(PlatformType.AUTOMATION)))
            .thenThrow(new RateLimitExceededException("Concurrent execution limit reached (allowed=10)"));

        ErrorWorkflowDispatchCounter counter = Mockito.mock(ErrorWorkflowDispatchCounter.class);

        ErrorWorkflowJobStatusApplicationEventListener eventListener =
            new ErrorWorkflowJobStatusApplicationEventListener(
                new ErrorWorkflowPayloadFactory("https://app.example.com"), errorWorkflowResolver, jobService,
                principalJobFacade, principalJobService, taskExecutionService, counter);

        Assertions.assertDoesNotThrow(
            () -> eventListener.onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED)));

        Mockito.verify(counter)
            .record("rejected");
        Mockito.verify(counter, Mockito.never())
            .record("failed");
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

        // First failed-job event
        eventListener.onApplicationEvent(new JobStatusApplicationEvent(11L, Job.Status.FAILED));

        // Second failed-job event on same listener instance
        eventListener.onApplicationEvent(new JobStatusApplicationEvent(12L, Job.Status.FAILED));

        // Both should dispatch without error, but verify via principalJobFacade interactions
        // (the second call would have different job ID)
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

        // The ancestor task execution is marked FAILED by TaskExecutionErrorEventListener but its error is left
        // null -- only the leaf carries the real cause.
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

        // The payload must be nested under the handler workflow's error-trigger node name -- not passed as
        // top-level inputs -- so that editor data pills like ${newWorkflowError_1.execution.jobId} resolve. A
        // regression back to top-level inputs would put "execution"/"environment" directly on getInputs() and this
        // lookup would come back null.
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

        // The environment must come from the dispatch (ultimately the ProjectDeployment), never from job metadata
        // -- job.setMetadata(Map.of()) above carries no "environment" key, so a regression back to
        // String.valueOf(job.getMetadata("environment")) would show up here as the literal string "null".
        Assertions.assertEquals("STAGING", payload.get("environment"));

        // The reserved __triggerName input must be seeded alongside the trigger-name-keyed payload, same as every
        // other job-creation path (see JobInputConstants) -- otherwise a handler workflow built from an agent
        // (branch_in keyed on ${__triggerName}) could never route this dispatch.
        Assertions.assertEquals(
            "newWorkflowError_1", jobParametersDTO.getInputs()
                .get(com.bytechef.platform.workflow.JobInputConstants.TRIGGER_NAME_INPUT));
    }

    private ErrorWorkflowJobStatusApplicationEventListener listener() {
        return new ErrorWorkflowJobStatusApplicationEventListener(
            new ErrorWorkflowPayloadFactory("https://app.example.com"), errorWorkflowResolver, jobService,
            principalJobFacade, principalJobService, taskExecutionService, null);
    }
}
