/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromotionConnectionCandidatesTest {

    @Test
    void testSuggestByNameReturnsTheIdOfTheOnlyCandidateWithThatName() {
        List<PromotionConnectionCandidate> candidates = List.of(
            new PromotionConnectionCandidate(21L, "Slack"), new PromotionConnectionCandidate(22L, "Sheets"));

        assertThat(PromotionConnectionCandidates.suggestByName("Sheets", candidates)).isEqualTo(22L);
    }

    @Test
    void testSuggestByNameReturnsNullWhenNoCandidateHasThatName() {
        List<PromotionConnectionCandidate> candidates = List.of(new PromotionConnectionCandidate(21L, "Slack"));

        assertThat(PromotionConnectionCandidates.suggestByName("Sheets", candidates)).isNull();
    }

    @Test
    void testSuggestByNameReturnsNullWhenTwoCandidatesHaveThatName() {
        List<PromotionConnectionCandidate> candidates = List.of(
            new PromotionConnectionCandidate(21L, "Sheets"), new PromotionConnectionCandidate(22L, "Sheets"));

        assertThat(PromotionConnectionCandidates.suggestByName("Sheets", candidates)).isNull();
    }

    @Test
    void testSuggestByNameIsCaseSensitive() {
        List<PromotionConnectionCandidate> candidates = List.of(new PromotionConnectionCandidate(21L, "sheets"));

        assertThat(PromotionConnectionCandidates.suggestByName("Sheets", candidates)).isNull();
    }

    @Test
    void testCheckIsCandidateAcceptsAnIdInTheList() {
        List<PromotionConnectionCandidate> candidates = List.of(new PromotionConnectionCandidate(21L, "Slack"));

        assertThatCode(() -> PromotionConnectionCandidates.checkIsCandidate(21L, candidates))
            .doesNotThrowAnyException();
    }

    @Test
    void testCheckIsCandidateRejectsAnIdNotInTheList() {
        List<PromotionConnectionCandidate> candidates = List.of(new PromotionConnectionCandidate(21L, "Slack"));

        assertThatThrownBy(() -> PromotionConnectionCandidates.checkIsCandidate(99L, candidates))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());
    }
}
