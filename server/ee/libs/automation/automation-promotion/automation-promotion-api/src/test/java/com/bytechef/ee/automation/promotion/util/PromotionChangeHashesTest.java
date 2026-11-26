/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromotionChangeHashesTest {

    private static final String EMPTY_SET_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    void testEmptySetHashesEmptyString() {
        assertThat(PromotionChangeHashes.destructiveChangesHash(List.of())).isEqualTo(EMPTY_SET_HASH);
    }

    @Test
    void testHashIgnoresOrder() {
        PromotionChange first = new PromotionChange(
            PromotionChange.Kind.DELETE, "COLUMN", "a", "orders", "a", true);
        PromotionChange second = new PromotionChange(
            PromotionChange.Kind.DELETE, "DATA_TABLE", "b", null, "b", true);

        assertThat(PromotionChangeHashes.destructiveChangesHash(List.of(first, second)))
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of(second, first)));
    }

    @Test
    void testHashIgnoresNonDestructive() {
        PromotionChange destructive = new PromotionChange(
            PromotionChange.Kind.DELETE, "COLUMN", "a", "orders", "a", true);
        PromotionChange update = new PromotionChange(
            PromotionChange.Kind.UPDATE, "WORKFLOW", "w", null, "w", false);

        assertThat(PromotionChangeHashes.destructiveChangesHash(List.of(destructive, update)))
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of(destructive)));
    }

    @Test
    void testHashOfSingleDestructiveChange() {
        PromotionChange destructive = new PromotionChange(
            PromotionChange.Kind.DELETE, "COLUMN", "a", "orders", "a", true);

        assertThat(PromotionChangeHashes.destructiveChangesHash(List.of(destructive)))
            .isNotEqualTo(EMPTY_SET_HASH)
            .hasSize(64)
            .matches("[0-9a-f]{64}");
    }

    @Test
    void testNullParentNameHashesAsEmpty() {
        PromotionChange withNull = new PromotionChange(
            PromotionChange.Kind.DELETE, "DATA_TABLE", "t", null, "t", true);
        PromotionChange withEmpty = new PromotionChange(
            PromotionChange.Kind.DELETE, "DATA_TABLE", "t", "", "t", true);

        assertThat(PromotionChangeHashes.destructiveChangesHash(List.of(withNull)))
            .isEqualTo(PromotionChangeHashes.destructiveChangesHash(List.of(withEmpty)));
    }

    @Test
    void testCheckNotStaleNullSkips() {
        assertThatCode(() -> PromotionChangeHashes.checkNotStale(null, "anything")).doesNotThrowAnyException();
    }

    @Test
    void testCheckNotStaleMatchPasses() {
        assertThatCode(() -> PromotionChangeHashes.checkNotStale("abc", "abc")).doesNotThrowAnyException();
    }

    @Test
    void testCheckNotStaleMismatchThrows() {
        assertThatThrownBy(() -> PromotionChangeHashes.checkNotStale("abc", "def"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("The target changed since the preview; review the promotion again")
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey());
    }
}
