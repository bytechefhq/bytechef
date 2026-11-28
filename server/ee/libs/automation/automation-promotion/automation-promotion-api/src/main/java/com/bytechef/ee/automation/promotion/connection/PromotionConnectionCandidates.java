/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.connection;

import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PromotionConnectionCandidates {

    private PromotionConnectionCandidates() {
    }

    public static @Nullable Long suggestByName(
        String sourceConnectionName, List<PromotionConnectionCandidate> candidates) {

        List<PromotionConnectionCandidate> sameNameCandidates = candidates.stream()
            .filter(candidate -> Objects.equals(candidate.name(), sourceConnectionName))
            .toList();

        if (sameNameCandidates.size() != 1) {
            return null;
        }

        PromotionConnectionCandidate sameNameCandidate = sameNameCandidates.getFirst();

        return sameNameCandidate.connectionId();
    }

    public static void checkIsCandidate(long targetConnectionId, List<PromotionConnectionCandidate> candidates) {
        boolean isCandidate = candidates.stream()
            .anyMatch(candidate -> candidate.connectionId() == targetConnectionId);

        if (!isCandidate) {
            throw new ConfigurationException(
                "Target connection is not a candidate for this promotion",
                EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID);
        }
    }
}
