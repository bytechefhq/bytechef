/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SafeMessageTest {

    @Test
    void testRejectsUuidLikeContent() {
        assertThatThrownBy(() -> SafeMessage.of(
            "Request 550e8400-e29b-41d4-a716-446655440000 was rejected"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsContentWithoutUuids() {
        SafeMessage safeMessage = SafeMessage.of("Request was rejected: invalid input format");

        assertThat(safeMessage.value()).isEqualTo("Request was rejected: invalid input format");
    }

    @Test
    void testRejectsLongHexContent() {
        assertThatThrownBy(() -> SafeMessage.of("Failed at span deadbeefcafebabe"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsShortHexFragmentsBelowThreshold() {
        SafeMessage safeMessage = SafeMessage.of("Encountered error code 0x4F");

        assertThat(safeMessage.value()).isEqualTo("Encountered error code 0x4F");
    }

    @Test
    void testRejectsLongDecimalContent() {
        assertThatThrownBy(() -> SafeMessage.of("Could not load record 123456789"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsShortDecimalsBelowThreshold() {
        SafeMessage safeMessage = SafeMessage.of("Retry attempt 3 of 5 failed");

        assertThat(safeMessage.value()).isEqualTo("Retry attempt 3 of 5 failed");
    }

    @Test
    void testRejectsInternalPrefixTokens() {
        assertThatThrownBy(() -> SafeMessage.of("Error: workspace_id mismatch"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsHumanReadableTerminology() {
        SafeMessage safeMessage = SafeMessage.of("Please review your workspace settings before retrying");

        assertThat(safeMessage.value()).isEqualTo("Please review your workspace settings before retrying");
    }

    @Test
    void testRejectsSqlShapeContent() {
        assertThatThrownBy(() -> SafeMessage.of("Failed: SELECT count FROM ai_eval_score WHERE workspace = ?"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsProseWithoutSqlKeywords() {
        SafeMessage safeMessage = SafeMessage.of("Choose one of the available options");

        assertThat(safeMessage.value()).isEqualTo("Choose one of the available options");
    }

    @Test
    void testRejectsRawExceptionClassNames() {
        assertThatThrownBy(() -> SafeMessage.of("NullPointerException: cannot read field"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsProseWithoutClassNames() {
        SafeMessage safeMessage = SafeMessage.of("The request could not be processed at this time");

        assertThat(safeMessage.value()).isEqualTo("The request could not be processed at this time");
    }

    @Test
    void testRawBypassesScreening() {
        SafeMessage safeMessage = SafeMessage.raw("Customer-visible reference: 550e8400-e29b-41d4-a716-446655440000");

        assertThat(safeMessage.value())
            .isEqualTo("Customer-visible reference: 550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void testRejectsBlankAndNullInputs() {
        assertThatThrownBy(() -> SafeMessage.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> SafeMessage.of(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> SafeMessage.of("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
    }

    @Test
    void testEqualsAndHashCodeOnIdenticalValues() {
        SafeMessage left = SafeMessage.of("hello world");
        SafeMessage right = SafeMessage.of("hello world");

        assertThat(left).isEqualTo(right);
        assertThat(left).hasSameHashCodeAs(right);
    }
}
