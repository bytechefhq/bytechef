/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import com.bytechef.ee.platform.ai.observability.facade.RejectionDetail;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Summary of a batch write: counts + a per-failure detail list (empty on success). Each {@link RejectionDetail} carries
 * the inbound batch index so callers can correlate failures with the original request rather than parsing prose — and a
 * structured {@code code} field so dashboards can branch on rejection class without prose-matching.
 *
 * <p>
 * {@code listenerFailedCount} is a separate bucket from {@code rejectedCount}. A listener failure means the row was
 * persisted but the post-commit notification (counter increment, recorded-event publish) failed: the DB has the row,
 * but observability/downstream consumers may not have seen it. Callers deduplicating on {@code acceptedCount} would
 * otherwise resubmit the row (no unique index on {@code ai_eval_score} would catch the second write), so listener
 * failures stay counted toward {@code acceptedCount} — a per-row {@link RejectionDetail} with code
 * {@link RejectionCode#LISTENER_FAILED} is added to {@code rejectionReasons} purely as a diagnostic. Operators alert on
 * {@code listenerFailedCount > 0} for notification-side problems, on {@code rejectedCount > 0} for write failures.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiExternalScoreBatchResult(
    int acceptedCount, int rejectedCount, int listenerFailedCount, List<RejectionDetail> rejectionReasons) {

    public AiExternalScoreBatchResult {
        Validate.isTrue(
            acceptedCount >= 0 && rejectedCount >= 0 && listenerFailedCount >= 0, "counts must be >= 0");
        Validate.notNull(rejectionReasons, "rejectionReasons must not be null");
        rejectionReasons = List.copyOf(rejectionReasons);

        // The rejection-reasons list MUST be 1:1 with (rejectedCount + listenerFailedCount). A mismatch
        // misleads operators about which rows failed and why — the audit signal becomes useless if a
        // regression silently skips a per-row reason. Listener failures contribute one detail row each
        // (the row is in the DB, but the diagnostic still has to surface).
        Validate.isTrue(
            rejectedCount + listenerFailedCount == rejectionReasons.size(),
            "rejectionReasons size (%d) must equal rejectedCount + listenerFailedCount (%d)",
            rejectionReasons.size(), rejectedCount + listenerFailedCount);

        // listenerFailedCount cannot exceed acceptedCount: a listener failure presupposes a successful
        // persistence (the row's already in the DB). Treat any inversion as a programming error, not a
        // user-facing failure mode.
        Validate.isTrue(
            listenerFailedCount <= acceptedCount,
            "listenerFailedCount (%d) must not exceed acceptedCount (%d)",
            listenerFailedCount, acceptedCount);
    }

}
