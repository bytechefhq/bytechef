/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link OtelSpanStatus} ordinal contract. The Javadoc documents the append-only invariant; this test makes a
 * regression that reorders enum values fail at build time. Without it, a cosmetic refactor (e.g., alphabetising the
 * enum) would silently flip statuses on any in-flight ingestion that reads spans persisted under the previous ordering.
 *
 * @author Ivica Cardic
 * @version ee
 */
class OtelSpanStatusTest {

    @Test
    void testOrdinalContract() {
        assertThat(OtelSpanStatus.UNSET.ordinal()).isEqualTo(0);
        assertThat(OtelSpanStatus.OK.ordinal()).isEqualTo(1);
        assertThat(OtelSpanStatus.ERROR.ordinal()).isEqualTo(2);
    }

    @Test
    void testValuesArrayShape() {
        // Hardens against an accidental insert anywhere other than the end. A reorder that preserves
        // historical ordinals but adds a new value mid-array would still fail this test.
        assertThat(OtelSpanStatus.values()).containsExactly(
            OtelSpanStatus.UNSET, OtelSpanStatus.OK, OtelSpanStatus.ERROR);
    }
}
