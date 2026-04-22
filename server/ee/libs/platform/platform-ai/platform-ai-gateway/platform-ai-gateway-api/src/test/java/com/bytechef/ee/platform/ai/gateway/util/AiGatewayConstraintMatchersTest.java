/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/**
 * Pins the word-boundary matching contract and the early-return path on non-Postgres causes. The reflective probe
 * against {@code org.postgresql.util.PSQLException#getServerErrorMessage()} is exercised end-to-end by
 * {@code AiObservabilityOtlpIngestFacadeIntTest} against a real Postgres container — that suite is the only place where
 * a real {@code PSQLException} cause can be observed without pulling the driver onto the unit-test classpath.
 *
 * <p>
 * The regressions this unit test prevents:
 * <ul>
 * <li><strong>Naive substring matching</strong>: a future index named {@code <existing>_v2} silently matching the
 * original constraint name. Word boundaries reject the prefix overlap; without them, dedup bookkeeping would mis-route
 * a different schema violation as the original constraint hit.</li>
 * <li><strong>False-positive on non-Postgres causes</strong>: the early return on a different exception class must NOT
 * fall through into the message-fallback path with a constraint name that happens to appear in an unrelated
 * message.</li>
 * <li><strong>Cache stability</strong>: repeated calls share the same cached {@link Pattern}.</li>
 * </ul>
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiGatewayConstraintMatchersTest {

    private static final String CONSTRAINT = "ux_ai_obs_span_trace_external_span";

    private static final Pattern PATTERN = AiGatewayConstraintMatchers.wordBoundaryPattern(CONSTRAINT);

    @Test
    void testMatchesConstraintWhenMessageContainsExactName() {
        DuplicateKeyException exception = new DuplicateKeyException(
            "duplicate key value violates unique constraint \"" + CONSTRAINT + "\"");

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isTrue();
    }

    @Test
    void testRejectsConstraintNameWithSuffixV2() {
        // A future schema migration adding a parallel index named `<existing>_v2` must NOT match the original. With
        // naive String#contains the longer name would silently match the shorter one and corrupt dedup bookkeeping —
        // the second index would be bucketed as the first index's hit, masking real persist failures.
        DuplicateKeyException exception = new DuplicateKeyException(
            "duplicate key value violates unique constraint \"" + CONSTRAINT + "_v2\"");

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .as("Word boundary must reject prefix overlap with _v2 suffix")
            .isFalse();
    }

    @Test
    void testRejectsConstraintNameWithPrefix() {
        DuplicateKeyException exception = new DuplicateKeyException(
            "duplicate key value violates unique constraint \"prefix_" + CONSTRAINT + "\"");

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .as("Word boundary must reject suffix overlap with prefix_ in front")
            .isFalse();
    }

    @Test
    void testReturnsFalseWhenMessageDoesNotContainConstraint() {
        DuplicateKeyException exception = new DuplicateKeyException(
            "some unrelated error involving a different constraint");

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isFalse();
    }

    @Test
    void testReturnsFalseWhenCauseIsNonPostgresClass() {
        // Cause is a plain RuntimeException — getClass().getName() does not equal the PSQLException name, so the
        // structured-probe path returns false on every cause-chain element. Falls through to message matching, which
        // also fails because the message does not contain the constraint name. No false positives from the early
        // return path.
        DuplicateKeyException exception = new DuplicateKeyException(
            "wrapper message", new RuntimeException("inner: not postgres"));

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isFalse();
    }

    @Test
    void testWordBoundaryPatternIsStable() {
        // The Javadoc tells callers to cache patterns at the call site (compilation is allocation-heavy and the
        // dedup hot path runs per-row under retry storms). Pinning the regex shape here surfaces an accidental
        // weakening (e.g., dropping the trailing \b) instead of letting it pass silently.
        Pattern pattern = AiGatewayConstraintMatchers.wordBoundaryPattern("ux_test");

        assertThat(pattern.pattern()).isEqualTo("\\b\\Qux_test\\E\\b");
    }

    @Test
    void testMatchesViaMostSpecificCauseMessage() {
        // Many DuplicateKeyExceptions Spring throws have a generic wrapper message and the actual constraint detail
        // on the most-specific cause. Verify the fallback still finds it.
        DuplicateKeyException exception = new DuplicateKeyException(
            "Spring wrapper: persist failure",
            new RuntimeException("wrapped: violates unique constraint \"" + CONSTRAINT + "\""));

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isTrue();
    }

    /**
     * Pins the reflective {@code PSQLException#getServerErrorMessage().getConstraint()} probe directly. The
     * {@code AiObservabilityOtlpIngestFacadeIntTest}'s end-to-end coverage actually validates only the
     * <em>combined</em> behavior — when its dedup assertion passes, it is impossible to tell whether the reflective
     * structured probe matched or the message-substring fallback did (because the real Postgres error message contains
     * the constraint name verbatim). This test isolates the probe by:
     * <ol>
     * <li>Putting the constraint name <em>only</em> on the structured {@link org.postgresql.util.ServerErrorMessage}
     * stub (via test-only fakes in package {@code org.postgresql.util}), and</li>
     * <li>Crafting wrapper + cause messages that do NOT contain the constraint name.</li>
     * </ol>
     * <p>
     * Therefore a true result is only reachable through the reflective path. A regression that breaks the probe — a
     * driver-rename of {@code getServerErrorMessage} / {@code getConstraint}, an asymmetric package-name change, the
     * cached-Method initialization losing a race in a future refactor — would flip this assertion to false. Without
     * this test the probe could silently degrade to message scanning under retry storms with no CI signal.
     */
    @Test
    void testMatchesViaReflectiveServerErrorMessageProbeWhenMessageDoesNotContainConstraint() {
        org.postgresql.util.ServerErrorMessage serverErrorMessage = new org.postgresql.util.ServerErrorMessage(
            CONSTRAINT);
        org.postgresql.util.PSQLException psqlException = new org.postgresql.util.PSQLException(
            "wrapped exception with no constraint name in the message", serverErrorMessage);

        DuplicateKeyException exception = new DuplicateKeyException("wrapper without constraint name", psqlException);

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .as("Structured PSQLException probe must match by constraint field even when no message contains the name")
            .isTrue();
    }

    @Test
    void testReflectiveProbeReturnsFalseWhenStructuredConstraintIsNull() {
        // Driver returns null when the failure was not a constraint violation. Probe must not throw and must not
        // false-positive on the message text either (constraint name is absent from both messages).
        org.postgresql.util.ServerErrorMessage serverErrorMessage = new org.postgresql.util.ServerErrorMessage(null);
        org.postgresql.util.PSQLException psqlException = new org.postgresql.util.PSQLException(
            "some non-constraint error", serverErrorMessage);

        DuplicateKeyException exception = new DuplicateKeyException("wrapper without constraint name", psqlException);

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isFalse();
    }
}
