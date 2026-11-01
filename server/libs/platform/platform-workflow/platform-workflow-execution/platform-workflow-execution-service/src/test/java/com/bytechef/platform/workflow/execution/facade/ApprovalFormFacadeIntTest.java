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

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Import({
    ApprovalFormFacadeImpl.class, JobServiceImpl.class, TaskExecutionServiceImpl.class
})
@SpringBootTest(
    classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class ApprovalFormFacadeIntTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static final String SIGNING_SECRET = createSigningSecret();

    private static final long MISSING_TASK_EXECUTION_ID = 999_999L;
    private static final String TENANT_ID = "000001";

    @DynamicPropertySource
    static void signingSecretProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("bytechef.approval.signed-token.secret", () -> SIGNING_SECRET);
    }

    @Autowired
    private ApprovalFormFacade approvalFormFacade;

    @Autowired
    private ApprovalTokens approvalTokens;

    @Autowired
    private ObjectProvider<ApprovalTokens> approvalTokensProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskStateService taskStateService;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM task_state");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");
    }

    @Test
    void testGetApprovalFormOpensItsTransactionUnderTheTenantOfTheResumeId() {
        Job job = saveJob(Job.Status.STARTED);

        long jobId = Validate.notNull(job.getId(), "id");

        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(jobId));

        List<String> transactionTenantIds = new ArrayList<>();
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        TransactionOperations recordingTransactionOperations = new TransactionOperations() {

            @Override
            public <T> T execute(TransactionCallback<T> transactionCallback) {
                transactionTenantIds.add(TenantContext.getCurrentTenantId());

                return transactionTemplate.execute(transactionCallback);
            }
        };

        ApprovalFormFacade tenantRecordingApprovalFormFacade = new ApprovalFormFacadeImpl(
            approvalTokensProvider, jobService, taskExecutionService, taskStateService,
            recordingTransactionOperations);

        String id = jobResumeId.toString();

        assertThatThrownBy(() -> tenantRecordingApprovalFormFacade.getApprovalForm(id))
            .isInstanceOf(RuntimeException.class)
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage("Approval form is no longer available; job " + jobId + " is STARTED");

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
    }

    @Test
    void testGetApprovalFormRejectsAResumeIdThatDoesNotMatch() {
        Job job = saveJob(Job.Status.STOPPED);

        long jobId = Validate.notNull(job.getId(), "id");

        stopJob(job, JobResumeId.of(jobId), MISSING_TASK_EXECUTION_ID);

        JobResumeId otherJobResumeId = JobResumeId.of(jobId);

        assertThatThrownBy(() -> approvalFormFacade.getApprovalForm(otherJobResumeId.toString()))
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage(
                "Approval form is no longer available; the resume id of job " + jobId + " does not match");
    }

    @Test
    void testGetApprovalFormReturnsTheParametersStoredWithTheSuspend() {
        Job job = saveJob(Job.Status.STOPPED);

        long jobId = Validate.notNull(job.getId(), "id");

        JobResumeId jobResumeId = JobResumeId.of(jobId);

        long taskExecutionId = saveTaskExecution(jobId, Map.of("model", "agent model"), Map.of("environmentId", 2));

        stopJob(job, jobResumeId, taskExecutionId);

        taskStateService.save(
            jobResumeId,
            new Suspend(
                Map.of(MetadataConstants.APPROVAL_FORM_PARAMETERS, Map.of("formTitle", "Refund order 42")), null));

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(jobResumeId.toString());

        assertThat(approvalForm).isEqualTo(Map.of("formTitle", "Refund order 42", "environmentId", 2));
    }

    @Test
    void testGetApprovalFormFallsBackToTheTaskParameters() {
        Job job = saveJob(Job.Status.STOPPED);

        long jobId = Validate.notNull(job.getId(), "id");

        JobResumeId jobResumeId = JobResumeId.of(jobId);

        long taskExecutionId = saveTaskExecution(jobId, Map.of("formTitle", "Approve the invoice"), Map.of());

        stopJob(job, jobResumeId, taskExecutionId);

        taskStateService.save(jobResumeId, new Suspend(Map.of("formUrl", "https://example.com/resume/abc"), null));

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(jobResumeId.toString());

        assertThat(approvalForm).isEqualTo(Map.of("formTitle", "Approve the invoice"));
    }

    @Test
    void testGetApprovalFormForSignedToken() {
        JobResumeId jobResumeId = saveStoppedJobWithTaskParameters();

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(signedToken);

        assertThat(approvalForm)
            .extractingByKey("formTitle")
            .isEqualTo("Approve order");
    }

    @Test
    void testGetApprovalFormRejectsTamperedSignedToken() {
        JobResumeId jobResumeId = saveStoppedJobWithTaskParameters();

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        int signatureStart = signedToken.lastIndexOf('.') + 1;
        char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

        String tamperedToken =
            signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);

        assertThatThrownBy(() -> approvalFormFacade.getApprovalForm(tamperedToken))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid approval form id");
    }

    @Test
    void testGetApprovalFormForLegacyUnsignedToken() {
        JobResumeId jobResumeId = saveStoppedJobWithTaskParameters();

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(jobResumeId.toString());

        assertThat(approvalForm)
            .extractingByKey("formTitle")
            .isEqualTo("Approve order");
    }

    private JobResumeId saveStoppedJobWithTaskParameters() {
        Job job = saveJob(Job.Status.STOPPED);

        long jobId = Validate.notNull(job.getId(), "id");

        JobResumeId jobResumeId = JobResumeId.of(jobId);

        long taskExecutionId = saveTaskExecution(jobId, Map.of("formTitle", "Approve order"), Map.of());

        stopJob(job, jobResumeId, taskExecutionId);

        return jobResumeId;
    }

    private Job saveJob(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("workflow-1");

        return jobRepository.save(job);
    }

    private long saveTaskExecution(long jobId, Map<String, ?> parameters, Map<String, ?> metadata) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .metadata(metadata)
            .status(TaskExecution.Status.STARTED)
            .taskNumber(0)
            .workflowTask(
                new WorkflowTask(Map.of("name", "approval_1", "type", "approval/v1/request", "parameters", parameters)))
            .build();

        taskExecution = taskExecutionRepository.save(taskExecution);

        return Validate.notNull(taskExecution.getId(), "id");
    }

    private void stopJob(Job job, JobResumeId jobResumeId, long taskExecutionId) {
        Job currentJob = jobService.getJob(Validate.notNull(job.getId(), "id"));

        currentJob.setMetadata(
            Map.of(
                MetadataConstants.JOB_RESUME_ID, jobResumeId.toString(),
                MetadataConstants.TASK_EXECUTION_RESUME_ID, taskExecutionId));

        jobService.update(currentJob);
    }

    private static String createSigningSecret() {
        byte[] secretBytes = new byte[32];

        SECURE_RANDOM.nextBytes(secretBytes);

        return EncodingUtils.base64EncodeToString(secretBytes);
    }
}
