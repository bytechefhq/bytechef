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
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Marks the {@link ApprovalTask} keyed on a resumed job's identifier as {@link ApprovalTask.Status#COMPLETED}. Runs
 * after the resume transaction commits so the job state transition is durable before the task status flips. A missing
 * approval task is a normal outcome — not every resumed job has a backing approval-task row — and is silently ignored.
 *
 * @author Ivica Cardic
 */
@Component
public class ApprovalTaskCompletionListener {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskCompletionListener.class);

    private final ApprovalTaskService approvalTaskService;
    private final ApprovalTokens approvalTokens;

    @SuppressFBWarnings("EI")
    public ApprovalTaskCompletionListener(ApprovalTaskService approvalTaskService, ApprovalTokens approvalTokens) {
        this.approvalTaskService = approvalTaskService;
        this.approvalTokens = approvalTokens;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobResumed(JobResumedEvent event) {
        Optional<ApprovalTask> approvalTaskOptional = approvalTaskService
            .fetchApprovalTaskByJobResumeId(event.jobResumeId())
            .or(() -> fetchUnresolvedApprovalTaskWithTheSameJobResumeId(event.jobResumeId()));

        if (approvalTaskOptional.isEmpty()) {
            return;
        }

        ApprovalTask approvalTask = approvalTaskOptional.get();

        if (approvalTask.isResolved()) {
            return;
        }

        approvalTask.complete();

        approvalTaskService.resolve(approvalTask);

        if (log.isDebugEnabled()) {
            log.debug("Marked approval task id={} as COMPLETED after job resume", approvalTask.getId());
        }
    }

    private Optional<ApprovalTask> fetchUnresolvedApprovalTaskWithTheSameJobResumeId(String token) {
        Optional<JobResumeId> jobResumeIdOptional = approvalTokens.resolveJobResumeId(token);

        if (jobResumeIdOptional.isEmpty()) {
            return Optional.empty();
        }

        JobResumeId jobResumeId = jobResumeIdOptional.get();

        return approvalTaskService.getUnresolvedApprovalTasks()
            .stream()
            .filter(approvalTask -> approvalTask.getJobResumeId() != null)
            .filter(approvalTask -> approvalTokens.resolveJobResumeId(approvalTask.getJobResumeId())
                .filter(jobResumeId::equals)
                .isPresent())
            .findFirst();
    }
}
