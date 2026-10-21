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

package com.bytechef.platform.workflow.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowCallResultsTest {

    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);

    @Test
    void testReadResultPrefersTheCallableResponse() {
        Job job = job(7L, Job.Status.COMPLETED);
        FileEntry outputEntry = mock(FileEntry.class);
        TaskExecution lastTaskExecution = mock(TaskExecution.class);

        doReturn(Map.of(MetadataConstants.CALLABLE_RESPONSE, true)).when(lastTaskExecution)
            .getMetadata();
        when(lastTaskExecution.getOutput()).thenReturn(outputEntry);
        when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.of(lastTaskExecution));
        when(taskFileStorage.readTaskExecutionOutput(outputEntry))
            .thenReturn(new ActionDefinition.CallableResponse(Map.of("approved", true)));

        assertThat(WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage))
            .isEqualTo(Map.of("approved", true));
    }

    @Test
    void testReadResultFallsBackToTheJobOutputs() {
        Job job = job(7L, Job.Status.COMPLETED);
        FileEntry jobOutputs = mock(FileEntry.class);

        job.setOutputs(jobOutputs);

        when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.empty());
        doReturn(Map.of("total", 3)).when(taskFileStorage)
            .readJobOutputs(jobOutputs);

        assertThat(WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage))
            .isEqualTo(Map.of("total", 3));
    }

    @Test
    void testReadResultWithoutOutputsIsNull() {
        Job job = job(7L, Job.Status.COMPLETED);

        when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.empty());

        assertThat(WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage)).isNull();
    }

    @Test
    void testStoppedJobWithAResumeIdIsPendingApproval() {
        Job job = job(7L, Job.Status.STOPPED);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

        assertThat(WorkflowCallResults.isPendingApproval(job)).isTrue();
        assertThat(WorkflowCallResults.fetchApprovalFormUrl(job, "https://bc.example", null))
            .contains("https://bc.example/resume/resume-1");
        assertThat(WorkflowCallResults.fetchResumeToken(job, null)).contains("resume-1");
    }

    @Test
    void testStoppedJobWithoutAResumeIdIsNotPendingApproval() {
        Job job = job(7L, Job.Status.STOPPED);

        assertThat(WorkflowCallResults.isPendingApproval(job)).isFalse();
        assertThat(WorkflowCallResults.fetchApprovalFormUrl(job, "https://bc.example", null)).isEmpty();
    }

    @Test
    void testCompletedJobIsNotPendingApprovalEvenWithAResumeId() {
        Job job = job(7L, Job.Status.COMPLETED);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

        assertThat(WorkflowCallResults.isPendingApproval(job)).isFalse();
    }

    private static Job job(long id, Job.Status status) {
        Job job = new Job();

        job.setId(id);
        job.setStatus(status);

        return job;
    }
}
