/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the contract on {@link AiGatewayThrowables#summarize}: a {@link Throwable} with a null message must collapse to
 * its {@code SimpleName} rather than emitting the literal string {@code "null"} in operator-facing rejection reasons.
 * Without this, the OTLP / external-scores facades concatenating {@code exception.getMessage()} would surface shapes
 * like {@code "trace 123: null"} whenever a Spring {@code DataIntegrityViolationException} carried no message —
 * destroying the audit signal exactly when it mattered most.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiGatewayThrowablesTest {

    @Test
    void testNullMessageRendersAsSimpleNameAlone() {
        // The exact regression. RuntimeException(null) produces getMessage() == null, and a naive
        // toString-style render would emit "RuntimeException: null". The summarize() contract is to drop the
        // dangling colon and surface the class name alone.
        String rendered = AiGatewayThrowables.summarize(new RuntimeException());

        assertThat(rendered).isEqualTo("RuntimeException");
    }

    @Test
    void testBlankMessageRendersAsSimpleNameAlone() {
        // Blank-but-not-null is the same regression in disguise: getMessage() returning "" or " " would
        // surface as "RuntimeException: " under naive concatenation. summarize() treats blank == null.
        assertThat(AiGatewayThrowables.summarize(new RuntimeException("")))
            .isEqualTo("RuntimeException");
        assertThat(AiGatewayThrowables.summarize(new RuntimeException("   ")))
            .isEqualTo("RuntimeException");
    }

    @Test
    void testNonNullMessageRendersAsSimpleNameColonMessage() {
        String rendered = AiGatewayThrowables.summarize(
            new IllegalArgumentException("trace 123 not found"));

        assertThat(rendered).isEqualTo("IllegalArgumentException: trace 123 not found");
    }

    @Test
    void testWrapperWithNullMessageWalksToCauseMessage() {
        // The wrapper itself has no diagnostic content; the cause does. summarize() walks to the deepest
        // non-empty message and tags the root cause class so operators can tell wrapper from cause. Note:
        // {@code new RuntimeException(cause)} (no message arg) silently inherits the cause's toString() as
        // its own message, defeating this test — so we explicitly pass a null message via the
        // {@code (String, Throwable)} constructor to force the walk into the cause chain.
        Throwable cause = new IllegalStateException("connection pool exhausted");
        Throwable wrapper = new RuntimeException(null, cause);

        String rendered = AiGatewayThrowables.summarize(wrapper);

        assertThat(rendered)
            .startsWith("RuntimeException: connection pool exhausted")
            .contains("(root cause: IllegalStateException)");
    }

    /**
     * Named static inner class for {@link #testWalkStopsAtCycleAndFallsBackToSimpleName}. Anonymous inner classes
     * return an empty {@code getSimpleName()} on the JVM, which would defeat the assertion that summarize() falls back
     * to the SimpleName when no message is available — we'd assert isNotBlank() on an empty string and the test would
     * pass for the wrong reason.
     */
    private static class SelfCyclingException extends RuntimeException {

        @Override
        public synchronized Throwable getCause() {
            return this;
        }
    }

    @Test
    void testWalkStopsAtCycleAndFallsBackToSimpleName() {
        // Spring AOP proxies and reflection wrappers occasionally produce A -> B -> A loops. The walk caps
        // at depth 6 and breaks on a self-cycle so summarize() cannot infinite-loop. With no usable message
        // along the chain, the result must collapse to the wrapper class name.
        Throwable selfCycle = new SelfCyclingException();

        String rendered = AiGatewayThrowables.summarize(selfCycle);

        assertThat(rendered)
            .isEqualTo("SelfCyclingException")
            .doesNotContain("null");
    }

    @Test
    void testTruncatesOversizedRenderedString() {
        // A deeply nested Spring DataAccessException can carry messages > 4KB. summarize() caps output at
        // 1024 chars so a runaway cause cannot bloat a persisted row past the column limit and trigger a
        // secondary persist failure.
        String oversize = "x".repeat(2_000);

        String rendered = AiGatewayThrowables.summarize(new RuntimeException(oversize));

        assertThat(rendered).hasSize(1024);
    }

    /**
     * Pins the depth-6 cap. Construct a 7-deep chain where ONLY the deepest exception carries a usable message; every
     * wrapper has {@code null} message + {@code null}-message {@link Throwable} class names that the renderer collapses
     * to the simpleName fallback. The cap MUST stop the walk before reaching the message at depth 7, collapsing to the
     * head's class name. A regression that loosens the cap to, e.g., 100 would expose the deep message and pass
     * {@link #testWalkStopsAtCycleAndFallsBackToSimpleName} (which only covers self-cycles).
     */
    @Test
    void testWalkRespectsDepthCapEvenWithoutCycle() {
        Throwable depthSeven = new RuntimeException(
            null, new RuntimeException(
                null, new RuntimeException(
                    null, new RuntimeException(
                        null, new RuntimeException(
                            null, new RuntimeException(
                                null, new RuntimeException("DEEP_MESSAGE_AT_DEPTH_SEVEN"))))))) {

            @Override
            public String toString() {
                return "WrapperException";
            }
        };

        String rendered = AiGatewayThrowables.summarize(depthSeven);

        assertThat(rendered)
            .as("Depth-6 cap must stop walk before reaching depth-7 message")
            .doesNotContain("DEEP_MESSAGE_AT_DEPTH_SEVEN");
    }
}
