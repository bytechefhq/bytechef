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

package com.bytechef.automation.task.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.SuspendUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.JobSuspends;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApprovalTaskFacadeImpl implements ApprovalTaskFacade {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskFacadeImpl.class);

    private final ApprovalTaskService approvalTaskService;
    private final ApprovalTokens approvalTokens;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final @Nullable String publicUrl;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskFacadeImpl(
        ApprovalTaskService approvalTaskService, ApprovalTokens approvalTokens, JobService jobService,
        PrincipalJobService principalJobService, ProjectDeploymentService projectDeploymentService,
        @Value("${bytechef.public-url:#{null}}") @Nullable String publicUrl,
        TaskExecutionService taskExecutionService, WorkflowService workflowService) {

        this.approvalTaskService = approvalTaskService;
        this.approvalTokens = approvalTokens;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
        this.publicUrl = publicUrl;
        this.taskExecutionService = taskExecutionService;
        this.workflowService = workflowService;
    }

    @Override
    public ApprovalTask createApprovalTask(ApprovalTask approvalTask) {
        String jobResumeIdString = approvalTask.getJobResumeId();

        Assert.notNull(jobResumeIdString, "'jobResumeId' must not be null");

        JobResumeId jobResumeId = approvalTokens.resolveJobResumeId(jobResumeIdString)
            .orElse(null);

        if (jobResumeId == null) {
            log.warn("Rejected approval task '{}': its resume token is invalid or expired", approvalTask.getName());

            throw new IllegalArgumentException("Invalid or expired approval token");
        }

        approvalTask.setEnvironment(getEnvironment(jobResumeId.getJobId()));

        return approvalTaskService.create(approvalTask);
    }

    @Override
    public List<PendingApproval> getPendingApprovals(@Nullable Integer environmentId) {
        List<PendingApproval> pendingApprovals = new ArrayList<>();

        for (Job job : jobService.getStaleJobs(Job.Status.STOPPED, Instant.now())) {
            Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);
            Long jobId = job.getId();

            if (jobResumeId == null || jobId == null || !JobSuspends.isAwaitingApproval(job)) {
                continue;
            }

            if (environmentId != null) {
                Environment environment;

                try {
                    environment = getEnvironment(jobId);
                } catch (RuntimeException runtimeException) {
                    log.warn("Could not resolve the environment of suspended job {}", jobId, runtimeException);

                    continue;
                }

                if (environment.ordinal() != environmentId) {
                    continue;
                }
            }

            Instant expiresAt = resolveExpiresAt(job);

            String formUrl = ApprovalFormUrls.buildFormUrl(
                publicUrl, jobResumeId.toString(), approvalTokens, expiresAt)
                .orElse(null);

            pendingApprovals.add(
                new PendingApproval(jobId, resolveWorkflowLabel(job), formUrl, job.getCreatedDate(), expiresAt));
        }

        pendingApprovals.sort(
            Comparator.comparing(
                PendingApproval::createdDate, Comparator.nullsLast(Comparator.reverseOrder())));

        return pendingApprovals;
    }

    private String resolveWorkflowLabel(Job job) {
        try {
            Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

            String label = workflow.getLabel();

            return label == null || label.isBlank() ? job.getWorkflowId() : label;
        } catch (Exception exception) {
            return job.getWorkflowId();
        }
    }

    private @Nullable Instant resolveExpiresAt(Job job) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return null;
        }

        try {
            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionResumeId);

            return SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata());
        } catch (Exception exception) {
            return null;
        }
    }

    private Environment getEnvironment(long jobId) {
        Optional<Long> projectDeploymentIdOptional = principalJobService.fetchJobPrincipalId(
            jobId, PlatformType.AUTOMATION);

        if (projectDeploymentIdOptional.isEmpty()) {
            return Environment.DEVELOPMENT;
        }

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            projectDeploymentIdOptional.get());

        return projectDeployment.getEnvironment();
    }
}
