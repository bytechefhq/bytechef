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

package com.bytechef.platform.workflow.coordinator.trigger.jobparameter;

/**
 * Reserved keys that the platform coordinator stores inside an Atlas {@code Job.metadata} map. The {@code Job} entity
 * intentionally exposes only one structured map column ({@code metadata}); to avoid a schema change every time we need
 * to carry one more piece of out-of-band state, the coordinator co-locates entries under double-underscore-prefixed
 * keys that are reserved for the platform and ignored by user code.
 *
 * <p>
 * Phase 17b uses one such key: {@link #JOB_PARAMETERS} — a nested {@code Map<String, ?>} containing Spring Batch
 * JobParameter overrides assembled at trigger fire-time from the firing trigger's static {@code jobParameters} block
 * (commit 4) and the registered {@link TriggerJobParameterContributor} chain (commit 5 Layer 1). The
 * {@code DataStreamStreamActionDefinition} reads this back out at perform-time and applies the entries on the spawned
 * Spring Batch {@code JobLauncher.run(...)} call.
 * </p>
 *
 * <p>
 * Why piggyback on {@code metadata} instead of adding a dedicated {@code job_parameters} column: the {@code Job} entity
 * is the most-loaded table in the system, touched by hundreds of integration paths. Touching its schema for an opt-in
 * feature that only a small fraction of jobs ever carry would force universal compile / test churn for marginal
 * ergonomic gain. The reserved-key convention scopes the change to two callers (the coordinator and the dataStream task
 * action) and leaves everything else untouched.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class JobMetadataKeys {

    /**
     * Map-valued metadata entry carrying the Spring Batch JobParameter overrides for the spawned job. Value is a
     * {@code Map<String, ?>} when present; absent when no trigger {@code jobParameters} block was declared and no
     * contributor recognized the workflow (pre-17b behavior). Readers must treat both "key absent" and "value not a
     * Map" as the empty case and fall back to whatever default the consumer cares about (e.g., the task's baked
     * {@code mode} parameter for the dataStream destination writer).
     *
     * <p>
     * The {@code __} prefix is the platform convention for reserved metadata keys. User-authored workflows cannot
     * collide with this namespace because the workflow DSL rejects keys starting with {@code __} at validation time
     * (Verify this — if not enforced today, the reserved-key contract is documentation-level only and an adversarial
     * user could overwrite it.)
     * </p>
     */
    public static final String JOB_PARAMETERS = "__jobParameters";

    private JobMetadataKeys() {
    }
}
