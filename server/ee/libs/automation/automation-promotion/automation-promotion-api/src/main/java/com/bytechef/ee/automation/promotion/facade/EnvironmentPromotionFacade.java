/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.facade;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import java.util.Map;

/**
 * Promotes an environment-scoped resource from one environment to another.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EnvironmentPromotionFacade {

    /**
     * Computes what promoting the resource into the target environment would do, without changing anything.
     *
     * @param resourceType        the kind of resource
     * @param sourceId            the id of the resource in its current environment
     * @param targetEnvironmentId the ordinal of the target environment
     * @return the preview
     */
    EnvironmentPromotionPreview preview(PromotionResourceType resourceType, long sourceId, long targetEnvironmentId);

    /**
     * Creates or updates the counterpart of the resource in the target environment.
     *
     * @param resourceType        the kind of resource
     * @param sourceId            the id of the resource in its current environment
     * @param targetEnvironmentId the ordinal of the target environment
     * @param connectionMappings  source connection id to target connection id
     * @return the outcome of the promotion
     */
    EnvironmentPromotionResult promote(
        PromotionResourceType resourceType, long sourceId, long targetEnvironmentId,
        Map<Long, Long> connectionMappings);
}
