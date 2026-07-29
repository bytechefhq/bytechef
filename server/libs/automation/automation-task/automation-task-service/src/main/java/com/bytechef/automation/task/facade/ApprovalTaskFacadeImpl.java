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
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        String innerToken = resolveInnerToken(approvalTask.getJobResumeId());

        approvalTask.setJobResumeId(innerToken);

        approvalTask.setEnvironment(getEnvironment(innerToken));

        return approvalTaskService.create(approvalTask);
    }

    @Override
    public List<PendingApproval> getPendingApprovals(@Nullable Integer environmentId) {
        List<PendingApproval> pendingApprovals = new ArrayList<>();

        for (Job job : jobService.getStaleJobs(Job.Status.STOPPED, Instant.now())) {
            Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

            if (jobResumeId == null || job.getId() == null) {
                continue;
            }

            // Scope to the requested environment so the banner matches the environment-scoped approval-task inbox
            // shown beside it. The environment is resolved from the run's project deployment, defaulting to
            // DEVELOPMENT for runs with no principal-job row.
            if (environmentId != null) {
                Environment environment = getEnvironment(jobResumeId.toString());

                if (environment.ordinal() != environmentId) {
                    continue;
                }
            }

            String formUrl = ApprovalFormUrls.buildFormUrl(publicUrl, jobResumeId.toString(), approvalTokens)
                .orElse(null);

            pendingApprovals.add(
                new PendingApproval(
                    job.getId(), resolveWorkflowLabel(job), formUrl, job.getCreatedDate(), resolveExpiresAt(job)));
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

    private String resolveInnerToken(String jobResumeIdString) {
        Assert.notNull(jobResumeIdString, "'jobResumeId' must not be null");

        return approvalTokens.resolveInnerToken(jobResumeIdString)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired approval token"));
    }

    private Environment getEnvironment(String jobResumeIdString) {
        try {
            JobResumeId jobResumeId = JobResumeId.parse(jobResumeIdString);

            long projectDeploymentId = principalJobService.getJobPrincipalId(
                jobResumeId.getJobId(), PlatformType.AUTOMATION);

            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

            return projectDeployment.getEnvironment();
        } catch (Exception exception) {
            // A run that is not backed by an automation project deployment (no principal-job row) has no environment
            // to derive — fall back to DEVELOPMENT per the facade contract instead of failing the approval-task
            // delivery for that run.
            if (log.isDebugEnabled()) {
                log.debug(
                    "Could not resolve the environment for approval task {}; defaulting to DEVELOPMENT: {}",
                    jobResumeIdString, exception.getMessage());
            }

            return Environment.DEVELOPMENT;
        }
    }
}
