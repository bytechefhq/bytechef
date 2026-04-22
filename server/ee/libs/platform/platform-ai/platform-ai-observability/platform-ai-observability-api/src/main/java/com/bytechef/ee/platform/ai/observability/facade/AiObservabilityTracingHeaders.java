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
 * Tracing-header bundle extracted from inbound HTTP headers (or constructed programmatically by experiment replay).
 *
 * <p>
 * The five leading {@link String} fields are independently nullable: a missing header MUST be represented as
 * {@code null} (not the empty string). The {@code has*()} accessors give callers a tri-state-free way to ask "is this
 * header present?" without spreading {@code != null} checks across facades.
 *
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

    /**
     * Default trace source assumed when the caller did not set one explicitly. Centralized here so consumers do not
     * have to remember a "null means API" convention spread across facades.
     */
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

    /**
     * Returns an empty headers bundle (every nullable field {@code null}, default source). Use this in preference to
     * {@code new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())} — it makes the "no
     * headers were supplied by the caller" intent explicit at every call site.
     */
    public static AiObservabilityTracingHeaders empty() {
        return new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of(), null);
    }

    /**
     * Convenience constructor for callers that don't override the trace source. The component constructor defaults
     * {@link #source} to {@link #DEFAULT_SOURCE}.
     *
     * @deprecated New call sites must use either {@link #empty()} for the no-headers case or the canonical 8-arg
     *             constructor explicitly. Mixing 7-arg and 8-arg ctors on a record is fragile under reflective
     *             deserialization tooling — every variant of "missing source" must collapse onto one path. Marked
     *             {@code forRemoval} so the IDE / build surfaces the warning on every new call site rather than letting
     *             it accumulate as inert deprecation noise.
     */
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
