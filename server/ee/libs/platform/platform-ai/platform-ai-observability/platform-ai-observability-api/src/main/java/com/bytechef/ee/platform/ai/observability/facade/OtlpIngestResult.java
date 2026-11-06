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

        Validate.isTrue(
            rejectionReasons.size() <= rejectedSpans,
            "rejectionReasons size (%d) must not exceed rejectedSpans (%d)",
            rejectionReasons.size(), rejectedSpans);
        Validate.isTrue(
            rejectedSpans == 0 || !rejectionReasons.isEmpty(),
            "rejectionReasons must not be empty when rejectedSpans (%d) > 0",
            rejectedSpans);
    }

    @Deprecated(since = "1.0", forRemoval = false)
    public OtlpIngestResult(
        int acceptedSpans, int deduplicatedSpans, int rejectedSpans, List<RejectionDetail> rejectionReasons) {

        this(acceptedSpans, deduplicatedSpans, rejectedSpans, rejectionReasons, List.of());
    }

    public static OtlpIngestResult forBatch(
        OtelSpanBatch batch, int acceptedSpans, int deduplicatedSpans, int rejectedSpans,
        List<RejectionDetail> rejectionReasons, List<RejectionDetail> warnings) {

        Validate.notNull(batch, "batch must not be null");

        OtlpIngestResult result = new OtlpIngestResult(
            acceptedSpans, deduplicatedSpans, rejectedSpans, rejectionReasons, warnings);

        result.assertConservation(batch);

        return result;
    }

    public static OtlpIngestResult forBatch(
        OtelSpanBatch batch, int acceptedSpans, int deduplicatedSpans, int rejectedSpans,
        List<RejectionDetail> rejectionReasons) {

        return forBatch(batch, acceptedSpans, deduplicatedSpans, rejectedSpans, rejectionReasons, List.of());
    }

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
