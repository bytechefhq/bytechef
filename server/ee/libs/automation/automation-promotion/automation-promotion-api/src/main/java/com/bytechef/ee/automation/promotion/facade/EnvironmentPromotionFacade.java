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
 * Entry point for promoting an API collection, MCP server or A2A server from one environment to another. Dispatches to
 * the {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} registered for the given
 * {@link PromotionResourceType}.
 *
 * <p>
 * This facade never checks authorization itself; see
 * {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} for where those guards live.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EnvironmentPromotionFacade {

    /**
     * Computes a dry-run preview of promoting {@code sourceId} into the environment identified by
     * {@code targetEnvironmentId}, without making any changes.
     *
     * @param resourceType        the kind of resource being previewed
     * @param sourceId            the id of the resource in its current environment
     * @param targetEnvironmentId the ordinal of the {@link com.bytechef.platform.configuration.domain.Environment} to
     *                            promote into
     * @return the computed preview
     */
    EnvironmentPromotionPreview preview(PromotionResourceType resourceType, long sourceId, long targetEnvironmentId);

    /**
     * Promotes {@code sourceId} into the environment identified by {@code targetEnvironmentId}, creating or updating
     * the target-environment resource.
     *
     * @param resourceType        the kind of resource being promoted
     * @param sourceId            the id of the resource in its current environment
     * @param targetEnvironmentId the ordinal of the {@link com.bytechef.platform.configuration.domain.Environment} to
     *                            promote into
     * @param connectionMappings  source connection id to target connection id, for connections the caller resolved
     *                            explicitly (unmapped connections fall back to the handler's suggested mapping, if any)
     * @return the outcome of the promotion
     */
    EnvironmentPromotionResult promote(
        PromotionResourceType resourceType, long sourceId, long targetEnvironmentId,
        Map<Long, Long> connectionMappings);
}
