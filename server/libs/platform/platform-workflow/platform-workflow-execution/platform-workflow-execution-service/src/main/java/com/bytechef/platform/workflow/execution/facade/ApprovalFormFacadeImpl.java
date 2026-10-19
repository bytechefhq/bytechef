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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Service
public class ApprovalFormFacadeImpl implements ApprovalFormFacade {

    private static final String ENVIRONMENT_ID_METADATA_KEY = "environmentId";

    private final ObjectProvider<ApprovalTokens> approvalTokensProvider;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskStateService taskStateService;
    private final TransactionOperations transactionOperations;

    @Autowired
    @SuppressFBWarnings("EI")
    public ApprovalFormFacadeImpl(
        ObjectProvider<ApprovalTokens> approvalTokensProvider, JobService jobService,
        TaskExecutionService taskExecutionService, TaskStateService taskStateService,
        PlatformTransactionManager platformTransactionManager) {

        this(
            approvalTokensProvider, jobService, taskExecutionService, taskStateService,
            new TransactionTemplate(platformTransactionManager));
    }

    @SuppressFBWarnings("EI")
    public ApprovalFormFacadeImpl(
        ObjectProvider<ApprovalTokens> approvalTokensProvider, JobService jobService,
        TaskExecutionService taskExecutionService, TaskStateService taskStateService,
        TransactionOperations transactionOperations) {

        this.approvalTokensProvider = approvalTokensProvider;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskStateService = taskStateService;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public Optional<Map<String, ?>> fetchApprovalForm(String id) {
        String innerToken = resolveInnerToken(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid approval form id"));

        JobResumeId jobResumeId = JobResumeId.parse(innerToken);

        return TenantContext.callWithTenantId(
            jobResumeId.getTenantId(),
            () -> Objects.requireNonNull(
                transactionOperations.execute(transactionStatus -> fetchApprovalForm(jobResumeId))));
    }

    private Optional<Map<String, ?>> fetchApprovalForm(JobResumeId jobResumeId) {
        Optional<Job> jobOptional = jobService.fetchJob(jobResumeId.getJobId());

        if (jobOptional.isEmpty()) {
            return Optional.empty();
        }

        Job job = jobOptional.get();

        Map<String, ?> jobMetadata = job.getMetadata();

        if (job.getStatus() != Job.Status.STOPPED ||
            !jobResumeId.matches((String) jobMetadata.get(MetadataConstants.JOB_RESUME_ID))) {

            return Optional.empty();
        }

        TaskExecution taskExecution = taskExecutionService.getTaskExecution(
            MapUtils.getLong(jobMetadata, MetadataConstants.TASK_EXECUTION_RESUME_ID));

        Map<String, Object> approvalForm = new HashMap<>(
            ApprovalFormParameters.get(taskStateService, jobResumeId, taskExecution));

        Map<String, ?> taskExecutionMetadata = taskExecution.getMetadata();

        Object environmentId = taskExecutionMetadata.get(ENVIRONMENT_ID_METADATA_KEY);

        if (environmentId != null) {
            approvalForm.put(ENVIRONMENT_ID_METADATA_KEY, environmentId);
        }

        return Optional.of(approvalForm);
    }

    private Optional<String> resolveInnerToken(String id) {
        ApprovalTokens approvalTokens = approvalTokensProvider.getIfAvailable();

        if (approvalTokens == null) {
            return Optional.of(id);
        }

        return approvalTokens.resolveInnerToken(id);
    }
}
