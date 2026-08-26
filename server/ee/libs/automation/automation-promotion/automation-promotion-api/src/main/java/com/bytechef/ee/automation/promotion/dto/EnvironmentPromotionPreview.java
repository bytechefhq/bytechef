/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.dto;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A dry-run preview of promoting one resource from {@link #sourceEnvironment()} to {@link #targetEnvironment()},
 * returned by {@link com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade#preview} before the caller
 * commits to {@code promote}.
 *
 * @param resourceType       the kind of resource being previewed
 * @param sourceId           the id of the resource in the source environment
 * @param sourceEnvironment  the environment the resource is being promoted from
 * @param targetEnvironment  the environment the resource is being promoted to
 * @param existingTargetId   the id of the same resource (by lineage uuid) already present in the target environment, or
 *                           {@code null} when promoting would create a new resource
 * @param existingTargetName the name of the existing target resource, or {@code null} when there is none
 * @param projects           the projects the source resource depends on, with their target-environment version status
 * @param connections        the connections the source resource depends on, with suggested target-environment mappings
 * @param warnings           human-readable warnings surfaced to the user before they confirm the promotion
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record EnvironmentPromotionPreview(
    PromotionResourceType resourceType, long sourceId, Environment sourceEnvironment,
    Environment targetEnvironment, @Nullable Long existingTargetId, @Nullable String existingTargetName,
    List<PromotionProjectPreview> projects, List<PromotionConnectionMapping> connections,
    List<String> warnings) {

    public EnvironmentPromotionPreview {
        projects = List.copyOf(projects);
        connections = List.copyOf(connections);
        warnings = List.copyOf(warnings);
    }
}
