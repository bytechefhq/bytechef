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
public record PromotionConnectionMapping(
    long sourceConnectionId, String sourceConnectionName, String componentName, int connectionVersion,
    @Nullable Long suggestedTargetConnectionId, List<String> usedBy) {

    public PromotionConnectionMapping {
        usedBy = List.copyOf(usedBy);
    }
}
