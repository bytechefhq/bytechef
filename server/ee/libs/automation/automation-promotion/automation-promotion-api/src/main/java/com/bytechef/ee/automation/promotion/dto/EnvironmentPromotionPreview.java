/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.dto;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record EnvironmentPromotionPreview(
    String resourceType, long sourceId, Environment sourceEnvironment, Environment targetEnvironment,
    @Nullable Long existingTargetId, @Nullable String existingTargetName, List<PromotionProjectPreview> projects,
    List<PromotionConnectionMapping> connections, List<String> warnings, List<PromotionSchemaChange> schemaChanges,
    List<PromotionChange> changes, String destructiveChangesHash) {

    public EnvironmentPromotionPreview {
        projects = List.copyOf(projects);
        connections = List.copyOf(connections);
        warnings = List.copyOf(warnings);
        schemaChanges = List.copyOf(schemaChanges);
        changes = List.copyOf(changes);
    }
}
