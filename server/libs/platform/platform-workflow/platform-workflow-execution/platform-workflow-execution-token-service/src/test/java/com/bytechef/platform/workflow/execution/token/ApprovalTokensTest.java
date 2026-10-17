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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.workflow.execution.JobResumeId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalTokensTest {

    private static final String SECRET = base64Secret("approval-token-secret-key-0001!!");
    private static final String SECRET2 = base64Secret("approval-token-secret-key-0002!!");
    private static final String INNER = "dGVuYW50OjQyOnV1aWQtMTIzOnRydWU=";
    private static final Duration TTL = Duration.ofDays(30);

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final ApprovalTokens approvalTokens = new ApprovalTokensImpl(
        clock, SECRET, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

    @Test
    void testRoundTrip() {
        String token = approvalTokens.toSignedToken(INNER, TTL);

        assertThat(approvalTokens.looksLikeSignedToken(token)).isTrue();
        assertThat(approvalTokens.parseSignedToken(token)).contains(INNER);
    }

    @Test
    void testTamperedPayloadRejected() {
        String token = approvalTokens.toSignedToken(INNER, TTL);

        String[] parts = token.split("\\.");
        String forgedPayload = EncodingUtils.urlEncodeBase64ToString("tenant:99:uuid-123:true");
        String forged = parts[0] + "." + parts[1] + "." + forgedPayload + "." + parts[3];

        assertThat(approvalTokens.parseSignedToken(forged)).isEmpty();
    }

    @Test
    void testTamperedSignatureRejected() {
        String token = approvalTokens.toSignedToken(INNER, TTL);

        assertThat(approvalTokens.parseSignedToken(token + "x")).isEmpty();
    }

    @Test
    void testExpiredRejected() {
        String token = approvalTokens.toSignedToken(INNER, Duration.ofSeconds(10));

        ApprovalTokens later = new ApprovalTokensImpl(
            Clock.fixed(Instant.parse("2026-06-20T01:00:00Z"), ZoneOffset.UTC), SECRET, List.of(), Duration.ofDays(30),
            Duration.ofSeconds(60), false);

        assertThat(later.parseSignedToken(token)).isEmpty();
    }

    @Test
    void testLegacyOrMalformedNotASignedToken() {
        assertThat(approvalTokens.looksLikeSignedToken(INNER)).isFalse();
        assertThat(approvalTokens.parseSignedToken(INNER)).isEmpty();
        assertThat(approvalTokens.parseSignedToken("garbage")).isEmpty();
    }

    @Test
    void testRotationPreviousKeyStillVerifies() {
        String token = approvalTokens.toSignedToken(INNER, TTL);

        ApprovalTokens rotated = new ApprovalTokensImpl(
            clock, SECRET2, List.of(SECRET), Duration.ofDays(30), Duration.ofSeconds(60), false);

        assertThat(rotated.parseSignedToken(token)).contains(INNER);
    }

    @Test
    void testUnconfiguredMintThrowsAndVerifyEmpty() {
        ApprovalTokens unconfigured = new ApprovalTokensImpl(
            clock, null, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

        assertThatThrownBy(() -> unconfigured.toSignedToken(INNER, TTL)).isInstanceOf(IllegalStateException.class);
        assertThat(unconfigured.toSignedTokenIfConfigured(INNER)).isEmpty();
        assertThat(unconfigured.parseSignedToken(approvalTokens.toSignedToken(INNER, TTL))).isEmpty();
    }

    @Test
    void testSignedTokenRequiredFlagExposed() {
        ApprovalTokens required = new ApprovalTokensImpl(
            clock, SECRET, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), true);

        assertThat(required.isSignedTokenRequired()).isTrue();
        assertThat(approvalTokens.isSignedTokenRequired()).isFalse();
    }

    private static String base64Secret(String raw) {
        return EncodingUtils.base64EncodeToString(raw);
    }

    @Test
    void testResolveJobResumeIdUnwrapsSignedToken() {
        JobResumeId jobResumeId = JobResumeId.of("public", 42L);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), TTL);

        assertThat(approvalTokens.resolveJobResumeId(token)).contains(jobResumeId);
        assertThat(approvalTokens.resolveJobResumeId(jobResumeId.toString())).contains(jobResumeId);
        assertThat(approvalTokens.resolveJobResumeId(token + "x")).isEmpty();
        assertThat(approvalTokens.resolveJobResumeId("garbage")).isEmpty();
    }

    @Test
    void testShortTokenRoundTripsToTheSameSignedToken() {
        String token = approvalTokens.toSignedToken(jobResumeId("public", 42L), TTL);

        String shortToken = approvalTokens.toShortToken(token)
            .orElseThrow();

        assertThat(shortToken.length()).isLessThanOrEqualTo(62);
        assertThat(approvalTokens.toShortToken(token)).contains(shortToken);
        assertThat(approvalTokens.parseShortToken(shortToken)).contains(token);
    }

    @Test
    void testShortTokenFitsTelegramCallbackDataForATenantSchema() {
        String token = approvalTokens.toSignedToken(jobResumeId("000001", 1_000_000_000L), TTL);

        String shortToken = approvalTokens.toShortToken(token)
            .orElseThrow();

        assertThat((shortToken + ":a").length()).isLessThanOrEqualTo(64);
        assertThat(approvalTokens.parseShortToken(shortToken)).contains(token);
    }

    @Test
    void testShortTokenIsEmptyWhenItWouldNotFitTelegramCallbackData() {
        String token = approvalTokens.toSignedToken(jobResumeId("public", Long.MAX_VALUE), TTL);

        assertThat(approvalTokens.toShortToken(token)).isEmpty();
    }

    @Test
    void testShortTokenRejectsTokensItCannotVerify() {
        String token = approvalTokens.toSignedToken(jobResumeId("public", 42L), TTL);

        assertThat(approvalTokens.toShortToken(token + "x")).isEmpty();
        assertThat(approvalTokens.toShortToken(jobResumeId("public", 42L))).isEmpty();
        assertThat(approvalTokens.toShortToken(approvalTokens.toSignedToken(INNER, TTL))).isEmpty();
        assertThat(approvalTokens.toShortToken("")).isEmpty();
    }

    @Test
    void testTamperedShortTokenIsRejected() {
        String token = approvalTokens.toSignedToken(jobResumeId("public", 42L), TTL);

        String shortToken = approvalTokens.toShortToken(token)
            .orElseThrow();

        char first = shortToken.charAt(0);
        String tamperedShortToken = (first == 'A' ? 'B' : 'A') + shortToken.substring(1);

        assertThat(approvalTokens.parseShortToken(tamperedShortToken)).isEmpty();
        assertThat(approvalTokens.parseShortToken(shortToken.substring(1))).isEmpty();
        assertThat(approvalTokens.parseShortToken("garbage")).isEmpty();
        assertThat(approvalTokens.parseShortToken("")).isEmpty();
    }

    @Test
    void testExpiredShortTokenIsRejected() {
        String token = approvalTokens.toSignedToken(jobResumeId("public", 42L), Duration.ofHours(1));

        String shortToken = approvalTokens.toShortToken(token)
            .orElseThrow();

        Clock later = Clock.offset(clock, Duration.ofMinutes(62));

        ApprovalTokens laterApprovalTokens = new ApprovalTokensImpl(
            later, SECRET, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

        assertThat(laterApprovalTokens.parseShortToken(shortToken)).isEmpty();
    }

    @Test
    void testShortTokenMintedWithARotatedOutSecretIsResignedWithTheActiveSecret() {
        JobResumeId jobResumeId = JobResumeId.of("public", 42L);

        String shortToken = approvalTokens.toShortToken(approvalTokens.toSignedToken(jobResumeId.toString(), TTL))
            .orElseThrow();

        ApprovalTokens rotatedApprovalTokens = new ApprovalTokensImpl(
            clock, SECRET2, List.of(SECRET), Duration.ofDays(30), Duration.ofSeconds(60), false);

        String token = rotatedApprovalTokens.parseShortToken(shortToken)
            .orElseThrow();

        assertThat(rotatedApprovalTokens.resolveJobResumeId(token)).contains(jobResumeId);
        assertThat(approvalTokens.parseShortToken(shortToken)).isPresent();

        ApprovalTokens otherApprovalTokens = new ApprovalTokensImpl(
            clock, SECRET2, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

        assertThat(otherApprovalTokens.parseShortToken(shortToken)).isEmpty();
    }

    @Test
    void testShortTokensNeedASigningSecret() {
        ApprovalTokens unconfigured = new ApprovalTokensImpl(
            clock, null, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

        String token = approvalTokens.toSignedToken(jobResumeId("public", 42L), TTL);

        String shortToken = approvalTokens.toShortToken(token)
            .orElseThrow();

        assertThat(unconfigured.parseShortToken(shortToken)).isEmpty();
        assertThat(unconfigured.toShortToken(jobResumeId("public", 42L))).isEmpty();
    }

    private static String jobResumeId(String tenantId, long jobId) {
        JobResumeId jobResumeId = JobResumeId.of(tenantId, jobId);

        return jobResumeId.toString();
    }
}
