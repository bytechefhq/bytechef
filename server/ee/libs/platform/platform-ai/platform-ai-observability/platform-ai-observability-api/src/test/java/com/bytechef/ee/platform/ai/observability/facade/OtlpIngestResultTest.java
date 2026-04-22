/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
@SuppressWarnings("deprecation")
class OtlpIngestResultTest {

    @Test
    void testCanonicalConstructorAllowsAggregatedRejections() {
        // Aggregation case: ten rejected spans collapsed into one diagnostic row. Mass mapper-rejection batches
        // would otherwise emit identical RejectionDetail rows and swamp dashboards.
        OtlpIngestResult result = new OtlpIngestResult(0, 0, 10, List.of(
            RejectionDetail.withoutIndex(RejectionCode.REJECTED_BY_MAPPER, "10 span(s) rejected by mapper")));

        assertThat(result.rejectedSpans()).isEqualTo(10);
        assertThat(result.rejectionReasons()).hasSize(1);
    }

    @Test
    void testCanonicalConstructorRejectsRejectionListLargerThanCount() {
        // Reverse mismatch: more reasons than rejected spans would silently overstate failures.
        assertThatThrownBy(() -> new OtlpIngestResult(0, 0, 1, List.of(
            RejectionDetail.at(0, RejectionCode.REJECTED_PERSIST_FAILED, "row 0"),
            RejectionDetail.at(1, RejectionCode.REJECTED_PERSIST_FAILED, "row 1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed rejectedSpans");
    }

    @Test
    void testCanonicalConstructorRejectsEmptyReasonsWithPositiveCount() {
        // A positive rejected count with no diagnostic breadcrumb leaves operators with nothing to investigate.
        assertThatThrownBy(() -> new OtlpIngestResult(0, 0, 3, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be empty when rejectedSpans");
    }

    @Test
    void testForBatchEnforcesConservationInvariantWhenSumIsLow() {
        // Five spans in the batch but only 4 outcomes reported — a future ingest() refactor that drops a counter
        // increment would silently misreport totals to the OTLP exporter and defeat the partial-success contract.
        OtelSpanBatch batch = new OtelSpanBatch(List.of(), 0, 5);

        assertThatThrownBy(() -> OtlpIngestResult.forBatch(
            batch, 1, 1, 2, List.of(
                RejectionDetail.at(0, RejectionCode.REJECTED_BY_MAPPER, "1"),
                RejectionDetail.at(1, RejectionCode.REJECTED_BY_MAPPER, "2"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conservation broken")
                    .hasMessageContaining("expected 5");
    }

    @Test
    void testForBatchEnforcesConservationInvariantWhenSumIsHigh() {
        OtelSpanBatch batch = new OtelSpanBatch(List.of(), 0, 2);

        assertThatThrownBy(() -> OtlpIngestResult.forBatch(batch, 3, 0, 0, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("conservation broken");
    }

    @Test
    void testForBatchAcceptsBalancedTotals() {
        OtelSpanBatch batch = new OtelSpanBatch(List.of(), 0, 3);

        OtlpIngestResult result = OtlpIngestResult.forBatch(
            batch, 0, 0, 3, List.of(
                RejectionDetail.at(0, RejectionCode.REJECTED_BY_MAPPER, "1"),
                RejectionDetail.at(1, RejectionCode.REJECTED_BY_MAPPER, "2"),
                RejectionDetail.at(2, RejectionCode.REJECTED_BY_MAPPER, "3")));

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.deduplicatedSpans()).isZero();
        assertThat(result.rejectedSpans()).isEqualTo(3);
    }

    @Test
    void testForBatchRejectsNullBatch() {
        assertThatThrownBy(() -> OtlpIngestResult.forBatch(null, 0, 0, 0, List.of()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("batch must not be null");
    }
}
