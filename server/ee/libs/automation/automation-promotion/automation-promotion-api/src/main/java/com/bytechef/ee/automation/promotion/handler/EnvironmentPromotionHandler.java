/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;

/**
 * Promotes one {@link PromotionResourceType}. Implementations carry the authorization guards.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EnvironmentPromotionHandler {

    PromotionResourceType getResourceType();

    /**
     * Computes what promoting the resource into the target environment would do, without changing anything.
     *
     * @param sourceId          the id of the resource in its current environment
     * @param targetEnvironment the target environment
     * @return the preview
     */
    EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment);

    /**
     * Creates or updates the counterpart of the resource in the target environment.
     *
     * @param sourceId           the id of the resource in its current environment
     * @param targetEnvironment  the target environment
     * @param connectionMappings source connection id to target connection id
     * @return the outcome of the promotion
     */
    EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings);
}
