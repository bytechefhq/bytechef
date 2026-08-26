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
 * SPI implemented once per {@link PromotionResourceType} (API collection, MCP server, A2A server), and dispatched to by
 * {@link com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade}.
 *
 * <p>
 * {@code @PreAuthorize} guards live on the implementing beans, not on this interface or on
 * {@link com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade} — the facade never checks
 * authorization itself. An implementation's guard resolves the owning project/workspace id for {@code sourceId} through
 * a separate {@code promotionAuthorizer} bean (a peer collaborator injected into the implementation, not a
 * self-reference), following the precedent set by {@code WorkspaceConnectionFacadeImpl}'s
 * {@code @PreAuthorize("@permissionService.isResourceOwner(...)")}, which also delegates to a separate bean rather than
 * to itself.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EnvironmentPromotionHandler {

    /**
     * @return the resource type this handler promotes
     */
    PromotionResourceType getResourceType();

    /**
     * Computes a dry-run preview of promoting {@code sourceId} into {@code targetEnvironment}, without making any
     * changes.
     *
     * @param sourceId          the id of the resource in its current environment
     * @param targetEnvironment the environment to promote into
     * @return the computed preview
     */
    EnvironmentPromotionPreview preview(long sourceId, Environment targetEnvironment);

    /**
     * Promotes {@code sourceId} into {@code targetEnvironment}, creating or updating the target-environment resource.
     *
     * @param sourceId           the id of the resource in its current environment
     * @param targetEnvironment  the environment to promote into
     * @param connectionMappings source connection id to target connection id, for connections the caller resolved
     *                           explicitly
     * @return the outcome of the promotion
     */
    EnvironmentPromotionResult promote(
        long sourceId, Environment targetEnvironment, Map<Long, Long> connectionMappings);
}
