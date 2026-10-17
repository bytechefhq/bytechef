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

package com.bytechef.automation.task.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskCompletionListenerTest {

    private final ApprovalTaskService approvalTaskService = mock(ApprovalTaskService.class);
    private final ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

    private final ApprovalTaskCompletionListener approvalTaskCompletionListener =
        new ApprovalTaskCompletionListener(approvalTaskService, approvalTokens);

    @Test
    void testCompletesTheTaskWhoseTokenTheResumeMatchesExactly() {
        ApprovalTask approvalTask = approvalTask("original-token");

        when(approvalTaskService.fetchApprovalTaskByJobResumeId("original-token"))
            .thenReturn(Optional.of(approvalTask));

        approvalTaskCompletionListener.onJobResumed(new JobResumedEvent("original-token"));

        assertThat(approvalTask.getStatus()).isEqualTo(ApprovalTask.Status.COMPLETED);

        verify(approvalTaskService).resolve(approvalTask);
    }

    @Test
    void testCompletesTheTaskWhenTheResumeCarriesANewlySignedTokenForTheSameJobResumeId() {
        JobResumeId jobResumeId = JobResumeId.of("tenant", 7L);
        ApprovalTask approvalTask = approvalTask("original-token");

        when(approvalTaskService.fetchApprovalTaskByJobResumeId("resigned-token")).thenReturn(Optional.empty());
        when(approvalTokens.resolveJobResumeId("resigned-token")).thenReturn(Optional.of(jobResumeId));
        when(approvalTokens.resolveJobResumeId("original-token")).thenReturn(Optional.of(jobResumeId));
        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));

        approvalTaskCompletionListener.onJobResumed(new JobResumedEvent("resigned-token"));

        assertThat(approvalTask.getStatus()).isEqualTo(ApprovalTask.Status.COMPLETED);

        verify(approvalTaskService).resolve(approvalTask);
    }

    @Test
    void testLeavesTasksOfOtherJobResumeIdsUnresolved() {
        ApprovalTask approvalTask = approvalTask("other-token");

        when(approvalTaskService.fetchApprovalTaskByJobResumeId("resigned-token")).thenReturn(Optional.empty());
        when(approvalTokens.resolveJobResumeId("resigned-token"))
            .thenReturn(Optional.of(JobResumeId.of("tenant", 7L)));
        when(approvalTokens.resolveJobResumeId("other-token")).thenReturn(Optional.of(JobResumeId.of("tenant", 8L)));
        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));

        approvalTaskCompletionListener.onJobResumed(new JobResumedEvent("resigned-token"));

        assertThat(approvalTask.isResolved()).isFalse();

        verify(approvalTaskService, never()).resolve(approvalTask);
    }

    private static ApprovalTask approvalTask(String jobResumeId) {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId(jobResumeId);

        return approvalTask;
    }
}
