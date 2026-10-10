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
}
