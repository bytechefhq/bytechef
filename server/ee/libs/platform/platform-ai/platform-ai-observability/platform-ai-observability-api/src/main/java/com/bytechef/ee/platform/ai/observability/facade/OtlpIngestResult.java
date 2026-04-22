/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Summary of an OTLP ingest call. Per-span rejections do not fail the whole request — this record surfaces the
 * partial-success shape back to the client so they can retry individual spans if desired.
 *
 * <p>
 * Three structurally separate buckets:
 * <ul>
 * <li>{@code acceptedSpans} — newly persisted gen_ai spans (one DB row each).</li>
 * <li>{@code deduplicatedSpans} — spans that were already persisted by an earlier OTLP delivery and matched the partial
 * unique index on {@code (trace_id, external_span_id)}. Surfacing dedup as its own bucket (rather than folding it into
 * "accepted") prevents misleading downstream dashboards into believing the workspace has more spans than the database
 * holds, while still keeping the OTLP exporter out of a retry loop — the wire response treats deduplicated as a
 * non-error outcome that does not require client retry.</li>
 * <li>{@code rejectedSpans} — pre-persist boundary violations (missing gen_ai.system, missing status), persist-time
 * failures (DB constraint other than dedup, transient connection errors), or mapper-rejected inputs (malformed
 * trace/span ids, etc.).</li>
 * </ul>
 *
 * <p>
 * Relationship between {@code rejectedSpans} and {@code rejectionReasons}: the count is exact (one increment per
 * rejected span), but the detail list MAY aggregate batches of identical rejections into a single row. For example, a
 * 10k-span batch where every span fails the mapper produces {@code rejectedSpans=10000} but a single aggregated
 * {@link RejectionDetail#withoutIndex(RejectionCode, String)} entry — emitting 10k identical rows would swamp
 * dashboards and train operators to treat distinct entries as noise. The invariant is therefore
 * {@code rejectionReasons.size() <= rejectedSpans}; clients must not assume 1:1.
 *
 * <p>
 * {@code warnings} is a structurally separate channel from {@code rejectionReasons}: it carries breadcrumbs for spans
 * that DID persist but with reduced fidelity (e.g., resource-attribute decode failed and {@code service.name} is
 * absent). Folding warnings into rejection reasons would either inflate {@code rejectedSpans} (so accepted/rejected no
 * longer reflect persistence outcomes) or violate the {@code rejectionReasons.size() <= rejectedSpans} invariant.
 * Surfacing them as a separate field lets OTLP exporters distinguish "all spans persisted, some with reduced fidelity"
 * from "some spans rejected, retry these" — different operator actions for different outcomes.
 *
 * <p>
 * Conservation invariant: {@code acceptedSpans + deduplicatedSpans + rejectedSpans == batch.spans().size() +
 * batch.rejectedSpans()}. Production callers MUST construct results via
 * {@link #forBatch(OtelSpanBatch, int, int, int, List, List)} which enforces the invariant against the originating
 * batch and is the only way to be sure the response shape reflects what was actually persisted. The bare canonical
 * constructor is retained for Jackson deserialization of REST responses (where no batch is in scope) and
 * {@link #assertConservation(OtelSpanBatch)} can be invoked on a deserialized result to re-verify the equation. A
 * future {@code ingest()} refactor that bypasses {@code forBatch} and calls the canonical constructor directly will
 * fail this audit even though the canonical itself cannot enforce conservation in isolation.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record OtlpIngestResult(
    int acceptedSpans, int deduplicatedSpans, int rejectedSpans, List<RejectionDetail> rejectionReasons,
    List<RejectionDetail> warnings) {

    public OtlpIngestResult {
        Validate.isTrue(acceptedSpans >= 0, "acceptedSpans must be >= 0");
        Validate.isTrue(deduplicatedSpans >= 0, "deduplicatedSpans must be >= 0");
        Validate.isTrue(rejectedSpans >= 0, "rejectedSpans must be >= 0");
        Validate.notNull(rejectionReasons, "rejectionReasons must not be null");
        Validate.notNull(warnings, "warnings must not be null");
        rejectionReasons = List.copyOf(rejectionReasons);
        warnings = List.copyOf(warnings);

        // The detail list may aggregate identical mapper-batch rejections into one row to keep operator
        // dashboards readable on mass-failure batches, so the contract is rejectionReasons.size() <= rejectedSpans.
        // A reasons list LARGER than the count would silently overstate failures — that direction is still rejected.
        // Empty reasons with a positive count is also rejected: the count must be backed by at least one diagnostic
        // breadcrumb so operators have something to investigate.
        Validate.isTrue(
            rejectionReasons.size() <= rejectedSpans,
            "rejectionReasons size (%d) must not exceed rejectedSpans (%d)",
            rejectionReasons.size(), rejectedSpans);
        Validate.isTrue(
            rejectedSpans == 0 || !rejectionReasons.isEmpty(),
            "rejectionReasons must not be empty when rejectedSpans (%d) > 0",
            rejectedSpans);
    }

    /**
     * Backward-compatible 4-arg constructor that defaults {@code warnings} to empty.
     *
     * @deprecated Producers should construct results via {@link #forBatch(OtelSpanBatch, int, int, int, List, List)} so
     *             the conservation invariant is enforced against the originating batch. This bare 4-arg form silently
     *             drops the mapper-warnings channel and cannot verify conservation; a future producer reaching for it
     *             instead of the factory would be a regression.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public OtlpIngestResult(
        int acceptedSpans, int deduplicatedSpans, int rejectedSpans, List<RejectionDetail> rejectionReasons) {

        this(acceptedSpans, deduplicatedSpans, rejectedSpans, rejectionReasons, List.of());
    }

    /**
     * Constructs an ingest result while enforcing the conservation invariant against the originating batch. A future
     * {@code ingest()} refactor that drops a mapper-rejected count or skips a counter increment will fail fast here
     * instead of silently misreporting totals to the OTLP exporter (which would defeat the partial-success contract).
     */
    public static OtlpIngestResult forBatch(
        OtelSpanBatch batch, int acceptedSpans, int deduplicatedSpans, int rejectedSpans,
        List<RejectionDetail> rejectionReasons, List<RejectionDetail> warnings) {

        Validate.notNull(batch, "batch must not be null");

        OtlpIngestResult result = new OtlpIngestResult(
            acceptedSpans, deduplicatedSpans, rejectedSpans, rejectionReasons, warnings);

        result.assertConservation(batch);

        return result;
    }

    /**
     * Backward-compatible factory without warnings. Defaults to an empty warnings list so existing call sites compile
     * unchanged. New producers passing mapper warnings should use the 6-arg overload.
     */
    public static OtlpIngestResult forBatch(
        OtelSpanBatch batch, int acceptedSpans, int deduplicatedSpans, int rejectedSpans,
        List<RejectionDetail> rejectionReasons) {

        return forBatch(batch, acceptedSpans, deduplicatedSpans, rejectedSpans, rejectionReasons, List.of());
    }

    /**
     * Verifies the conservation invariant against the supplied batch. Throws {@link IllegalArgumentException} if the
     * equation does not hold. Java's record canonical constructor is forced to be at least as accessible as the record
     * itself — a fundamentally public back door that {@link #forBatch} alone cannot close. Exposing the conservation
     * check as a publicly-callable method gives test code, integration suites, and any future producer reaching past
     * {@code forBatch} a single canonical place to assert the equation, and the same arithmetic runs in either path.
     */
    public void assertConservation(OtelSpanBatch batch) {
        Validate.notNull(batch, "batch must not be null");

        int batchSpansSize = batch.spans()
            .size();
        int expected = batchSpansSize + batch.rejectedSpans();
        int actual = acceptedSpans + deduplicatedSpans + rejectedSpans;

        Validate.isTrue(
            actual == expected,
            "OtlpIngestResult conservation broken: accepted(%d) + deduplicated(%d) + rejected(%d) = %d, " +
                "expected %d (batch.spans=%d + batch.rejectedSpans=%d)",
            acceptedSpans, deduplicatedSpans, rejectedSpans, actual, expected,
            batchSpansSize, batch.rejectedSpans());
    }
}
