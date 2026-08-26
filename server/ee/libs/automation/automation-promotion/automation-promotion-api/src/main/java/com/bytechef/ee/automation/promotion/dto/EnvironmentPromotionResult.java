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
 * The outcome of promoting one resource to a target environment, returned by
 * {@link com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade#promote}.
 *
 * @param targetId                the id of the resource in the target environment, whether newly created or already
 *                                existing
 * @param created                 {@code true} when a new resource was created in the target environment, {@code false}
 *                                when an existing one (matched by lineage uuid) was updated in place
 * @param targetUrl               a resource-specific URL to the promoted resource in the target environment, or
 *                                {@code null} when the resource type has no user-facing URL
 * @param unresolvedConnectionIds the ids of source connections that could not be mapped to a target-environment
 *                                connection, either because no mapping was supplied or the supplied mapping was invalid
 *
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
