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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.ApprovalInputException;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(ObjectMapperSetupExtension.class)
@ContextConfiguration(classes = {
    MattermostInteractivityController.class,
    MattermostInteractivityControllerIntTest.MattermostInteractivityControllerIntTestConfiguration.class
})
@WebMvcTest(controllers = MattermostInteractivityController.class, properties = "bytechef.coordinator.enabled=true")
class MattermostInteractivityControllerIntTest {

    private static final long JOB_ID = 42L;
    private static final String SIGNING_SECRET = EncodingUtils.base64EncodeToString("0123456789abcdef0123456789abcdef");
    private static final String TENANT_ID = "public";

    @Autowired
    private ApprovalTokens approvalTokens;

    @MockitoBean
    private JobResumeFacade jobResumeFacade;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApprovedClickUpdatesTheMessage() throws Exception {
        String signedToken = signedToken();

        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", true))))
            .thenReturn(JobResumeOutcome.OK);

        perform(signedToken, true)
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    "{\"update\":{\"message\":\":white_check_mark: Approved.\"}}", JsonCompareMode.STRICT));

        verify(jobResumeFacade).resumeJob(signedToken, Map.of("approved", true));
    }

    @Test
    void testGoneClickReportsTheExpiry() throws Exception {
        String jobResumeId = JobResumeId.of(TENANT_ID, JOB_ID)
            .toString();
        String signedToken = approvalTokens.toSignedToken(jobResumeId, Duration.ofHours(1));

        givenClosedApproval(Job.Status.FAILED, MetadataConstants.EXPIRED_JOB_RESUME_ID, jobResumeId);

        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", false))))
            .thenReturn(JobResumeOutcome.GONE);

        perform(signedToken, false)
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    "{\"update\":{\"message\":\":hourglass: This approval expired before it was resolved.\"}}",
                    JsonCompareMode.STRICT));
    }

    @Test
    void testGoneClickOnAStoppedRunReportsThatTheApprovalIsNoLongerAvailable() throws Exception {
        String jobResumeId = JobResumeId.of(TENANT_ID, JOB_ID)
            .toString();
        String signedToken = approvalTokens.toSignedToken(jobResumeId, Duration.ofHours(1));

        givenClosedApproval(Job.Status.CANCELLED, MetadataConstants.CANCELLED_JOB_RESUME_ID, jobResumeId);

        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", true))))
            .thenReturn(JobResumeOutcome.GONE);

        perform(signedToken, true)
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    "{\"update\":{\"message\":\":warning: This approval is no longer available.\"}}",
                    JsonCompareMode.STRICT));
    }

    @Test
    void testGoneClickOnACompletedRunReportsTheResolution() throws Exception {
        String signedToken = signedToken();

        Job job = new Job(JOB_ID);

        job.setStatus(Job.Status.COMPLETED);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));
        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", true))))
            .thenReturn(JobResumeOutcome.GONE);

        perform(signedToken, true)
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    "{\"update\":{\"message\":\":information_source: This approval was already resolved.\"}}",
                    JsonCompareMode.STRICT));
    }

    @Test
    void testApprovalNeedingFormInputRepliesWithTheFormHint() throws Exception {
        String signedToken = signedToken();

        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", true))))
            .thenThrow(new ApprovalInputException(List.of("'Amount' is required")));

        perform(signedToken, true)
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{\"ephemeral_text\":\":warning: This approval could not be applied here — open the approval form.\"}",
                JsonCompareMode.STRICT));
    }

    @Test
    void testLostConcurrentResumeRaceReportsTheResolution() throws Exception {
        String signedToken = signedToken();

        Job job = new Job(JOB_ID);

        job.setStatus(Job.Status.COMPLETED);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));
        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", true))))
            .thenThrow(new RuntimeException(new OptimisticLockingFailureException("stale job version")));

        perform(signedToken, true)
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    "{\"update\":{\"message\":\":information_source: This approval was already resolved.\"}}",
                    JsonCompareMode.STRICT));
    }

    @Test
    void testUnexpectedResumeFailureAsksToTryAgain() throws Exception {
        String signedToken = signedToken();

        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", true))))
            .thenThrow(new IllegalStateException("database unavailable"));

        perform(signedToken, true)
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{\"ephemeral_text\":\":warning: This approval could not be applied — try again in a moment.\"}",
                JsonCompareMode.STRICT));
    }

    @Test
    void testNotYetSuspendedClickKeepsTheMessage() throws Exception {
        String signedToken = signedToken();

        when(jobResumeFacade.resumeJob(eq(signedToken), eq(Map.of("approved", true))))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        perform(signedToken, true)
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    "{\"ephemeral_text\":\"Still preparing — try again in a moment.\"}", JsonCompareMode.STRICT));
    }

    @Test
    void testTamperedTokenIsRejectedWithoutResuming() throws Exception {
        String signedToken = signedToken();

        int signatureStart = signedToken.lastIndexOf('.') + 1;
        char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

        String tamperedToken =
            signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);

        perform(tamperedToken, true)
            .andExpect(status().isOk())
            .andExpect(
                content().json(
                    "{\"update\":{\"message\":\"This approval is no longer available.\"}}", JsonCompareMode.STRICT));

        verify(jobResumeFacade, never()).resumeJob(anyString(), anyMap());
        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    @Test
    void testUnsignedTokenIsRejectedWithoutResuming() throws Exception {
        perform(
            JobResumeId.of(TENANT_ID, JOB_ID)
                .toString(),
            true)
                .andExpect(status().isOk())
                .andExpect(
                    content().json(
                        "{\"update\":{\"message\":\"This approval is no longer available.\"}}",
                        JsonCompareMode.STRICT));

        verify(jobResumeFacade, never()).resumeJob(anyString(), anyMap());
    }

    private ResultActions perform(String resumeId, boolean approved)
        throws Exception {

        String body = JsonUtils.write(Map.of("context", Map.of("resumeId", resumeId, "approved", approved)));

        return mockMvc.perform(
            post("/mattermost/interactivity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void givenClosedApproval(Job.Status status, String closureMetadataKey, String jobResumeId) {
        Job job = new Job(JOB_ID);

        job.setMetadata(
            Map.of(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId, closureMetadataKey, jobResumeId));
        job.setStatus(status);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));
    }

    private String signedToken() {
        return approvalTokens.toSignedToken(
            JobResumeId.of(TENANT_ID, JOB_ID)
                .toString(),
            Duration.ofHours(1));
    }

    static class MattermostInteractivityControllerIntTestConfiguration {

        @Bean
        ApprovalTokens approvalTokens() {
            return new ApprovalTokensImpl(
                Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), true);
        }
    }
}
