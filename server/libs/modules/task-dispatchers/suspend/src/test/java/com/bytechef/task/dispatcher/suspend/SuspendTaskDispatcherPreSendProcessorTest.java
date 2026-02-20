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

package com.bytechef.task.dispatcher.suspend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SuspendTaskDispatcherPreSendProcessorTest {

    private final JobService jobService = mock(JobService.class);
    private final TaskStateService taskStateService = mock(TaskStateService.class);

    private final SuspendTaskDispatcherPreSendProcessor preSendProcessor =
        new SuspendTaskDispatcherPreSendProcessor(jobService, taskStateService);

    @Test
    void testCanProcessReturnsTrueWhenJobHasResumeId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);

        String jobResumeIdString = createJobResumeIdString(100L, true);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeIdString));

        when(jobService.getJob(100L))
            .thenReturn(job);

        assertTrue(preSendProcessor.canProcess(taskExecution));
    }

    @Test
    void testCanProcessReturnsFalseWhenJobHasNoResumeId() {
        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of());

        when(jobService.getJob(100L))
            .thenReturn(job);

        assertFalse(preSendProcessor.canProcess(taskExecution));
    }

    @Test
    void testProcessRecoversSuspendStateAndCleansUp() {
        String jobResumeIdString = createJobResumeIdString(100L, true);
        Suspend suspend = new Suspend(Map.of("key", "value"), null);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeIdString));

        when(jobService.getJob(100L))
            .thenReturn(job);
        when(taskStateService.fetchValue(any(JobResumeId.class)))
            .thenReturn(Optional.of(suspend));

        TaskExecution result = preSendProcessor.process(taskExecution);

        assertNotNull(result);
        assertNotNull(result.getMetadata().get(MetadataConstants.SUSPEND));

        verify(jobService).update(any(Job.class));
        verify(taskStateService).delete(any(JobResumeId.class));
    }

    @Test
    void testProcessHandlesMissingSuspendState() {
        String jobResumeIdString = createJobResumeIdString(100L, true);

        TaskExecution taskExecution = TaskExecution.builder()
            .build();

        taskExecution.setJobId(100L);

        Job job = new Job();

        job.setId(100L);
        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeIdString));

        when(jobService.getJob(100L))
            .thenReturn(job);
        when(taskStateService.fetchValue(any(JobResumeId.class)))
            .thenReturn(Optional.empty());

        TaskExecution result = preSendProcessor.process(taskExecution);

        assertNotNull(result);
        assertFalse(result.getMetadata().containsKey(MetadataConstants.SUSPEND));

        verify(jobService).update(any(Job.class));
        verify(taskStateService).delete(any(JobResumeId.class));
    }

    private static String createJobResumeIdString(long jobId, boolean approved) {
        return EncodingUtils.base64EncodeToString(
            "public:" + jobId + ":" + UUID.randomUUID() + ":" + approved);
    }
}
