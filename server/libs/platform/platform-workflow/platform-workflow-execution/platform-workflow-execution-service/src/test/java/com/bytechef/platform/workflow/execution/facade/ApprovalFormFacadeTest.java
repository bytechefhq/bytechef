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

package com.bytechef.platform.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApprovalFormFacadeTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 7L;

    private final JobService jobService = mock(JobService.class);
    private final TaskExecution taskExecution = mock(TaskExecution.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskStateService taskStateService = mock(TaskStateService.class);
    private final ApprovalFormFacade approvalFormFacade = new ApprovalFormFacadeImpl(
        jobService, taskExecutionService, taskStateService);

    @Test
    void testGetApprovalFormRejectsAResumeIdThatDoesNotMatch() {
        JobResumeId storedJobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(stoppedJob(storedJobResumeId));

        JobResumeId otherJobResumeId = JobResumeId.of(JOB_ID);

        assertThatThrownBy(() -> approvalFormFacade.getApprovalForm(otherJobResumeId.toString()))
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage(
                "Approval form is no longer available; the resume id of job " + JOB_ID + " does not match");

        verify(taskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    void testGetApprovalFormReturnsTheParametersStoredWithTheSuspend() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(stoppedJob(jobResumeId));
        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);
        doReturn(Map.of("model", "agent model")).when(taskExecution)
            .getParameters();
        doReturn(Map.of()).when(taskExecution)
            .getMetadata();
        when(taskStateService.<Suspend>fetchValue(jobResumeId)).thenReturn(
            Optional.of(
                new Suspend(
                    Map.of(MetadataConstants.APPROVAL_FORM_PARAMETERS, Map.of("formTitle", "Refund order 42")),
                    null)));

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(jobResumeId.toString());

        assertThat(approvalForm).isEqualTo(Map.of("formTitle", "Refund order 42"));
    }

    @Test
    void testGetApprovalFormFallsBackToTheTaskParameters() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenReturn(stoppedJob(jobResumeId));
        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);
        doReturn(Map.of("formTitle", "Approve the invoice")).when(taskExecution)
            .getParameters();
        doReturn(Map.of()).when(taskExecution)
            .getMetadata();
        when(taskStateService.<Suspend>fetchValue(jobResumeId)).thenReturn(
            Optional.of(new Suspend(Map.of("formUrl", "https://example.com/resume/abc"), null)));

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(jobResumeId.toString());

        assertThat(approvalForm).isEqualTo(Map.of("formTitle", "Approve the invoice"));
    }

    private static Job stoppedJob(JobResumeId storedJobResumeId) {
        Job job = new Job(JOB_ID);

        job.setStatus(Job.Status.STOPPED);
        job.setMetadata(
            Map.of(
                MetadataConstants.JOB_RESUME_ID, storedJobResumeId.toString(),
                MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_ID));

        return job;
    }
}
