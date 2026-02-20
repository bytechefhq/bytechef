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

package com.bytechef.task.dispatcher.suspend.completion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class SuspendTaskCompletionHandlerTest {

    private final ContextService contextService = mock(ContextService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final Base64FileStorageService base64FileStorageService = new Base64FileStorageService();
    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(base64FileStorageService);
    private final TaskStateService taskStateService = mock(TaskStateService.class);

    private final SuspendTaskCompletionHandler suspendTaskCompletionHandler = new SuspendTaskCompletionHandler(
        contextService, eventPublisher, jobService, taskExecutionService, taskFileStorage, taskStateService);

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);
    }

    @Test
    void testCanHandleReturnsTrueWhenNoParentAndHasResumeId() {
        String jobResumeIdString = createJobResumeIdString(100L, true);
        Suspend suspend = new Suspend(Map.of(), null);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, jobResumeIdString);
        taskExecution.putMetadata(MetadataConstants.SUSPEND, suspend);

        assertTrue(suspendTaskCompletionHandler.canHandle(taskExecution));
    }

    @Test
    void testCanHandleReturnsFalseWhenHasParentId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setParentId(1L);
        taskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, "someId");

        assertFalse(suspendTaskCompletionHandler.canHandle(taskExecution));
    }

    @Test
    void testCanHandleReturnsFalseWhenNoResumeId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        assertFalse(suspendTaskCompletionHandler.canHandle(taskExecution));
    }

    @Test
    void testHandleSavesStateAndStopsJob() {
        String jobResumeIdString = createJobResumeIdString(100L, true);
        Suspend suspend = new Suspend(Map.of("key", "value"), null);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "testTask", WorkflowConstants.TYPE, "suspend/v1")))
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(100L);
        taskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, jobResumeIdString);
        taskExecution.putMetadata(MetadataConstants.SUSPEND, suspend);

        when(taskExecutionService.update(any(TaskExecution.class)))
            .thenReturn(taskExecution);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of());

        when(jobService.getTaskExecutionJob(1L))
            .thenReturn(job);

        FileEntry contextFileEntry = taskFileStorage.storeContextValue(
            100L, Context.Classname.JOB, Map.of());

        when(contextService.peek(eq(100L), eq(Context.Classname.JOB)))
            .thenReturn(contextFileEntry);

        Job stoppedJob = new Job();

        stoppedJob.setId(100L);
        stoppedJob.setStatus(Job.Status.STOPPED);

        when(jobService.update(any(Job.class)))
            .thenReturn(job);
        when(jobService.setStatusToStopped(100L))
            .thenReturn(stoppedJob);

        suspendTaskCompletionHandler.handle(taskExecution);

        assertTrue(taskExecution.isHandled());

        verify(taskStateService).save(any(JobResumeId.class), any(Suspend.class));
        verify(jobService).update(any(Job.class));
        verify(jobService).setStatusToStopped(100L);

        ArgumentCaptor<JobStatusApplicationEvent> eventCaptor = ArgumentCaptor.forClass(
            JobStatusApplicationEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
    }

    @Test
    void testHandleThrowsWhenJobNotFound() {
        String jobResumeIdString = createJobResumeIdString(100L, true);
        Suspend suspend = new Suspend(Map.of(), null);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "testTask", WorkflowConstants.TYPE, "suspend/v1")))
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(100L);
        taskExecution.putMetadata(MetadataConstants.JOB_RESUME_ID, jobResumeIdString);
        taskExecution.putMetadata(MetadataConstants.SUSPEND, suspend);

        when(taskExecutionService.update(any(TaskExecution.class)))
            .thenReturn(taskExecution);
        when(jobService.getTaskExecutionJob(1L))
            .thenReturn(null);

        assertThrows(IllegalStateException.class, () -> suspendTaskCompletionHandler.handle(taskExecution));
    }

    private static String createJobResumeIdString(long jobId, boolean approved) {
        return EncodingUtils.base64EncodeToString(
            "public:" + jobId + ":" + UUID.randomUUID() + ":" + approved);
    }
}
