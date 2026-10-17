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

package com.bytechef.platform.workflow.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.workflow.execution.config.ApprovalShortTokenIntTestConfiguration;
import com.bytechef.platform.workflow.execution.domain.ApprovalShortToken;
import com.bytechef.platform.workflow.execution.repository.ApprovalShortTokenRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = ApprovalShortTokenIntTestConfiguration.class)
class ApprovalShortTokenServiceIntTest {

    @MockitoSpyBean
    private ApprovalShortTokenRepository approvalShortTokenRepository;

    @Autowired
    private ApprovalShortTokenService approvalShortTokenService;

    @AfterEach
    void afterEach() {
        approvalShortTokenRepository.deleteAll();
    }

    @Test
    void testMintedShortIdResolvesToItsToken() {
        String shortId = approvalShortTokenService.mint("v1.1.token-a.signature", inOneHour());

        assertThat(shortId).hasSizeLessThanOrEqualTo(32)
            .doesNotContain(":");
        assertThat(approvalShortTokenService.fetchToken(shortId)).contains("v1.1.token-a.signature");
    }

    @Test
    void testMintingTheSameTokenTwiceReturnsTheSameShortId() {
        String shortId = approvalShortTokenService.mint("v1.1.token-a.signature", inOneHour());

        assertThat(approvalShortTokenService.mint("v1.1.token-a.signature", inOneHour())).isEqualTo(shortId);
        assertThat(approvalShortTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void testDifferentTokensGetDifferentShortIds() {
        String shortId = approvalShortTokenService.mint("v1.1.token-a.signature", inOneHour());

        assertThat(approvalShortTokenService.mint("v1.1.token-b.signature", inOneHour())).isNotEqualTo(shortId);
    }

    @Test
    void testShortIdCollisionIsRetriedWithAFreshShortId() {
        AtomicReference<String> firstShortId = new AtomicReference<>();

        doThrow(new DuplicateKeyException("approval_short_token_short_id_key"))
            .when(approvalShortTokenRepository)
            .save(argThat(approvalShortToken -> isFirstAttempt(approvalShortToken, firstShortId)));

        String shortId = approvalShortTokenService.mint("v1.1.token-e.signature", inOneHour());

        assertThat(shortId).isNotEqualTo(firstShortId.get());
        assertThat(approvalShortTokenService.fetchToken(shortId)).contains("v1.1.token-e.signature");
        assertThat(approvalShortTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void testRepeatedShortIdCollisionsAreRethrown() {
        doThrow(new DuplicateKeyException("approval_short_token_short_id_key"))
            .when(approvalShortTokenRepository)
            .save(any(ApprovalShortToken.class));

        assertThatThrownBy(() -> approvalShortTokenService.mint("v1.1.token-f.signature", inOneHour()))
            .hasRootCauseInstanceOf(DuplicateKeyException.class);

        verify(approvalShortTokenRepository, times(2)).save(any(ApprovalShortToken.class));
    }

    @Test
    void testConcurrentMintOfTheSameTokenReturnsTheWinningShortId() {
        AtomicReference<String> firstShortId = new AtomicReference<>();

        doAnswer(invocation -> {
            ApprovalShortToken attemptedApprovalShortToken = invocation.getArgument(0);

            approvalShortTokenRepository.save(
                new ApprovalShortToken(
                    "winning-id", attemptedApprovalShortToken.getToken(), attemptedApprovalShortToken.getTokenHash(),
                    attemptedApprovalShortToken.getExpiryDate()));

            throw new DuplicateKeyException("approval_short_token_token_hash_key");
        }).when(approvalShortTokenRepository)
            .save(argThat(approvalShortToken -> isFirstAttempt(approvalShortToken, firstShortId)));

        String shortId = approvalShortTokenService.mint("v1.1.token-g.signature", inOneHour());

        assertThat(shortId).isEqualTo("winning-id");
        assertThat(approvalShortTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void testConstructorRejectsMissingFields() {
        Instant expiryDate = inOneHour();

        assertThatThrownBy(() -> new ApprovalShortToken(null, "token", "hash", expiryDate))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ApprovalShortToken("short", null, "hash", expiryDate))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ApprovalShortToken("short", "token", null, expiryDate))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ApprovalShortToken("short", "token", "hash", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testUnknownShortIdResolvesToNothing() {
        assertThat(approvalShortTokenService.fetchToken("unknown")).isEmpty();
    }

    @Test
    void testExpiredShortIdResolvesToNothing() {
        ApprovalShortToken approvalShortToken = approvalShortTokenRepository.save(
            new ApprovalShortToken("expired-id", "v1.1.token-c.signature", "hash-c", oneHourAgo()));

        assertThat(approvalShortTokenService.fetchToken(approvalShortToken.getShortId())).isEmpty();
    }

    @Test
    void testDeleteExpiredRemovesOnlyExpiredEntries() {
        approvalShortTokenRepository.save(
            new ApprovalShortToken("expired-id", "v1.1.token-c.signature", "hash-c", oneHourAgo()));

        String liveShortId = approvalShortTokenService.mint("v1.1.token-d.signature", inOneHour());

        approvalShortTokenService.deleteExpired();

        assertThat(approvalShortTokenRepository.findByShortId("expired-id")).isEmpty();
        assertThat(approvalShortTokenService.fetchToken(liveShortId)).contains("v1.1.token-d.signature");
    }

    private static boolean isFirstAttempt(
        ApprovalShortToken approvalShortToken, AtomicReference<String> firstShortId) {

        if (approvalShortToken == null) {
            return false;
        }

        firstShortId.compareAndSet(null, approvalShortToken.getShortId());

        return approvalShortToken.getShortId()
            .equals(firstShortId.get());
    }

    private static Instant inOneHour() {
        Instant now = Instant.now();

        return now.plus(1, ChronoUnit.HOURS)
            .truncatedTo(ChronoUnit.SECONDS);
    }

    private static Instant oneHourAgo() {
        Instant now = Instant.now();

        return now.minus(1, ChronoUnit.HOURS)
            .truncatedTo(ChronoUnit.SECONDS);
    }
}
