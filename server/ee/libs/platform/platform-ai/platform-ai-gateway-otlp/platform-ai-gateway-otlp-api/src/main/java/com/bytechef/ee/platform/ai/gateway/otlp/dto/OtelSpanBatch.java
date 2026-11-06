/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record OtelSpanBatch(
    List<OtelGenAiSpan> spans, int droppedSpans, int rejectedSpans, List<String> mapperWarnings) {

    public OtelSpanBatch {
        Validate.notNull(spans, "spans must not be null");
        Validate.isTrue(droppedSpans >= 0, "droppedSpans must be >= 0");
        Validate.isTrue(rejectedSpans >= 0, "rejectedSpans must be >= 0");
        Validate.notNull(mapperWarnings, "mapperWarnings must not be null");
        spans = List.copyOf(spans);
        mapperWarnings = List.copyOf(mapperWarnings);
    }

    public OtelSpanBatch(List<OtelGenAiSpan> spans, int droppedSpans, int rejectedSpans) {
        this(spans, droppedSpans, rejectedSpans, List.of());
    }

    public int size() {
        return spans.size();
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }
}
