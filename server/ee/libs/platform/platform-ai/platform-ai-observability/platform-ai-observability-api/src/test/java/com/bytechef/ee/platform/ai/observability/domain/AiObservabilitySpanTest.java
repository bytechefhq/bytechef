/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Pins the post-close immutability invariant on {@link AiObservabilitySpan}. Once {@code close()} (or a direct
 * {@code setStatus(...)} terminal write) has stamped a terminal status, every other setter — including the time/status
 * finalization triad ({@code setEndTime}, {@code setStartTime}, {@code setStatus}) — must throw
 * {@link IllegalStateException}. A regression that strips {@code assertActive} from one of those setters would silently
 * allow an OTLP retry to overwrite a finalized observation row, which the partial-success ingest contract specifically
 * forbids.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiObservabilitySpanTest {

    @Test
    void testFreshSpanIsActive() {
        AiObservabilitySpan span = new AiObservabilitySpan(1L, AiObservabilitySpanType.GENERATION);

        assertThat(span.getStatus()).isEqualTo(AiObservabilitySpanStatus.ACTIVE);
    }

    @Test
    void testCloseStampsEndTimeLatencyAndStatus() {
        AiObservabilitySpan span = new AiObservabilitySpan(1L, AiObservabilitySpanType.GENERATION);

        Instant end = span.getStartTime()
            .plusMillis(250L);

        span.close(end, AiObservabilitySpanStatus.COMPLETED);

        assertThat(span.getStatus()).isEqualTo(AiObservabilitySpanStatus.COMPLETED);
        assertThat(span.getEndTime()).isEqualTo(end);
        assertThat(span.getLatencyMs()).isEqualTo(250);
    }

    @Test
    void testCloseRejectsActiveTerminalStatus() {
        AiObservabilitySpan span = new AiObservabilitySpan(1L, AiObservabilitySpanType.GENERATION);

        assertThatThrownBy(() -> span.close(Instant.now(), AiObservabilitySpanStatus.ACTIVE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("terminal status");
    }

    @Test
    void testCloseRejectsRecloseFromTerminal() {
        AiObservabilitySpan span = new AiObservabilitySpan(1L, AiObservabilitySpanType.GENERATION);

        span.close(Instant.now(), AiObservabilitySpanStatus.COMPLETED);

        assertThatThrownBy(() -> span.close(Instant.now(), AiObservabilitySpanStatus.ERROR))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already in terminal state");
    }

    @Test
    void testSetCostThrowsAfterClose() {
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setCost(new BigDecimal("0.01")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cost");
    }

    @Test
    void testSetInputTokensThrowsAfterClose() {
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setInputTokens(100))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("inputTokens");
    }

    @Test
    void testSetOutputTokensThrowsAfterClose() {
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setOutputTokens(42))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("outputTokens");
    }

    @Test
    void testSetLatencyMsThrowsAfterClose() {
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setLatencyMs(123))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("latencyMs");
    }

    @Test
    void testSetNameThrowsAfterClose() {
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setName("retry-name"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("name");
    }

    @Test
    void testSetModelThrowsAfterClose() {
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setModel("gpt-4o"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("model");
    }

    @Test
    void testCloseRejectsRecloseAfterClose() {
        // setEndTime is now private — the equivalent regression to guard against is a double-close. close()
        // checks for already-terminal state explicitly and throws so a buggy retry loop cannot silently
        // overwrite the finalized latency / status invariant.
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.close(Instant.now(), AiObservabilitySpanStatus.COMPLETED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already in terminal state");
    }

    @Test
    void testSetStartTimeThrowsAfterClose() {
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setStartTime(Instant.now()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("startTime");
    }

    @Test
    void testSetStatusThrowsAfterClose() {
        // setStatus is a permitted close-state-machine entry point while ACTIVE, but locked out after.
        AiObservabilitySpan span = closedSpan();

        assertThatThrownBy(() -> span.setStatus(AiObservabilitySpanStatus.ERROR))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("status");
    }

    @Test
    void testSetStatusAllowsTerminalTransitionWhileActive() {
        // Pins that the assertActive guard does NOT block the legitimate ACTIVE → terminal transition via the
        // direct setStatus path. close() is the preferred entry point, but setStatus(terminal) remains a valid
        // public mutator on an ACTIVE span (e.g. for callers stamping a terminal status without an endTime).
        AiObservabilitySpan span = new AiObservabilitySpan(1L, AiObservabilitySpanType.GENERATION);

        span.setStatus(AiObservabilitySpanStatus.COMPLETED);

        assertThat(span.getStatus()).isEqualTo(AiObservabilitySpanStatus.COMPLETED);
    }

    @Test
    void testCloseRejectsEndTimeBeforeStartTime() {
        // Routed through close() since setEndTime is now private — the underlying invariant (endTime must not be
        // before startTime) still surfaces because close() delegates to the private setter.
        AiObservabilitySpan span = new AiObservabilitySpan(1L, AiObservabilitySpanType.GENERATION);

        Instant start = span.getStartTime();

        assertThatThrownBy(() -> span.close(start.minusSeconds(1L), AiObservabilitySpanStatus.COMPLETED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endTime");
    }

    private static AiObservabilitySpan closedSpan() {
        AiObservabilitySpan span = new AiObservabilitySpan(1L, AiObservabilitySpanType.GENERATION);

        span.close(span.getStartTime()
            .plusMillis(100L), AiObservabilitySpanStatus.COMPLETED);

        return span;
    }
}
