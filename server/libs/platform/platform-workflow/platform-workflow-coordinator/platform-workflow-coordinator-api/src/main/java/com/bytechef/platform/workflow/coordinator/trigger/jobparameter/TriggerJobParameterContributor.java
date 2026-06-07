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

import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.Map;

/**
 * SPI for contributing additional Spring Batch {@code JobParameter} overrides at trigger fire-time. Phase 17b
 * introduces this hook so each subsystem (Context Store, Knowledge Base, future agent surfaces) can attach derived
 * values — {@code datastream.since} from the source's last sync timestamp, for example — without the platform
 * coordinator needing to know about subsystem-specific row shapes.
 *
 * <p>
 * Contract: implementations receive the workflow's static {@code metadata} block and the firing
 * {@link WorkflowExecutionId}. They MUST return an empty map when they do not recognize the workflow (the coordinator
 * unions every contributor's response across the registered list), and MUST NOT throw — a missing source row, a
 * transient lookup failure, or a recognised-but-not-applicable metadata shape all collapse to {@code Map.of()} so a
 * single misbehaving contributor cannot block trigger dispatch. Long-running work belongs elsewhere; contributors run
 * synchronously on the trigger completion path.
 * </p>
 *
 * <p>
 * Coordinator-side merge rule: the trigger's own {@code jobParameters} block (declared statically in the workflow JSON
 * by Phase 17b commit 4) is applied first; each contributor's response is then layered on top, with later contributors
 * winning on key collisions. Each contributor in practice writes a disjoint key set keyed by its subsystem prefix
 * ({@code datastream.*}, {@code agent.*}, etc.), so collisions only happen when implementations accidentally overlap.
 * Pick well-namespaced keys; the merge order is implementation-detail.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface TriggerJobParameterContributor {

    /**
     * Returns additional {@code JobParameter} overrides for the job spawned by the firing trigger. The
     * {@code workflowMetadataMap} argument carries the workflow's static metadata block — what the workflow generator
     * stored under {@code metadata} (e.g., {@code contextStoreSourceId}, {@code knowledgeBaseSourceId}). The
     * {@code workflowExecutionId} is the firing trigger's principal binding; subsystems that key off principal type or
     * workspace can use it directly instead of round-tripping through metadata.
     *
     * <p>
     * Return {@code Map.of()} on any "I do not recognise this workflow" outcome — first-run with no
     * {@code lastSyncRunAt}, metadata missing the discriminator key, source row deleted between trigger fire and job
     * creation, etc. The platform-level dispatch is more important than any one subsystem's contribution.
     * </p>
     */
    Map<String, ?> contribute(Map<String, ?> workflowMetadataMap, WorkflowExecutionId workflowExecutionId);
}
