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

import com.bytechef.automation.task.config.AutomationTaskIntTestConfiguration;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationTaskIntTestConfiguration.class)
class ApprovalTaskCompletionListenerIntTest {

    @Autowired
    private ApprovalTaskService approvalTaskService;

    @Autowired
    private ApprovalTokens approvalTokens;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ApprovalTaskCompletionListener approvalTaskCompletionListener;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM approval_task");

        approvalTaskCompletionListener = new ApprovalTaskCompletionListener(approvalTaskService, approvalTokens);
    }

    @Test
    void testCompletesTheTaskWhoseTokenTheResumeMatchesExactly() {
        String token = signedToken(7L);

        long approvalTaskId = createApprovalTask(token, Status.OPEN);

        approvalTaskCompletionListener.onJobResumed(new JobResumedEvent(token));

        assertThat(getStatus(approvalTaskId)).isEqualTo(Status.COMPLETED);
    }

    @Test
    void testCompletesTheTaskWhenTheResumeCarriesANewlySignedTokenForTheSameJobResumeId() {
        String jobResumeId = jobResumeId(7L);

        long approvalTaskId = createApprovalTask(
            approvalTokens.toSignedToken(jobResumeId, Duration.ofHours(1)), Status.IN_PROGRESS);

        approvalTaskCompletionListener.onJobResumed(
            new JobResumedEvent(approvalTokens.toSignedToken(jobResumeId, Duration.ofHours(2))));

        assertThat(getStatus(approvalTaskId)).isEqualTo(Status.COMPLETED);
    }

    @Test
    void testLeavesTasksOfOtherJobResumeIdsUnresolved() {
        long approvalTaskId = createApprovalTask(signedToken(8L), Status.OPEN);

        approvalTaskCompletionListener.onJobResumed(new JobResumedEvent(signedToken(7L)));

        assertThat(getStatus(approvalTaskId)).isEqualTo(Status.OPEN);
    }

    @Test
    void testLeavesAnExpiredTaskExpired() {
        String token = signedToken(7L);

        long approvalTaskId = createApprovalTask(token, Status.EXPIRED);

        approvalTaskCompletionListener.onJobResumed(new JobResumedEvent(token));

        assertThat(getStatus(approvalTaskId)).isEqualTo(Status.EXPIRED);
    }

    private long createApprovalTask(String jobResumeId, Status status) {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId(jobResumeId);
        approvalTask.setName("Approve the order");
        approvalTask.setStatus(status);

        approvalTask = approvalTaskService.create(approvalTask);

        return Objects.requireNonNull(approvalTask.getId());
    }

    private Status getStatus(long approvalTaskId) {
        ApprovalTask approvalTask = approvalTaskService.getApprovalTask(approvalTaskId);

        return approvalTask.getStatus();
    }

    private static String jobResumeId(long jobId) {
        JobResumeId jobResumeId = JobResumeId.of(TenantContext.DEFAULT_TENANT_ID, jobId);

        return jobResumeId.toString();
    }

    private String signedToken(long jobId) {
        return approvalTokens.toSignedToken(jobResumeId(jobId), Duration.ofHours(1));
    }
}
