/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import org.apache.commons.lang3.StringUtils;

/**
 * Structured per-row failure detail for partial-success batch responses.
 *
 * <p>
 * Used by {@link OtlpIngestResult} and {@link AiExternalScoreBatchResult} so the per-row reason carries a stable
 * machine-parseable {@code code} alongside the human-readable {@code reason}, with a non-null contract enforced at
 * construction. A structured shape closes the class of bugs where a raw {@code String} reason concatenated a
 * potentially-null exception message into the wire response.
 *
 * <p>
 * Fields:
 * <ul>
 * <li>{@code index} — caller-supplied position in the inbound batch (0-based) when the row's identity is known. May be
 * {@code null} for boundary-rejected rows where no row identity could be reconstructed (e.g., spans rejected by the
 * OTLP mapper before a {@code spanId} could be safely decoded).</li>
 * <li>{@code code} — typed machine-parseable token from {@link RejectionCode}. Wire-stable via {@link Enum#name()}; the
 * enum keeps the token list compile-time-pinned so dashboards / clients can branch without parsing prose, and typos
 * that would otherwise slip into dashboards as silent miscategorization fail at compile time.</li>
 * <li>{@code reason} — human-readable summary including any exception class name. Built via
 * {@link com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables#summarize} when the source is an exception so
 * null messages collapse to the class name rather than the literal string {@code "null"}.</li>
 * </ul>
 *
 * @author Ivica Cardic
 * @version ee
 */
public record RejectionDetail(Integer index, RejectionCode code, String reason) {

    /**
     * Maximum reason length on the wire. Identical to
     * {@link com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables#DEFAULT_MAX_RENDERED_LENGTH} so a reason
     * built from a chain of nested exceptions is bounded the same way regardless of which producer constructed it. An
     * un-summarised hand-built reason that exceeds this cap is truncated at construction with a {@code …} suffix; the
     * full text remains in the producer's server-side log line.
     */
    public static final int MAX_REASON_LENGTH = 1024;

    private static final String TRUNCATION_SUFFIX = "…";

    public RejectionDetail {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }

        if (StringUtils.isBlank(reason)) {
            throw new IllegalArgumentException("reason must not be blank");
        }

        if (index != null && index < 0) {
            throw new IllegalArgumentException("index must be >= 0 when present; got " + index);
        }

        // An un-summarised producer (a hand-built rejection reason that did not route through
        // AiGatewayThrowables.summarize) can carry an arbitrarily long string. Truncating here matches the
        // summarize cap so the wire payload size is bounded regardless of construction path; the full text
        // stays in the producer's server-side log line where it belongs.
        if (reason.length() > MAX_REASON_LENGTH) {
            reason = reason.substring(0, MAX_REASON_LENGTH - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
        }
    }

    /** Convenience factory for failures where the row has no recoverable batch index (e.g. mapper-rejected). */
    public static RejectionDetail withoutIndex(RejectionCode code, String reason) {
        return new RejectionDetail(null, code, reason);
    }

    /** Convenience factory for per-row failures where the index is known. */
    public static RejectionDetail at(int index, RejectionCode code, String reason) {
        return new RejectionDetail(index, code, reason);
    }

    /**
     * Convenience factory that validates the index against a known batch size in addition to the lower bound. Use this
     * when the caller has the inbound batch size in hand — passing {@code Integer.MAX_VALUE} or a stale index from a
     * sibling batch would otherwise pass the bare {@link #at(int, RejectionCode, String)} check and confuse downstream
     * dashboards that index back into the original request body.
     */
    public static RejectionDetail at(int index, int batchSize, RejectionCode code, String reason) {
        if (batchSize < 0) {
            throw new IllegalArgumentException("batchSize must be >= 0; got " + batchSize);
        }

        if (index >= batchSize) {
            throw new IllegalArgumentException(
                "index (" + index + ") must be < batchSize (" + batchSize + ")");
        }

        return new RejectionDetail(index, code, reason);
    }
}
