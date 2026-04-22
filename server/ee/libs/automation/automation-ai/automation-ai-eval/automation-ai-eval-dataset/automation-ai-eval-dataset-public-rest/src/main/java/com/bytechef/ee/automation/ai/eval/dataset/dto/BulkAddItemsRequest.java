/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.dto;

import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Bulk body for {@code POST /api/ai-gateway/v1/datasets/{id}/items/bulk}. Enforces a 1000-entry cap mirroring the
 * external scores batch cap — keeping request sizes bounded preserves predictable latency and prevents tombstone-sized
 * payloads from pushing the gateway into request-timeout territory.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record BulkAddItemsRequest(List<AddItemRequest> items) {

    public static final int MAX_BATCH_SIZE = 1000;

    public BulkAddItemsRequest {
        Validate.notNull(items, "items must not be null");
        Validate.isTrue(
            items.size() <= MAX_BATCH_SIZE,
            "items size (%d) exceeds cap (%d)", items.size(), MAX_BATCH_SIZE);

        items = List.copyOf(items);
    }
}
