/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiObservabilityTracingHeaders(
    String traceId,
    String sessionId,
    String spanName,
    String parentSpanId,
    String userId,
    Map<String, String> metadata,
    List<String> tagNames) {

    public AiObservabilityTracingHeaders {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        tagNames = tagNames == null ? List.of() : Collections.unmodifiableList(tagNames);
    }

    public static final String HEADER_METADATA_PREFIX = "X-ByteChef-Metadata-";
    public static final String HEADER_PARENT_SPAN_ID = "X-ByteChef-Parent-Span-Id";
    public static final String HEADER_SESSION_ID = "X-ByteChef-Session-Id";
    public static final String HEADER_SPAN_NAME = "X-ByteChef-Span-Name";
    public static final String HEADER_TAGS = "X-ByteChef-Tags";
    public static final String HEADER_TRACE_ID = "X-ByteChef-Trace-Id";
    public static final String HEADER_USER_ID = "X-ByteChef-User-Id";

    public boolean hasExternalTraceId() {
        return traceId != null;
    }
}
