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

package com.bytechef.automation.knowledgebase.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.core.type.TypeReference;

/**
 * Unit test for {@link KnowledgeBaseSourceWorkflowGenerator}. Verifies the structure of the auto-generated workflow
 * JSON: trigger type/parameters, the data-stream task, the SOURCE/DESTINATION cluster elements (including the explicit
 * {@code mode: "FULL_REPLACE"} parameter), and the metadata ownership-marking. Cadence translation and trigger-mutation
 * are pinned separately because they're load-bearing for the {@code update(cadence)} path.
 *
 * <p>
 * Auto-pick of the source cluster element name lives in
 * {@link com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacadeImpl} (which has Spring DI
 * access to the component definition service); the generator itself is a pure JSON builder and requires the resolved
 * name on the source entity it receives.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = JacksonConfiguration.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class KnowledgeBaseSourceWorkflowGeneratorTest {

    @Test
    void testGenerateProducesExpectedShape() {
        KnowledgeBaseSource source = newSource("HubSpot Production", 42L, "hubspot", 1, "@hourly", "searchContacts");

        String definition = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        assertThat(parsed.get("label"))
            .isEqualTo("Knowledge Base sync — HubSpot Production");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) parsed.get("metadata");

        assertThat(metadata.get("knowledgeBaseSourceId"))
            .isEqualTo(42);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(1);

        Map<String, Object> trigger = triggers.get(0);

        assertThat(trigger.get("type")).isEqualTo("schedule/v1/cron");
        assertThat(trigger.get("name")).isEqualTo("scheduledSync");

        @SuppressWarnings("unchecked")
        Map<String, Object> triggerParameters = (Map<String, Object>) trigger.get("parameters");

        assertThat(triggerParameters.get("expression")).isEqualTo("0 * * * *");
        assertThat(triggerParameters.get("timezone")).isEqualTo("UTC");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) parsed.get("tasks");

        assertThat(tasks).hasSize(1);

        Map<String, Object> task = tasks.get(0);

        assertThat(task.get("name")).isEqualTo("sync");
        assertThat(task.get("type")).isEqualTo("dataStream/v1/stream");

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) task.get("clusterElements");

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceElement = (Map<String, Object>) clusterElements.get("source");

        assertThat(sourceElement.get("name")).isEqualTo("hubspot_1");
        assertThat(sourceElement.get("type")).isEqualTo("hubspot/v1/searchContacts");

        @SuppressWarnings("unchecked")
        Map<String, Object> destinationElement = (Map<String, Object>) clusterElements.get("destination");

        assertThat(destinationElement.get("name")).isEqualTo("knowledgeBase_1");
        assertThat(destinationElement.get("type")).isEqualTo("knowledgeBase/v1/writeAsDocument");

        @SuppressWarnings("unchecked")
        Map<String, Object> destinationParameters = (Map<String, Object>) destinationElement.get("parameters");

        assertThat(destinationParameters.get("sourceId")).isEqualTo(42);
        assertThat(destinationParameters.get("mode")).isEqualTo("FULL_REPLACE");
    }

    @Test
    void testGenerateWithManualCadenceUsesNeverFiresCron() {
        KnowledgeBaseSource source = newSource("Manual", 1L, "x", 1, "@manual", "reader");

        String definition = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        @SuppressWarnings("unchecked")
        Map<String, Object> triggerParameters = (Map<String, Object>) triggers.get(0)
            .get("parameters");

        // Sentinel cron: Feb 31 — never fires.
        assertThat(triggerParameters.get("expression")).isEqualTo("0 0 31 2 0");
    }

    @Test
    void testUpdateCadenceMutatesOnlyTriggerCron() {
        KnowledgeBaseSource source = newSource("HubSpot", 1L, "hubspot", 1, "@hourly", "reader");

        String original = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        String updated = KnowledgeBaseSourceWorkflowGenerator.updateCadence(original, "@daily");

        Map<String, Object> originalParsed = JsonUtils.read(original, new TypeReference<>() {});
        Map<String, Object> updatedParsed = JsonUtils.read(updated, new TypeReference<>() {});

        // Tasks unchanged — including the explicit mode parameter on the destination.
        assertThat(updatedParsed.get("tasks")).isEqualTo(originalParsed.get("tasks"));

        // Trigger expression changed.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedTriggers = (List<Map<String, Object>>) updatedParsed.get("triggers");

        @SuppressWarnings("unchecked")
        Map<String, Object> triggerParameters = (Map<String, Object>) updatedTriggers.get(0)
            .get("parameters");

        assertThat(triggerParameters.get("expression")).isEqualTo("0 0 * * *");
    }

    @Test
    void testUpdateCadenceFailsWithoutTrigger() {
        String malformed = JsonUtils.write(Map.of("label", "x", "tasks", List.of()));

        assertThatThrownBy(() -> KnowledgeBaseSourceWorkflowGenerator.updateCadence(malformed, "@daily"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGenerateThrowsWhenSourceClusterElementNameUnresolved() {
        KnowledgeBaseSource source = newSource("Unresolved", 1L, "x", 1, "@hourly", null);

        assertThatThrownBy(() -> KnowledgeBaseSourceWorkflowGenerator.generate(source))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("sourceClusterElementName");
    }

    @Test
    void testGenerateThreadsSourceParametersIntoSourceClusterElement() {
        KnowledgeBaseSource source = newSource("Airtable Prod", 7L, "airtable", 1, "@hourly", "selectRecords");

        Map<String, Object> sourceParameters = new java.util.LinkedHashMap<>();

        sourceParameters.put("baseId", "appAbc123");
        sourceParameters.put("tableId", "tblXyz789");

        source.setParameters(sourceParameters);

        String definition = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) parsed.get("tasks");

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) tasks.get(0)
            .get("clusterElements");

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceElement = (Map<String, Object>) clusterElements.get("source");

        @SuppressWarnings("unchecked")
        Map<String, Object> capturedParameters = (Map<String, Object>) sourceElement.get("parameters");

        assertThat(capturedParameters)
            .containsEntry("baseId", "appAbc123")
            .containsEntry("tableId", "tblXyz789");
    }

    @Test
    void testGenerateUsesEmptySourceParametersWhenSourceHasNoParameters() {
        KnowledgeBaseSource source = newSource("NoParams", 8L, "csvFile", 1, "@hourly", "readCsv");

        String definition = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) parsed.get("tasks");

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) tasks.get(0)
            .get("clusterElements");

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceElement = (Map<String, Object>) clusterElements.get("source");

        @SuppressWarnings("unchecked")
        Map<String, Object> capturedParameters = (Map<String, Object>) sourceElement.get("parameters");

        assertThat(capturedParameters).isEmpty();
    }

    @Test
    void testGenerateWithFullReplaceCadenceEmitsPairedTriggers() {
        KnowledgeBaseSource source = newSource("HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts");

        source.setFullReplaceCadence("@daily");

        String definition = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(2);

        // Order matters: scheduler reads triggers in document order. The frequent incremental trigger comes first
        // so the generator's updateCadence (which edits trigger[0]) maps to the user-facing "Sync cadence" picker.
        Map<String, Object> incremental = triggers.get(0);

        assertThat(incremental.get("name")).isEqualTo("scheduledIncrementalSync");
        assertThat(incremental.get("label")).isEqualTo("Scheduled incremental sync");

        @SuppressWarnings("unchecked")
        Map<String, Object> incrementalParams = (Map<String, Object>) incremental.get("parameters");

        assertThat(incrementalParams.get("expression")).isEqualTo("0 * * * *");

        @SuppressWarnings("unchecked")
        Map<String, Object> incrementalJobParams = (Map<String, Object>) incremental.get("jobParameters");

        // The PARTIAL mode is what the destination writer's mode selector consumes (Phase 17b commit 5 wires the
        // scheduler-side injection). Single-trigger workflows have no jobParameters block at all.
        assertThat(incrementalJobParams.get("datastream.mode")).isEqualTo("PARTIAL");

        Map<String, Object> fullSync = triggers.get(1);

        assertThat(fullSync.get("name")).isEqualTo("scheduledFullSync");

        @SuppressWarnings("unchecked")
        Map<String, Object> fullSyncParams = (Map<String, Object>) fullSync.get("parameters");

        assertThat(fullSyncParams.get("expression")).isEqualTo("0 0 * * *");

        @SuppressWarnings("unchecked")
        Map<String, Object> fullSyncJobParams = (Map<String, Object>) fullSync.get("jobParameters");

        assertThat(fullSyncJobParams.get("datastream.mode")).isEqualTo("FULL_REPLACE");
    }

    @Test
    void testGenerateWithoutFullReplaceCadenceOmitsJobParametersBlock() {
        // Single-trigger MVP workflow: no jobParameters anywhere — the destination writer falls back to the task's
        // baked mode (FULL_REPLACE). Pre-17b workflows in the wild must keep behaving exactly as today.
        KnowledgeBaseSource source = newSource("HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts");

        String definition = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(1);
        assertThat(triggers.get(0)
            .get("name")).isEqualTo("scheduledSync");
        assertThat(triggers.get(0)).doesNotContainKey("jobParameters");
    }

    @Test
    void testUpdateFullReplaceCadenceSingleToPairedAddsSecondTriggerAndRenamesFirst() {
        KnowledgeBaseSource source = newSource("HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts");

        String singleTrigger = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        String paired = KnowledgeBaseSourceWorkflowGenerator.updateFullReplaceCadence(
            singleTrigger, "@hourly", "@daily");

        Map<String, Object> parsed = JsonUtils.read(paired, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(2);
        assertThat(triggers.get(0)
            .get("name")).isEqualTo("scheduledIncrementalSync");
        assertThat(triggers.get(1)
            .get("name")).isEqualTo("scheduledFullSync");

        // The first trigger gained a PARTIAL jobParameters block during the transition.
        @SuppressWarnings("unchecked")
        Map<String, Object> incrementalJobParams = (Map<String, Object>) triggers.get(0)
            .get("jobParameters");

        assertThat(incrementalJobParams.get("datastream.mode")).isEqualTo("PARTIAL");
    }

    @Test
    void testUpdateFullReplaceCadencePairedToSingleDropsSecondTriggerAndRenamesFirstBack() {
        KnowledgeBaseSource source = newSource("HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts");

        source.setFullReplaceCadence("@daily");

        String paired = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        String dropped = KnowledgeBaseSourceWorkflowGenerator.updateFullReplaceCadence(paired, "@hourly", null);

        Map<String, Object> parsed = JsonUtils.read(dropped, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(1);
        assertThat(triggers.get(0)
            .get("name")).isEqualTo("scheduledSync");
        // Critical: when transitioning back to single-trigger, the jobParameters block is stripped so the runtime
        // doesn't keep forcing PARTIAL mode without a paired FULL_REPLACE trigger to catch deletions.
        assertThat(triggers.get(0)).doesNotContainKey("jobParameters");
    }

    @Test
    void testUpdateFullReplaceCadencePairedToPairedOnlyRewritesSecondCron() {
        KnowledgeBaseSource source = newSource("HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts");

        source.setFullReplaceCadence("@daily");

        String original = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        String updated = KnowledgeBaseSourceWorkflowGenerator.updateFullReplaceCadence(
            original, "@hourly", "0 0 * * 0"); // weekly Sundays

        Map<String, Object> originalParsed = JsonUtils.read(original, new TypeReference<>() {});
        Map<String, Object> updatedParsed = JsonUtils.read(updated, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedTriggers = (List<Map<String, Object>>) updatedParsed.get("triggers");

        // First trigger unchanged.
        assertThat(updatedTriggers.get(0))
            .isEqualTo(((List<?>) originalParsed.get("triggers")).get(0));

        // Second trigger's cron is the weekly expression; everything else preserved.
        @SuppressWarnings("unchecked")
        Map<String, Object> secondTriggerParams = (Map<String, Object>) updatedTriggers.get(1)
            .get("parameters");

        assertThat(secondTriggerParams.get("expression")).isEqualTo("0 0 * * 0");
    }

    private static KnowledgeBaseSource newSource(
        String name, long id, String component, int version, String cadence, String clusterElementName) {

        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setId(id);
        source.setName(name);
        source.setSourceComponentName(component);
        source.setSourceComponentVersion(version);
        source.setSourceClusterElementName(clusterElementName);
        source.setKnowledgeBaseId(99L);
        source.setCadence(cadence);
        source.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);

        return source;
    }
}
