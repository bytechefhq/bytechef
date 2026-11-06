/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record AiObservabilityTracingHeaders(
    String traceId,
    String sessionId,
    String spanName,
    String parentSpanId,
    String userId,
    Map<String, String> metadata,
    List<String> tagNames,
    AiObservabilityTraceSource source) {

    public static final AiObservabilityTraceSource DEFAULT_SOURCE = AiObservabilityTraceSource.API;

    public AiObservabilityTracingHeaders {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        tagNames = tagNames == null ? List.of() : List.copyOf(tagNames);
        source = source == null ? DEFAULT_SOURCE : source;
    }

    public static final String HEADER_METADATA_PREFIX = "X-ByteChef-Metadata-";
    public static final String HEADER_PARENT_SPAN_ID = "X-ByteChef-Parent-Span-Id";
    public static final String HEADER_SESSION_ID = "X-ByteChef-Session-Id";
    public static final String HEADER_SPAN_NAME = "X-ByteChef-Span-Name";
    public static final String HEADER_TAGS = "X-ByteChef-Tags";
    public static final String HEADER_TRACE_ID = "X-ByteChef-Trace-Id";
    public static final String HEADER_USER_ID = "X-ByteChef-User-Id";

    public static AiObservabilityTracingHeaders empty() {
        return new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of(), null);
    }

    @Deprecated(forRemoval = true)
    public AiObservabilityTracingHeaders(
        String traceId, String sessionId, String spanName, String parentSpanId, String userId,
        Map<String, String> metadata, List<String> tagNames) {

        this(traceId, sessionId, spanName, parentSpanId, userId, metadata, tagNames, null);
    }

    public boolean hasExternalTraceId() {
        return traceId != null;
    }

    public boolean hasSessionId() {
        return sessionId != null;
    }

    public boolean hasSpanName() {
        return spanName != null;
    }

    public boolean hasParentSpanId() {
        return parentSpanId != null;
    }

    public boolean hasUserId() {
        return userId != null;
    }
}
