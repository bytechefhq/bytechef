/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record EnvironmentPromotionResult(
    long targetId, boolean created, @Nullable String targetUrl, List<Long> unresolvedConnectionIds) {

    public EnvironmentPromotionResult {
        unresolvedConnectionIds = List.copyOf(unresolvedConnectionIds);
    }
}
