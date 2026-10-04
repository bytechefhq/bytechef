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
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApprovalFormFacadeTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 7L;
    private static final String SIGNING_SECRET = Base64.getEncoder()
        .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String TENANT_ID = "000001";

    private final ApprovalTokens approvalTokens = new ApprovalTokensImpl(
        Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);

    private final List<String> transactionTenantIds = new ArrayList<>();

    private final TransactionOperations transactionOperations = new TransactionOperations() {

        @Override
        public <T> T execute(TransactionCallback<T> transactionCallback) {
            transactionTenantIds.add(TenantContext.getCurrentTenantId());

            return transactionCallback.doInTransaction(new SimpleTransactionStatus());
        }
    };

    private final JobService jobService = mock(JobService.class);
    private final TaskExecution taskExecution = mock(TaskExecution.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskStateService taskStateService = mock(TaskStateService.class);

    private ApprovalFormFacade approvalFormFacade;

    @BeforeEach
    void setUp() {
        approvalFormFacade = new ApprovalFormFacadeImpl(
            approvalTokensProvider(), jobService, taskExecutionService, taskStateService, transactionOperations);
    }

    @Test
    void testGetApprovalFormOpensItsTransactionUnderTheTenantOfTheResumeId() {
        JobResumeId jobResumeId = TenantContext.callWithTenantId(TENANT_ID, () -> JobResumeId.of(JOB_ID));

        when(jobService.getJob(JOB_ID)).thenReturn(mock(Job.class));

        assertThatThrownBy(() -> approvalFormFacade.getApprovalForm(jobResumeId.toString()));

        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
    }

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

    @Test
    void testGetApprovalFormForSignedToken() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        stubStoppedJobWithTaskParameters(jobResumeId);

        String signedToken = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(signedToken);

        assertThat(approvalForm.get("formTitle")).isEqualTo("Approve order");

        verify(jobService).getJob(JOB_ID);
    }

    @Test
    void testGetApprovalFormRejectsTamperedSignedToken() {
        String signedToken = approvalTokens.toSignedToken(
            JobResumeId.of(JOB_ID)
                .toString(),
            Duration.ofHours(1));

        int signatureStart = signedToken.lastIndexOf('.') + 1;
        char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

        String tamperedToken =
            signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);

        assertThatThrownBy(() -> approvalFormFacade.getApprovalForm(tamperedToken))
            .isInstanceOf(IllegalArgumentException.class);

        verify(jobService, never()).getJob(anyLong());
    }

    @Test
    void testGetApprovalFormForLegacyUnsignedToken() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        stubStoppedJobWithTaskParameters(jobResumeId);

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(jobResumeId.toString());

        assertThat(approvalForm.get("formTitle")).isEqualTo("Approve order");
    }

    private ObjectProvider<ApprovalTokens> approvalTokensProvider() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("approvalTokens", approvalTokens);

        return beanFactory.getBeanProvider(ApprovalTokens.class);
    }

    private void stubStoppedJobWithTaskParameters(JobResumeId jobResumeId) {
        when(jobService.getJob(JOB_ID)).thenReturn(stoppedJob(jobResumeId));
        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);
        doReturn(Map.of("formTitle", "Approve order")).when(taskExecution)
            .getParameters();
        doReturn(Map.of()).when(taskExecution)
            .getMetadata();
        when(taskStateService.<Suspend>fetchValue(jobResumeId)).thenReturn(Optional.empty());
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
