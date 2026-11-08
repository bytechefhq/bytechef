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

        Validate.isTrue(
            rejectedCount + listenerFailedCount == rejectionReasons.size(),
            "rejectionReasons size (%d) must equal rejectedCount + listenerFailedCount (%d)",
            rejectionReasons.size(), rejectedCount + listenerFailedCount);

        Validate.isTrue(
            listenerFailedCount <= acceptedCount,
            "listenerFailedCount (%d) must not exceed acceptedCount (%d)",
            listenerFailedCount, acceptedCount);
    }

}
