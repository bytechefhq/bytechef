/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.knowledgebase.domain;

/**
 * Picks the policy by which the orchestrator detects upstream deletions of synced records (Phase 17b). Incremental sync
 * alone cannot see deletes — the upstream filter returns only records that were touched after the previous run. Each
 * strategy fills the gap differently; the right pick depends on what the source component can deliver and how much
 * staleness the workspace tolerates.
 *
 * <p>
 * Stored as the enum's {@code int} ordinal in the {@code knowledge_base_source.tombstone_strategy} column. Per the
 * project's enum-stability rule: <strong>append new values at the end</strong> — reordering silently relabels every
 * historical row to a different policy.
 * </p>
 *
 * @author Ivica Cardic
 */
public enum TombstoneStrategy {

    /**
     * Default. The orchestrator pairs the incremental cadence with a less-frequent FULL_REPLACE cadence (via
     * {@code KnowledgeBaseSource.fullReplaceCadence}). The full-replace run sees the complete current upstream set, and
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
