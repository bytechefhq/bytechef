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
 * A connection referenced by the source resource being promoted, together with a suggestion for which connection in the
 * target environment it should be rewired to.
 *
 * @param sourceConnectionId          the id of the connection used by the source resource
 * @param sourceConnectionName        the name of the source connection, shown to the user picking a target
 * @param componentName               the component the connection authenticates against
 * @param connectionVersion           the connection definition version
 * @param suggestedTargetConnectionId an automatically matched connection in the target environment, or {@code null}
 *                                    when none was found and the caller must supply one explicitly
 * @param usedBy                      human-readable identifiers (workflow/task names) of the places within the source
 *                                    resource that reference this connection
 *
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
