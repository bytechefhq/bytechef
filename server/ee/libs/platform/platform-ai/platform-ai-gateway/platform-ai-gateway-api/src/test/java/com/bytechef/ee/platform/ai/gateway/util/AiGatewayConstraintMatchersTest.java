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
 * @version ee
 *
 * @author Ivica Cardic
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
        DuplicateKeyException exception = new DuplicateKeyException(
            "wrapper message", new RuntimeException("inner: not postgres"));

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isFalse();
    }

    @Test
    void testWordBoundaryPatternIsStable() {
        Pattern pattern = AiGatewayConstraintMatchers.wordBoundaryPattern("ux_test");

        assertThat(pattern.pattern()).isEqualTo("\\b\\Qux_test\\E\\b");
    }

    @Test
    void testMatchesViaMostSpecificCauseMessage() {
        DuplicateKeyException exception = new DuplicateKeyException(
            "Spring wrapper: persist failure",
            new RuntimeException("wrapped: violates unique constraint \"" + CONSTRAINT + "\""));

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isTrue();
    }

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
        org.postgresql.util.ServerErrorMessage serverErrorMessage = new org.postgresql.util.ServerErrorMessage(null);
        org.postgresql.util.PSQLException psqlException = new org.postgresql.util.PSQLException(
            "some non-constraint error", serverErrorMessage);

        DuplicateKeyException exception = new DuplicateKeyException("wrapper without constraint name", psqlException);

        assertThat(AiGatewayConstraintMatchers.matchesConstraint(exception, CONSTRAINT, PATTERN))
            .isFalse();
    }
}
