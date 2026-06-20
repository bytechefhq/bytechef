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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies the approval-token signer: round-trip, tamper rejection (the whole point — a forged inner token must not
 * verify), expiry, rotation, and legacy/unconfigured behavior.
 *
 * @author Ivica Cardic
 */
class ApprovalTokensImplTest {

    private static final String SECRET = base64Secret("approval-token-secret-key-0001!!");
    private static final String SECRET2 = base64Secret("approval-token-secret-key-0002!!");
    private static final String INNER = "dGVuYW50OjQyOnV1aWQtMTIzOnRydWU="; // base64("tenant:42:uuid-123:true")

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final ApprovalTokens approvalTokens = new ApprovalTokensImpl(
        clock, SECRET, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

    @Test
    void testRoundTrip() {
        String token = approvalTokens.toSignedToken(INNER);

        assertThat(approvalTokens.looksLikeSignedToken(token)).isTrue();
        assertThat(approvalTokens.parseSignedToken(token)).contains(INNER);
    }

    @Test
    void testTamperedPayloadRejected() {
        String token = approvalTokens.toSignedToken(INNER);

        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("tenant:99:uuid-123:true".getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + parts[1] + "." + forgedPayload + "." + parts[3];

        assertThat(approvalTokens.parseSignedToken(forged)).isEmpty();
    }

    @Test
    void testTamperedSignatureRejected() {
        String token = approvalTokens.toSignedToken(INNER);

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
        String token = approvalTokens.toSignedToken(INNER);

        // Active key rotated to SECRET2, old SECRET kept as previous.
        ApprovalTokens rotated = new ApprovalTokensImpl(
            clock, SECRET2, List.of(SECRET), Duration.ofDays(30), Duration.ofSeconds(60), false);

        assertThat(rotated.parseSignedToken(token)).contains(INNER);
    }

    @Test
    void testUnconfiguredMintThrowsAndVerifyEmpty() {
        ApprovalTokens unconfigured = new ApprovalTokensImpl(
            clock, null, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), false);

        assertThatThrownBy(() -> unconfigured.toSignedToken(INNER)).isInstanceOf(IllegalStateException.class);
        assertThat(unconfigured.toSignedTokenIfConfigured(INNER)).isEmpty();
        assertThat(unconfigured.parseSignedToken(approvalTokens.toSignedToken(INNER))).isEmpty();
    }

    @Test
    void testSignedTokenRequiredFlagExposed() {
        ApprovalTokens required = new ApprovalTokensImpl(
            clock, SECRET, List.of(), Duration.ofDays(30), Duration.ofSeconds(60), true);

        assertThat(required.isSignedTokenRequired()).isTrue();
        assertThat(approvalTokens.isSignedTokenRequired()).isFalse();
    }

    private static String base64Secret(String raw) {
        return Base64.getEncoder()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
