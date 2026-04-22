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
 * Pins each {@link SafeMessage#of(String)} screening regex with a positive (rejected) and negative (accepted) case. The
 * type's invariants live entirely in the regex strings — an untested regex is an untested invariant, so a regression
 * loosening the 9-digit decimal threshold or dropping the {@code workspace_} prefix from the internal-context list
 * would silently re-open the leak channel that this type exists to close.
 *
 * <p>
 * Coverage map: each test pair targets one of the six screening rules (UUID, long-hex, long-decimal, internal-prefix,
 * SQL-shape, exception-class) plus the legitimate-prose round-trip via {@link SafeMessage#raw(String)}.
 *
 * @author Ivica Cardic
 * @version ee
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
        // Sixteen-character hex string (e.g., a span ID surfaced into a user-facing message) — should be screened.
        assertThatThrownBy(() -> SafeMessage.of("Failed at span deadbeefcafebabe"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsShortHexFragmentsBelowThreshold() {
        // A short hex sequence (8 characters or fewer) is below the LONG_HEX threshold — common in user-facing
        // error codes ("error 0x4F"), should NOT be flagged.
        SafeMessage safeMessage = SafeMessage.of("Encountered error code 0x4F");

        assertThat(safeMessage.value()).isEqualTo("Encountered error code 0x4F");
    }

    @Test
    void testRejectsLongDecimalContent() {
        // 9-digit decimal trips the LONG_DECIMAL screen — typical for primary-key id leaks.
        assertThatThrownBy(() -> SafeMessage.of("Could not load record 123456789"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsShortDecimalsBelowThreshold() {
        // Eight or fewer digits is fine — covers HTTP status codes, year stamps, retry counts.
        SafeMessage safeMessage = SafeMessage.of("Retry attempt 3 of 5 failed");

        assertThat(safeMessage.value()).isEqualTo("Retry attempt 3 of 5 failed");
    }

    @Test
    void testRejectsInternalPrefixTokens() {
        // A literal "workspace_id" / "tenant_id" / etc. token is presumptively a leak from an exception message —
        // the strict factory must refuse it.
        assertThatThrownBy(() -> SafeMessage.of("Error: workspace_id mismatch"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsHumanReadableTerminology() {
        // The INTERNAL_PREFIX screen is word-bounded — "workspace settings" must not match the "workspace_id"
        // prefix rule. The phrase here is also chosen to avoid SQL_SHAPE keywords (no select/insert/update/etc.
        // followed by a word).
        SafeMessage safeMessage = SafeMessage.of("Please review your workspace settings before retrying");

        assertThat(safeMessage.value()).isEqualTo("Please review your workspace settings before retrying");
    }

    @Test
    void testRejectsSqlShapeContent() {
        // SQL-shaped fragments are presumptively leaked statement context.
        assertThatThrownBy(() -> SafeMessage.of("Failed: SELECT count FROM ai_eval_score WHERE workspace = ?"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal context");
    }

    @Test
    void testAcceptsProseWithoutSqlKeywords() {
        // SQL_SHAPE matches a SQL verb followed by any word, so prose like "Please select one" trips it. The
        // negative case here intentionally avoids any of the SQL keywords (select / insert / update / delete /
        // where / join / from) so the screen stays out of the way for typical user-facing copy.
        SafeMessage safeMessage = SafeMessage.of("Choose one of the available options");

        assertThat(safeMessage.value()).isEqualTo("Choose one of the available options");
    }

    @Test
    void testRejectsRawExceptionClassNames() {
        // Exception class names (NullPointerException, IllegalStateException, etc.) belong in logs, not wire bodies.
        // The regex requires the class name be followed by ':' or end-of-string — typical exception-message shape.
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
        // raw(...) is the documented escape hatch — any legitimately-public id bearing message must use it
        // explicitly so the override is visible to reviewers.
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
