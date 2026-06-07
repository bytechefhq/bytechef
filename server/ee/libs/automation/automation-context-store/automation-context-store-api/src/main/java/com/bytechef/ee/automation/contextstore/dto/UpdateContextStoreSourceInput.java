/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.dto;

import com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy;
import org.jspecify.annotations.Nullable;

/**
 * Input DTO for updating a {@link com.bytechef.ee.platform.contextstore.domain.ContextStoreSource}. All fields are
 * optional; null means "leave unchanged". Consumed by
 * {@link com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade#update(Long, Long, UpdateContextStoreSourceInput)}.
 *
 * <p>
 * A {@code cadence} change triggers a targeted mutation of the workflow's trigger cron parameter; the rest of the
 * workflow definition is left untouched. An {@code enabled} change toggles the corresponding {@code
 * ProjectDeploymentWorkflow} (which controls trigger activation in the scheduler).
 *
 * <p>
 * {@code fullReplaceCadence} and {@code tombstoneStrategy} were added in Phase 17b. The cadence change here mirrors how
 * {@code cadence} updates work — a non-null value mutates the workflow's full-replace trigger expression in place; a
 * null leaves it unchanged. To clear a previously-set full cadence (drop back to single-trigger), pass an empty string;
 * the facade interprets {@code ""} as "remove" rather than "leave alone".
 *
 * @author Ivica Cardic
 * @version ee
 */
public record UpdateContextStoreSourceInput(
    @Nullable String name,
    @Nullable String cadence,
    @Nullable Boolean enabled,
    @Nullable String fullReplaceCadence,
    @Nullable TombstoneStrategy tombstoneStrategy) {
}
