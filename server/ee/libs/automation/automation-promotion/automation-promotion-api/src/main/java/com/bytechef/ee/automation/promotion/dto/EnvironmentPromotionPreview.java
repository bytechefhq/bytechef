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
