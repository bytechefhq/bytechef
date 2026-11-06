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
        assertThat(OtelSpanStatus.values()).containsExactly(
            OtelSpanStatus.UNSET, OtelSpanStatus.OK, OtelSpanStatus.ERROR);
    }
}
