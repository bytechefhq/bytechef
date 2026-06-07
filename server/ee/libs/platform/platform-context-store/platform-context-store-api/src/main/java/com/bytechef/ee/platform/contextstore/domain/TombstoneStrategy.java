/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

/**
 * Picks the policy by which the orchestrator detects upstream deletions of synced records (Phase 17b). Incremental sync
 * alone cannot see deletes — the upstream filter returns only records that were touched after the previous run. Each
 * strategy fills the gap differently; the right pick depends on what the source component can deliver and how much
 * staleness the workspace tolerates.
 *
 * <p>
 * Stored as the enum's {@code int} ordinal in the {@code context_store_source.tombstone_strategy} column. Per the
 * project's enum-stability rule: <strong>append new values at the end</strong> — reordering silently relabels every
 * historical row to a different policy.
 * </p>
 *
 * <p>
 * Parallels {@code com.bytechef.platform.knowledgebase.domain.TombstoneStrategy} by design — KB and CS use the same
 * conceptual strategies but the enums live in separate modules so neither subsystem depends on the other for a
 * persistence-level type.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum TombstoneStrategy {

    /**
     * Default. The orchestrator pairs the incremental cadence with a less-frequent FULL_REPLACE cadence (via
     * {@code ContextStoreSource.fullReplaceCadence}). The full-replace run sees the complete current upstream set, and
     * the existing tombstone sweep marks anything missing as deleted. Catches deletes within one full-cadence cycle.
     * Cheap on the orchestrator side; only the source component pays the cost during the full run.
     */
    PERIODIC_FULL_REPLACE,

    /**
     * The source component itself reads a deletion stream from the upstream system (HubSpot CRM events, Salesforce CDC,
     * etc.) and emits records carrying a {@code _deleted = true} marker. The DESTINATION cluster element tombstones at
     * write time. Requires component-level support — readers that don't advertise change-feed capability should not
     * select this strategy. Forward-compatible enum entry; this phase reserves the slot.
     */
    UPSTREAM_CHANGE_FEED,

    /**
     * Explicit opt-out. Records are append-only — never tombstoned. Suitable for write-once event streams or audit-log
     * sources where upstream deletions either don't exist or shouldn't propagate to the replica.
     */
    NONE
}
