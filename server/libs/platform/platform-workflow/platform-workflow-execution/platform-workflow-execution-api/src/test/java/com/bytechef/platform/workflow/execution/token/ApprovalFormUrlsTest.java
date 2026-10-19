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

package com.bytechef.platform.workflow.execution.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class ApprovalFormUrlsTest {

    @Test
    void testSignedTokenIsUsedWhenSignerConfigured() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured("inner")).thenReturn(Optional.of("v1.signed"));

        assertThat(ApprovalFormUrls.buildFormUrl("https://example.com", "inner", approvalTokens))
            .contains("https://example.com/resume/v1.signed");
    }

    @Test
    void testFormUrlTokenExpiresWithTheApprovalPlusTheMargin() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured(eq("inner"), any(Duration.class)))
            .thenReturn(Optional.of("v1.signed"));

        Instant expiresAt = Instant.now()
            .plus(Duration.ofDays(2));

        assertThat(ApprovalFormUrls.buildFormUrl("https://example.com", "inner", approvalTokens, expiresAt))
            .contains("https://example.com/resume/v1.signed");

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(approvalTokens).toSignedTokenIfConfigured(eq("inner"), ttlCaptor.capture());

        assertThat(ttlCaptor.getValue())
            .isBetween(Duration.ofDays(2), Duration.ofDays(2)
                .plus(ApprovalFormUrls.APPROVAL_TOKEN_TTL_MARGIN));
        assertThat(ttlCaptor.getValue()).isGreaterThan(Duration.ofDays(2)
            .plus(Duration.ofMinutes(59)));
    }

    @Test
    void testFormUrlTokenOfAnExpiredApprovalOnlyGetsTheMargin() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured("inner", ApprovalFormUrls.APPROVAL_TOKEN_TTL_MARGIN))
            .thenReturn(Optional.of("v1.signed"));

        assertThat(
            ApprovalFormUrls.buildFormUrl(
                "https://example.com", "inner", approvalTokens, Instant.now()
                    .minusSeconds(60)))
                        .contains("https://example.com/resume/v1.signed");
    }

    @Test
    void testFormUrlTokenWithoutExpiryGetsTheDefaultTtl() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured("inner")).thenReturn(Optional.of("v1.signed"));

        assertThat(ApprovalFormUrls.buildFormUrl("https://example.com", "inner", approvalTokens, null))
            .contains("https://example.com/resume/v1.signed");
    }

    @Test
    void testLegacyInnerTokenFallsThroughWithoutSigner() {
        assertThat(ApprovalFormUrls.buildFormUrl("https://example.com", "inner", null))
            .contains("https://example.com/resume/inner");
    }

    @Test
    void testEmptyWithoutPublicUrlOrResumeId() {
        assertThat(ApprovalFormUrls.buildFormUrl(null, "inner", null)).isEmpty();
        assertThat(ApprovalFormUrls.buildFormUrl("https://example.com", null, null)).isEmpty();
        assertThat(ApprovalFormUrls.buildFormUrl(" ", "inner", null)).isEmpty();
    }

    @Test
    void testResumeTokenIsSignedAndIndependentOfPublicUrl() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured("inner")).thenReturn(Optional.of("v1.signed"));

        assertThat(ApprovalFormUrls.buildResumeToken("inner", approvalTokens)).contains("v1.signed");
    }

    @Test
    void testResumeTokenFallsBackToInnerTokenWithoutSigner() {
        assertThat(ApprovalFormUrls.buildResumeToken("inner", null)).contains("inner");
    }

    @Test
    void testResumeTokenEmptyWithoutResumeId() {
        assertThat(ApprovalFormUrls.buildResumeToken(null, null)).isEmpty();
        assertThat(ApprovalFormUrls.buildResumeToken(" ", null)).isEmpty();
    }
}
