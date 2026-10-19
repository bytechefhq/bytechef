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

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.workflow.execution.domain.ApprovalShortToken;
import com.bytechef.platform.workflow.execution.repository.ApprovalShortTokenRepository;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ApprovalShortTokenServiceImpl implements ApprovalShortTokenService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalShortTokenServiceImpl.class);

    private static final int MAX_MINT_ATTEMPTS = 2;
    private static final int SHORT_ID_BYTES = 16;

    private final ApprovalShortTokenRepository approvalShortTokenRepository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public ApprovalShortTokenServiceImpl(
        ApprovalShortTokenRepository approvalShortTokenRepository, ObjectProvider<Clock> clockProvider) {

        this.approvalShortTokenRepository = approvalShortTokenRepository;
        this.clock = clockProvider.getIfAvailable(Clock::systemUTC);
    }

    @Override
    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT1H")
    public void deleteExpired() {
        int deletedCount = TenantContext.callWithTenantId(
            TenantContext.DEFAULT_TENANT_ID,
            () -> approvalShortTokenRepository.deleteByExpiryDateBefore(clock.instant()));

        if (deletedCount > 0) {
            log.debug("Deleted {} expired approval short tokens", deletedCount);
        }
    }

    @Override
    public Optional<String> fetchToken(String shortId) {
        Instant now = clock.instant();

        return TenantContext.callWithTenantId(
            TenantContext.DEFAULT_TENANT_ID,
            () -> approvalShortTokenRepository.findByShortId(shortId)
                .filter(approvalShortToken -> !approvalShortToken.isExpired(now))
                .map(ApprovalShortToken::getToken));
    }

    @Override
    public String mint(String token, Instant expiryDate) {
        String tokenHash = sha256Hex(token);

        return TenantContext.callWithTenantId(TenantContext.DEFAULT_TENANT_ID, () -> {
            Optional<ApprovalShortToken> existingApprovalShortToken =
                approvalShortTokenRepository.findByTokenHash(tokenHash);

            if (existingApprovalShortToken.isPresent()) {
                return existingApprovalShortToken.get()
                    .getShortId();
            }

            for (int attempt = 1;; attempt++) {
                ApprovalShortToken approvalShortToken = new ApprovalShortToken(
                    generateShortId(), token, tokenHash, expiryDate);

                try {
                    approvalShortToken = approvalShortTokenRepository.save(approvalShortToken);

                    return approvalShortToken.getShortId();
                } catch (DuplicateKeyException duplicateKeyException) {
                    Optional<ApprovalShortToken> concurrentApprovalShortToken =
                        approvalShortTokenRepository.findByTokenHash(tokenHash);

                    if (concurrentApprovalShortToken.isPresent()) {
                        log.debug("A concurrent request minted the same approval short token first");

                        ApprovalShortToken winningApprovalShortToken = concurrentApprovalShortToken.get();

                        return winningApprovalShortToken.getShortId();
                    }

                    if (attempt >= MAX_MINT_ATTEMPTS) {
                        throw duplicateKeyException;
                    }

                    log.debug("Approval short id collided with an existing one; retrying with a fresh short id");
                }
            }
        });
    }

    private String generateShortId() {
        byte[] bytes = new byte[SHORT_ID_BYTES];

        secureRandom.nextBytes(bytes);

        return EncodingUtils.urlEncodeBase64ToString(bytes);
    }

    private static String sha256Hex(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(messageDigest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 is unavailable", noSuchAlgorithmException);
        }
    }
}
