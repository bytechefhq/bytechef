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

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Marks the unresolved {@link ApprovalTask}s of a run that ended without a decision as
 * {@link ApprovalTask.Status#EXPIRED}.
 *
 * @author Ivica Cardic
 */
@Component
public class ApprovalTaskExpiryListener {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskExpiryListener.class);

    private final ApprovalTaskService approvalTaskService;
    private final ApprovalTokens approvalTokens;

    @SuppressFBWarnings("EI")
    public ApprovalTaskExpiryListener(ApprovalTaskService approvalTaskService, ApprovalTokens approvalTokens) {
        this.approvalTaskService = approvalTaskService;
        this.approvalTokens = approvalTokens;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onJobApprovalClosed(JobApprovalClosedEvent event) {
        for (ApprovalTask approvalTask : approvalTaskService.getUnresolvedApprovalTasks()) {
            if (!isTaskOfJob(approvalTask, event.jobId())) {
                continue;
            }

            approvalTask.expire();

            try {
                approvalTaskService.resolve(approvalTask);
            } catch (RuntimeException runtimeException) {
                log.warn(
                    "Could not expire approval task {} of job {}; the reconciliation sweep retries it",
                    approvalTask.getId(), event.jobId(), runtimeException);
            }
        }
    }

    private boolean isTaskOfJob(ApprovalTask approvalTask, long jobId) {
        String jobResumeIdString = approvalTask.getJobResumeId();

        if (jobResumeIdString == null) {
            return false;
        }

        Optional<JobResumeId> jobResumeIdOptional = approvalTokens.resolveJobResumeId(jobResumeIdString);

        return jobResumeIdOptional.isPresent() && jobResumeIdOptional.get()
            .getJobId() == jobId;
    }
}
