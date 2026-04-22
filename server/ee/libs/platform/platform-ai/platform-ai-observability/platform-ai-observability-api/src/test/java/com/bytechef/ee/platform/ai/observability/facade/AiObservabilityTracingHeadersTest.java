/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiObservabilityTracingHeadersTest {

    @Test
    void testSevenArgConstructorDefaultsSourceToApi() {
        // The seven-arg convenience constructor delegates to the eight-arg one with source=null; the component
        // constructor then substitutes the centralized DEFAULT_SOURCE so consumers never see a null source. Without
        // this default, every facade would have to remember a "null means API" convention spread across call sites.
        AiObservabilityTracingHeaders headers = new AiObservabilityTracingHeaders(
            "trace-1", "session-1", "span-1", null, "user-1", Map.of(), List.of());

        assertThat(headers.source()).isEqualTo(AiObservabilityTracingHeaders.DEFAULT_SOURCE);
        assertThat(AiObservabilityTracingHeaders.DEFAULT_SOURCE).isEqualTo(AiObservabilityTraceSource.API);
        assertThat(headers.traceId()).isEqualTo("trace-1");
        assertThat(headers.hasExternalTraceId()).isTrue();
    }

    @Test
    void testEightArgConstructorRetainsSource() {
        AiObservabilityTracingHeaders headers = new AiObservabilityTracingHeaders(
            "trace-1", null, "experiment-replay", null, null, Map.of(), List.of(),
            AiObservabilityTraceSource.EXPERIMENT);

        assertThat(headers.source()).isEqualTo(AiObservabilityTraceSource.EXPERIMENT);
    }

    @Test
    void testNullSourceIsReplacedWithDefault() {
        // Defensive: a caller that explicitly passes null (e.g. via a builder that hasn't set source) still gets the
        // default rather than a null source field — eliminates the "null means API" convention some old call sites
        // relied on.
        AiObservabilityTracingHeaders headers = new AiObservabilityTracingHeaders(
            "trace-1", null, "span-1", null, null, Map.of(), List.of(), null);

        assertThat(headers.source()).isEqualTo(AiObservabilityTracingHeaders.DEFAULT_SOURCE);
    }
}
