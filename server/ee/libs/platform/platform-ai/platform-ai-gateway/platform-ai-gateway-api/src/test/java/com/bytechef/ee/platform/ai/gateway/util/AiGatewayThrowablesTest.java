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
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGatewayThrowablesTest {

    @Test
    void testNullMessageRendersAsSimpleNameAlone() {
        String rendered = AiGatewayThrowables.summarize(new RuntimeException());

        assertThat(rendered).isEqualTo("RuntimeException");
    }

    @Test
    void testBlankMessageRendersAsSimpleNameAlone() {
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
        Throwable cause = new IllegalStateException("connection pool exhausted");
        Throwable wrapper = new RuntimeException(null, cause);

        String rendered = AiGatewayThrowables.summarize(wrapper);

        assertThat(rendered)
            .startsWith("RuntimeException: connection pool exhausted")
            .contains("(root cause: IllegalStateException)");
    }

    private static class SelfCyclingException extends RuntimeException {

        @Override
        public synchronized Throwable getCause() {
            return this;
        }
    }

    @Test
    void testWalkStopsAtCycleAndFallsBackToSimpleName() {
        Throwable selfCycle = new SelfCyclingException();

        String rendered = AiGatewayThrowables.summarize(selfCycle);

        assertThat(rendered)
            .isEqualTo("SelfCyclingException")
            .doesNotContain("null");
    }

    @Test
    void testTruncatesOversizedRenderedString() {
        String oversize = "x".repeat(2_000);

        String rendered = AiGatewayThrowables.summarize(new RuntimeException(oversize));

        assertThat(rendered).hasSize(1024);
    }

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
