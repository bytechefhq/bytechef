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

package com.bytechef.platform.webhook.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacadeImpl;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionOperations;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(ObjectMapperSetupExtension.class)
@ContextConfiguration(classes = {
    ApprovalController.class, ApprovalControllerIntTest.ApprovalControllerIntTestConfiguration.class
})
@WebMvcTest(controllers = ApprovalController.class, properties = "bytechef.coordinator.enabled=true")
class ApprovalControllerIntTest {

    private static final long JOB_ID = 42L;
    private static final String SIGNING_SECRET = EncodingUtils.base64EncodeToString("0123456789abcdef0123456789abcdef");
    private static final long TASK_EXECUTION_ID = 7L;
    private static final String TENANT_ID = "public";

    @Autowired
    private ApprovalTokens approvalTokens;

    @MockitoBean
    private JobFacade jobFacade;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void setUp() {
        when(jobService.tryClaimResume(any())).thenReturn(true);

        stubTaskExecution(null);
    }

    @Nested
    class LegacyStoppedJob {

        @BeforeEach
        void setUp() {
            stubJob(Job.Status.STOPPED, Map.of());
        }

        @Test
        void testApproveResumesJobForSignedToken() throws Exception {
            ApprovalId approvalId = ApprovalId.of(TENANT_ID, JOB_ID, true);

            mockMvc.perform(post("/approvals/{id}", sign(approvalId)))
                .andExpect(status().isNoContent());

            verify(jobFacade).resumeApproval(JOB_ID, approvalId.getUuidAsString(), true);
        }

        @Test
        void testApproveRejectsTamperedSignedToken() throws Exception {
            String signedToken = sign(ApprovalId.of(TENANT_ID, JOB_ID, true));

            int signatureStart = signedToken.lastIndexOf('.') + 1;
            char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

            String tamperedToken =
                signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);

            mockMvc.perform(post("/approvals/{id}", tamperedToken))
                .andExpect(status().isBadRequest());

            verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
        }

        @Test
        void testApproveRejectsUnsignedToken() throws Exception {
            ApprovalId approvalId = ApprovalId.of(TENANT_ID, JOB_ID, false);

            mockMvc.perform(post("/approvals/{id}", approvalId.toString()))
                .andExpect(status().isBadRequest());

            verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
        }

        @Test
        void testApproveRejectsMalformedToken() throws Exception {
            mockMvc.perform(post("/approvals/{id}", "not-a-token"))
                .andExpect(status().isBadRequest());

            verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
        }

        @Test
        void testApproveReturnsGoneForAnApprovalAlreadyProcessed() throws Exception {
            ApprovalId approvalId = ApprovalId.of(TENANT_ID, JOB_ID, true);

            doThrow(new IllegalArgumentException("Approval already processed"))
                .when(jobFacade)
                .resumeApproval(JOB_ID, approvalId.getUuidAsString(), true);

            mockMvc.perform(post("/approvals/{id}", sign(approvalId)))
                .andExpect(status().isGone());
        }

        @Test
        void testApproveDoesNotResumeAJobThatIsStillRunning() throws Exception {
            stubJob(Job.Status.STARTED, Map.of());

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isConflict());

            verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
        }
    }

    @Nested
    class SuspendedJob {

        private String storedJobResumeId;

        @BeforeEach
        void setUp() {
            storedJobResumeId = JobResumeId.of(TENANT_ID, JOB_ID)
                .toString();

            stubJob(Job.Status.STOPPED, suspendedMetadata(Map.of()));
        }

        @Test
        void testApproveResumesSuspendedJobThroughItsSuspendedTaskExecution() throws Exception {
            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isNoContent());

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", true));
            verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
        }

        @Test
        void testApproveRejectsAnUnsignedTokenForASuspendedApproval() throws Exception {
            stubJob(Job.Status.STOPPED, suspendedMetadata(Map.of(MetadataConstants.APPROVAL_RESUME, true)));

            String unsignedToken = ApprovalId.of(TENANT_ID, JOB_ID, true)
                .toString();

            mockMvc.perform(post("/approvals/{id}", unsignedToken))
                .andExpect(status().isBadRequest());

            mockMvc.perform(post("/approvals/{id}", unsignedToken).accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest());

            mockMvc.perform(get("/approvals/{id}", unsignedToken))
                .andExpect(status().isBadRequest());

            verifyNeverResumed();
        }

        @Test
        void testApproveRejectsATamperedTokenForASuspendedApproval() throws Exception {
            stubJob(Job.Status.STOPPED, suspendedMetadata(Map.of(MetadataConstants.APPROVAL_RESUME, true)));

            String signedToken = sign(ApprovalId.of(TENANT_ID, JOB_ID, true));

            int signatureStart = signedToken.lastIndexOf('.') + 1;
            char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

            String tamperedToken =
                signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);

            mockMvc.perform(post("/approvals/{id}", tamperedToken))
                .andExpect(status().isBadRequest());

            verifyNeverResumed();
        }

        @Test
        void testApproveResolvesASuspendedApprovalForASignedToken() throws Exception {
            stubJob(Job.Status.STOPPED, suspendedMetadata(Map.of(MetadataConstants.APPROVAL_RESUME, true)));

            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setWorkflowTask(
                new WorkflowTask(
                    Map.of("name", "approval_1", "type", "approval/v1/requestApproval", "parameters", Map.of())));

            when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isNoContent());

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", true));
        }

        @Test
        void testApproveRejectionReachesSuspendedTaskAsApprovedFalse() throws Exception {
            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, false))))
                .andExpect(status().isNoContent());

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", false));
        }

        @Test
        void testApproveConsumesTheSuspendAndTheApprovalPair() throws Exception {
            String pairUuid = UUID.randomUUID()
                .toString();

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, pairUuid, true, 3L))))
                .andExpect(status().isNoContent());

            ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);

            verify(jobService).update(jobArgumentCaptor.capture());

            Job updatedJob = jobArgumentCaptor.getValue();

            assertThat(updatedJob.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(storedJobResumeId);
            assertThat(updatedJob.getMetadata(MetadataConstants.CONSUMED_APPROVAL_IDS)).isEqualTo(List.of(pairUuid));
            assertThat(updatedJob.getMetadata(MetadataConstants.LAST_RESUMED_TASK_EXECUTION_ID))
                .isEqualTo(TASK_EXECUTION_ID);
        }

        @Test
        void testApproveRejectsAReplayedApprovalPair() throws Exception {
            String pairUuid = UUID.randomUUID()
                .toString();

            stubJob(
                Job.Status.STOPPED,
                suspendedMetadata(Map.of(MetadataConstants.CONSUMED_APPROVAL_IDS, List.of(pairUuid))));

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, pairUuid, false, 3L))))
                .andExpect(status().isGone());

            verifyNeverResumed();
        }

        @Test
        void testApproveRejectsAnAlreadyResolvedSuspend() throws Exception {
            stubJob(
                Job.Status.STOPPED,
                suspendedMetadata(Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, storedJobResumeId)));

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isGone());

            verifyNeverResumed();
        }

        @Test
        void testApproveRejectsAnExpiredSuspend() throws Exception {
            stubTaskExecution(
                Instant.now()
                    .minusSeconds(60));

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isGone());

            verifyNeverResumed();
        }

        @Test
        void testApproveRejectsALinkIssuedForAnEarlierSuspend() throws Exception {
            stubJob(
                Job.Status.STOPPED,
                suspendedMetadata(Map.of(MetadataConstants.LAST_RESUMED_TASK_EXECUTION_ID, 5L)));

            String pairUuid = UUID.randomUUID()
                .toString();

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, pairUuid, true, 3L))))
                .andExpect(status().isBadRequest());

            verifyNeverResumed();
        }

        @Test
        void testApproveRejectsALinkIssuedAfterTheSuspend() throws Exception {
            String pairUuid = UUID.randomUUID()
                .toString();

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, pairUuid, true, 9L))))
                .andExpect(status().isBadRequest());

            verifyNeverResumed();
        }

        @Test
        void testApproveReturnsConflictWhileTheJobIsStillRunning() throws Exception {
            stubJob(Job.Status.STARTED, suspendedMetadata(Map.of()));

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isConflict());

            verifyNeverResumed();
        }

        @Test
        void testGetRendersAConfirmationPageWithoutResuming() throws Exception {
            mockMvc.perform(get("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<form method=\"post\">")));

            verifyNeverResumed();
            verify(jobService, never()).getJob(anyLong());
        }

        @Test
        void testFormPostRendersTheOutcomePage() throws Exception {
            mockMvc.perform(
                post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("your response was recorded")));

            verify(jobFacade).resumeClaimedJob(JOB_ID, TASK_EXECUTION_ID, Map.of("approved", true));
        }

        @Test
        void testApproveOfAnApprovalWithRequiredFormInputsAsksForTheForm() throws Exception {
            stubApprovalWithARequiredInput();

            mockMvc.perform(
                post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))).accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Open the approval form")));

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, true))))
                .andExpect(status().isBadRequest());

            verifyNeverResumed();
        }

        @Test
        void testRejectionOfAnApprovalWithRequiredFormInputsResumesTheJob() throws Exception {
            stubApprovalWithARequiredInput();

            mockMvc.perform(post("/approvals/{id}", sign(ApprovalId.of(TENANT_ID, JOB_ID, false))))
                .andExpect(status().isNoContent());

            verify(jobFacade).resumeClaimedJob(
                JOB_ID, TASK_EXECUTION_ID, Map.of("approved", false, "reason", ""));
        }

        private void stubApprovalWithARequiredInput() {
            stubJob(Job.Status.STOPPED, suspendedMetadata(Map.of(MetadataConstants.APPROVAL_RESUME, true)));

            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setWorkflowTask(
                new WorkflowTask(
                    Map.of(
                        "name", "approval_1", "type", "approval/v1/requestApproval", "parameters",
                        Map.of("inputs", List.of(Map.of("fieldName", "reason", "fieldType", 6, "required", true))))));

            when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));
        }

        private Map<String, Object> suspendedMetadata(Map<String, Object> additionalMetadata) {
            Map<String, Object> metadata = new HashMap<>();

            metadata.put(MetadataConstants.JOB_RESUME_ID, storedJobResumeId);
            metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_ID);

            metadata.putAll(additionalMetadata);

            return metadata;
        }

        private void verifyNeverResumed() {
            verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), anyMap());
            verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
        }
    }

    private String sign(ApprovalId approvalId) {
        return approvalTokens.toSignedToken(approvalId.toString(), Duration.ofHours(1));
    }

    private void stubJob(Job.Status status, Map<String, ?> metadata) {
        Job job = new Job(JOB_ID);

        job.setMetadata(metadata);
        job.setStatus(status);

        when(jobService.getJob(JOB_ID)).thenReturn(job);
    }

    private void stubTaskExecution(Instant expiresAt) {
        TaskExecution taskExecution = new TaskExecution();

        if (expiresAt != null) {
            taskExecution.putMetadata(MetadataConstants.SUSPEND, Map.of("expiresAt", expiresAt.toEpochMilli()));
        }

        when(taskExecutionService.fetchTaskExecution(TASK_EXECUTION_ID)).thenReturn(Optional.of(taskExecution));
    }

    static class ApprovalControllerIntTestConfiguration {

        @Bean
        ApprovalTokens approvalTokens() {
            return new ApprovalTokensImpl(
                Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);
        }

        @Bean
        JobResumeFacade jobResumeFacade(
            ApplicationEventPublisher applicationEventPublisher, ObjectProvider<ApprovalTokens> approvalTokensProvider,
            JobFacade jobFacade, JobService jobService, ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
            TaskExecutionService taskExecutionService) {

            return new JobResumeFacadeImpl(
                applicationEventPublisher, approvalTokensProvider, jobFacade, jobService, meterRegistryObjectProvider,
                TransactionOperations.withoutTransaction(), taskExecutionService, mock(TaskStateService.class));
        }
    }
}
