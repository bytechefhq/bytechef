/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.facade;

import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Promotes an environment-scoped resource from one environment to another.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EnvironmentPromotionFacade {

    EnvironmentPromotionPreview preview(
        String resourceType, long sourceId, long sourceEnvironmentId, long targetEnvironmentId);

    EnvironmentPromotionResult promote(
        String resourceType, long sourceId, long sourceEnvironmentId, long targetEnvironmentId,
        Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash);
}
