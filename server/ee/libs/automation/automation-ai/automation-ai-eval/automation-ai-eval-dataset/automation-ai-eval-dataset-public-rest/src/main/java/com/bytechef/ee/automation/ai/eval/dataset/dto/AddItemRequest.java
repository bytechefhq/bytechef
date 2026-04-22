/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Request body for {@code POST /api/ai-gateway/v1/datasets/{id}/items}. {@code input} is typed as {@link Object} so
 * callers may submit either a JSON object (e.g. a chat-messages array) or a plain string without pre-encoding it.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AddItemRequest(
    Object input,
    Object expectedOutput,
    Map<String, Object> metadata) {

    public AddItemRequest {
        Validate.notNull(input, "input must not be null");

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }
}
