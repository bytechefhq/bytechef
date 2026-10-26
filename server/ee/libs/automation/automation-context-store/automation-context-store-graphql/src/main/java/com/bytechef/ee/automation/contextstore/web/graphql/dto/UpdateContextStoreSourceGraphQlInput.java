/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql.dto;

import com.bytechef.ee.automation.contextstore.dto.UpdateContextStoreSourceInput;
import com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record UpdateContextStoreSourceGraphQlInput(
    @Nullable String name, @Nullable String cadence, @Nullable Boolean enabled,
    @Nullable String fullReplaceCadence, @Nullable TombstoneStrategy tombstoneStrategy) {

    public UpdateContextStoreSourceInput toFacadeInput() {
        return new UpdateContextStoreSourceInput(name, cadence, enabled, fullReplaceCadence, tombstoneStrategy);
    }
}
