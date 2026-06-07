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

package com.bytechef.component.datastream.action.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Phase 17b unit tests for {@link DataStreamStreamActionDefinition}'s metadata-piggyback consumption path. The wider
 * {@code perform()} flow needs the full Atlas + Spring Batch stack to drive end-to-end (covered by the disabled
 * {@link com.bytechef.ee.platform.contextstore.ContextStoreSyncE2EIntTest} and KB equivalent), so this class exercises
 * the new {@code extractJobParameterOverrides} helper directly.
 *
 * <p>
 * The helper is package-private and static. Reflection lookup keeps the production code's access modifier minimal — the
 * helper isn't part of the perform-time API and shouldn't be promoted to public just to be testable.
 * </p>
 *
 * @author Ivica Cardic
 */
class DataStreamStreamActionDefinitionTest {

    private static final String JOB_PARAMETERS_KEY = "__jobParameters";

    @Test
    void testExtractJobParameterOverridesReturnsEmptyWhenMetadataEmpty() throws Exception {
        // Pre-17b path: parent Job carries no metadata at all. The action proceeds with no overrides — Spring
        // Batch JobParameters carry only the existing DESTINATION / SOURCE / TENANT_ID entries the action has
        // always built.
        Map<String, ?> result = invokeExtract(Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void testExtractJobParameterOverridesReturnsEmptyWhenReservedKeyMissing() throws Exception {
        // Workflow metadata exists (e.g. contextStoreSourceId for the contributor to recognize) but the handler
        // didn't produce any __jobParameters entries (no trigger jobParameters block AND no contributor returned
        // anything). The action treats this identically to "no metadata at all" — same MVP behavior.
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("contextStoreSourceId", 42L);

        Map<String, ?> result = invokeExtract(metadata);

        assertThat(result).isEmpty();
    }

    @Test
    void testExtractJobParameterOverridesReturnsReservedMapWhenPresent() throws Exception {
        // Happy path: the TriggerCompletionHandler stashed both a datastream.mode (from the static trigger
        // jobParameters block, commit 4) and a datastream.since (from the CS/KB contributor, Layer 1) under the
        // reserved key. Both flow through to the action which then routes them: mode -> DESTINATION cluster
        // element parameter override; since -> top-level Spring Batch JobParameter for the reader-side delegate.
        Map<String, Object> overrides = Map.of(
            "datastream.mode", "PARTIAL",
            "datastream.since", 1700000000000L);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(JOB_PARAMETERS_KEY, overrides);

        Map<String, ?> result = invokeExtract(metadata);

        assertThat(result).hasSize(2);
        assertThat(result.get("datastream.mode")).isEqualTo("PARTIAL");
        assertThat(result.get("datastream.since")).isEqualTo(1700000000000L);
    }

    @Test
    void testExtractJobParameterOverridesReturnsEmptyWhenReservedKeyValueIsNotMap() throws Exception {
        // Defensive: a future ETL bug or hand-crafted metadata could store a non-Map value under the reserved
        // key. The action treats this as "no override" rather than throwing — one bad metadata write must not
        // break the trigger dispatch path. Same recovery as the reserved-key-missing case.
        Map<String, Object> metadata = new HashMap<>();

        metadata.put(JOB_PARAMETERS_KEY, "not a map");

        Map<String, ?> result = invokeExtract(metadata);

        assertThat(result).isEmpty();
    }

    /**
     * Reflectively invokes the package-private static {@code extractJobParameterOverrides} helper. Keeping the helper
     * package-private avoids leaking it into the action's public API — it's an implementation detail of
     * {@code perform()}.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, ?> invokeExtract(Map<String, Object> jobMetadata) throws Exception {
        Method method = DataStreamStreamActionDefinition.class.getDeclaredMethod(
            "extractJobParameterOverrides", Map.class);

        method.setAccessible(true);

        return (Map<String, ?>) method.invoke(null, jobMetadata);
    }
}
