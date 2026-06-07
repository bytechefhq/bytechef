/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

/**
 * Lifecycle state of a Context Store source. Maps to Airbyte's progressive-availability model.
 *
 * <p>
 * Ordinals are pinned by {@code EnumOrdinalStabilityTest} — append new values at the end.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum ContextStoreSourceStatus {
    BUILDING_PREVIEW,
    PREVIEW,
    READY,
    FAILED,
    DISABLED
}
