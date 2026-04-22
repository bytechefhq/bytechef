/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Request body for {@code POST /api/ai-gateway/v1/datasets}. Creates a new dataset scoped to the caller's workspace.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record CreateDatasetRequest(String name, String description, List<String> tags) {

    public CreateDatasetRequest {
        Validate.isTrue(StringUtils.isNotBlank(name), "name must not be blank");

        if (tags != null) {
            tags = List.copyOf(tags);
        }
    }
}
