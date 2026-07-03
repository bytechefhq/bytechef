/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

/**
 * Lifecycle status of a AI Hub task.
 *
 * <p>
 * <strong>Ordinal stability is load-bearing.</strong> {@link AiHubTask#getStatus()} persists this enum as an
 * {@code int} ordinal in the {@code ai_hub_task.status} column (see the Liquibase migration that declares the column
 * type as {@code INT}). Reordering existing values would silently re-interpret every previously persisted row — an
 * existing {@code ACTIVE=0} would suddenly be read back as whatever new value lands at index 0. Append new values at
 * the end of the declaration list only; never reorder, rename, or remove existing values without a corresponding data
 * migration.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubTaskStatus {

    ACTIVE,
    ARCHIVED,
    DELETED
    // append-only — add new values BELOW this line and update EnumOrdinalStabilityTest.
}
