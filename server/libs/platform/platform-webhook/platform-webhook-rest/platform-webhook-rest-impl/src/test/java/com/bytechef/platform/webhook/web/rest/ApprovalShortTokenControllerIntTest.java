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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(ObjectMapperSetupExtension.class)
@ContextConfiguration(classes = {
    ApprovalShortTokenController.class,
    ApprovalShortTokenControllerIntTest.ApprovalShortTokenControllerIntTestConfiguration.class
})
@WebMvcTest(controllers = ApprovalShortTokenController.class, properties = "bytechef.coordinator.enabled=true")
class ApprovalShortTokenControllerIntTest {

    private static final String OTHER_SECRET = EncodingUtils.base64EncodeToString("fedcba9876543210fedcba9876543210");
    private static final String SIGNING_SECRET = EncodingUtils.base64EncodeToString("0123456789abcdef0123456789abcdef");

    @MockitoBean
    private ApprovalShortTokenService approvalShortTokenService;

    @Autowired
    private ApprovalTokens approvalTokens;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSignedTokenIsMintedUntilItsOwnExpiry() throws Exception {
        String signedToken = approvalTokens.toSignedToken(jobResumeId(), Duration.ofHours(2));

        Instant expiryDate = Instant.ofEpochSecond(Long.parseLong(signedToken.split("\\.")[1]));

        when(approvalShortTokenService.mint(signedToken, expiryDate)).thenReturn("short-id");

        mockMvc.perform(mint(signedToken))
            .andExpect(status().isOk())
            .andExpect(content().string("short-id"));

        verify(approvalShortTokenService).mint(signedToken, expiryDate);
    }

    @Test
    void testUnsignedTokenIsRejected() throws Exception {
        mockMvc.perform(mint(jobResumeId()))
            .andExpect(status().isBadRequest());

        verifyNeverMinted();
    }

    @Test
    void testTokenSignedWithAnotherSecretIsRejected() throws Exception {
        ApprovalTokens otherApprovalTokens = new ApprovalTokensImpl(
            Clock.systemUTC(), OTHER_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);

        mockMvc.perform(mint(otherApprovalTokens.toSignedToken(jobResumeId(), Duration.ofHours(1))))
            .andExpect(status().isBadRequest());

        verifyNeverMinted();
    }

    @Test
    void testExpiredTokenIsRejected() throws Exception {
        ApprovalTokens pastApprovalTokens = new ApprovalTokensImpl(
            Clock.offset(Clock.systemUTC(), Duration.ofHours(-3)), SIGNING_SECRET, List.of(), Duration.ofHours(1),
            Duration.ofSeconds(30), false);

        mockMvc.perform(mint(pastApprovalTokens.toSignedToken(jobResumeId(), Duration.ofHours(1))))
            .andExpect(status().isBadRequest());

        verifyNeverMinted();
    }

    @Test
    void testSignedTokenThatIsNotAResumeIdIsRejected() throws Exception {
        mockMvc.perform(mint(approvalTokens.toSignedToken("not-a-resume-id", Duration.ofHours(1))))
            .andExpect(status().isBadRequest());

        verifyNeverMinted();
    }

    @Test
    void testBlankTokenIsRejected() throws Exception {
        mockMvc.perform(mint(" "))
            .andExpect(status().isBadRequest());

        verifyNeverMinted();
    }

    private void verifyNeverMinted() {
        verify(approvalShortTokenService, never()).mint(anyString(), any());
    }

    private static RequestBuilder mint(String token) {
        return post("/approval/short-token")
            .contentType(MediaType.TEXT_PLAIN)
            .content(token);
    }

    private static String jobResumeId() {
        return JobResumeId.of("public", 42L)
            .toString();
    }

    static class ApprovalShortTokenControllerIntTestConfiguration {

        @Bean
        ApprovalTokens approvalTokens() {
            return new ApprovalTokensImpl(
                Clock.systemUTC(), SIGNING_SECRET, List.of(), Duration.ofHours(1), Duration.ofSeconds(30), false);
        }
    }
}
