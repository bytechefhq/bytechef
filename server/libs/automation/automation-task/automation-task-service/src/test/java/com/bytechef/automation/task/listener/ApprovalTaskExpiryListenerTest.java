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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskExpiryListenerTest {

    private static final String SIGNING_SECRET = EncodingUtils.base64EncodeToString("0123456789abcdef0123456789abcdef");

    private final ApprovalTaskService approvalTaskService = mock(ApprovalTaskService.class);
    private final ApprovalTokens approvalTokens = new ApprovalTokensImpl(
        Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);

    private final ApprovalTaskExpiryListener approvalTaskExpiryListener = new ApprovalTaskExpiryListener(
        approvalTaskService, approvalTokens);

    @Test
    void testOnJobApprovalClosedExpiresOnlyTheTasksOfThatJob() {
        ApprovalTask closedJobApprovalTask = approvalTask(42L);
        ApprovalTask otherJobApprovalTask = approvalTask(43L);

        when(approvalTaskService.getUnresolvedApprovalTasks())
            .thenReturn(List.of(closedJobApprovalTask, otherJobApprovalTask));

        approvalTaskExpiryListener.onJobApprovalClosed(new JobApprovalClosedEvent(42L));

        assertThat(closedJobApprovalTask.getStatus()).isEqualTo(ApprovalTask.Status.EXPIRED);
        assertThat(otherJobApprovalTask.getStatus()).isEqualTo(ApprovalTask.Status.OPEN);

        verify(approvalTaskService).resolve(closedJobApprovalTask);
        verify(approvalTaskService, never()).resolve(otherJobApprovalTask);
    }

    @Test
    void testOnJobApprovalClosedSkipsATaskWithAnUnreadableResumeToken() {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId("v1.1.garbage.signature");

        when(approvalTaskService.getUnresolvedApprovalTasks()).thenReturn(List.of(approvalTask));

        approvalTaskExpiryListener.onJobApprovalClosed(new JobApprovalClosedEvent(42L));

        verify(approvalTaskService, never()).resolve(any());
    }

    private ApprovalTask approvalTask(long jobId) {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setId(jobId);

        JobResumeId jobResumeId = JobResumeId.of("public", jobId);

        approvalTask.setJobResumeId(approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1)));

        return approvalTask;
    }
}
