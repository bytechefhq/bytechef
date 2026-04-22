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
 * Batch of GenAI spans extracted from a single OTLP export request. Wraps the parsed GenAI spans, the
 * {@code droppedSpans} counter (non-GenAI spans skipped during conversion), {@code rejectedSpans} (spans whose
 * construction failed boundary validation in the mapper — wrong-length trace/span ids, malformed timestamps, etc.) and
 * {@code mapperWarnings} (degraded-outcome breadcrumbs for spans that DID persist but with reduced fidelity, e.g.
 * resource-attribute decode failures that strip {@code service.name}). Per-span resource attributes (e.g.
 * {@code service.name}, deployment environment) are carried on each {@link OtelGenAiSpan#resourceAttributes()}, not on
 * the batch.
 *
 * <p>
 * The {@code rejectedSpans} count enforces the partial-success contract end-to-end: a single malformed span in an OTLP
 * export must not abort sibling spans during the protobuf-to-DTO mapping step (the per-span try/catch in the ingest
 * facade only kicks in for spans that survived mapping). Without this count, the facade would silently see a smaller
 * batch and have no way to report "rejected by mapper" to the caller.
 *
 * <p>
 * {@code mapperWarnings} is a list of free-form description strings rather than typed rejection codes — pulling
 * {@code RejectionDetail} into this module would create a dependency cycle (the gateway-api module already depends on
 * otlp-api). The ingest facade wraps each warning string into a typed {@code RejectionDetail} with the appropriate
 * mapper-warning code before surfacing on {@code OtlpIngestResult.warnings}. Warnings are structurally separate from
 * rejected counts because the affected spans WERE persisted — folding them into {@code rejectionReasons} downstream
 * would violate the {@code rejectionReasons.size() <= rejectedSpans} invariant on the result.
 *
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

    /**
     * Backward-compatible 3-arg constructor for callers that do not produce mapper warnings (test fixtures, legacy
     * mappers). New mappers should use the canonical 4-arg constructor.
     */
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
