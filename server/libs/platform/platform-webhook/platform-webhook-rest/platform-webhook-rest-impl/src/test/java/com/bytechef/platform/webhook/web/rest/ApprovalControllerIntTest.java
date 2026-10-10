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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    ApprovalController.class, ApprovalControllerIntTest.ApprovalTokensConfiguration.class
})
@WebMvcTest(controllers = ApprovalController.class, properties = "bytechef.coordinator.enabled=true")
class ApprovalControllerIntTest {

    private static final long JOB_ID = 42L;
    private static final String SIGNING_SECRET = EncodingUtils.base64EncodeToString("0123456789abcdef0123456789abcdef");

    @Autowired
    private ApprovalTokens approvalTokens;

    @MockitoBean
    private JobFacade jobFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApproveResumesJobForSignedToken() throws Exception {
        ApprovalId approvalId = ApprovalId.of(JOB_ID, true);

        mockMvc
            .perform(post("/approvals/{id}", approvalTokens.toSignedToken(approvalId.toString(), Duration.ofHours(1))))
            .andExpect(status().isNoContent());

        verify(jobFacade).resumeApproval(JOB_ID, approvalId.getUuidAsString(), true);
    }

    @Test
    void testApproveRejectsTamperedSignedToken() throws Exception {
        String signedToken = approvalTokens.toSignedToken(
            ApprovalId.of(JOB_ID, true)
                .toString(),
            Duration.ofHours(1));

        int signatureStart = signedToken.lastIndexOf('.') + 1;
        char replacement = signedToken.charAt(signatureStart) == 'A' ? 'B' : 'A';

        String tamperedToken =
            signedToken.substring(0, signatureStart) + replacement + signedToken.substring(signatureStart + 1);

        mockMvc.perform(post("/approvals/{id}", tamperedToken))
            .andExpect(status().isBadRequest());

        verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testApproveResumesJobForLegacyUnsignedToken() throws Exception {
        ApprovalId approvalId = ApprovalId.of(JOB_ID, false);

        mockMvc.perform(post("/approvals/{id}", approvalId.toString()))
            .andExpect(status().isNoContent());

        verify(jobFacade).resumeApproval(JOB_ID, approvalId.getUuidAsString(), false);
    }

    @Test
    void testApproveRejectsMalformedToken() throws Exception {
        mockMvc.perform(post("/approvals/{id}", "not-a-token"))
            .andExpect(status().isBadRequest());

        verify(jobFacade, never()).resumeApproval(anyLong(), anyString(), anyBoolean());
    }

    static class ApprovalTokensConfiguration {

        @Bean
        ApprovalTokens approvalTokens() {
            return new ApprovalTokensImpl(
                Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);
        }
    }
}
