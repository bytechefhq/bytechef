/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.core.type.TypeReference;

/**
 * Unit test for {@link ContextStoreWorkflowGenerator}. Verifies the structure of the auto-generated workflow JSON: the
 * trigger type/parameters, the data-stream task, the SOURCE/DESTINATION cluster elements, and the metadata
 * ownership-marking. Cadence translation and trigger-mutation are pinned separately because they're load-bearing for
 * the {@code update(cadence)} path.
 *
 * <p>
 * Source absorbed the former Entity layer in Phase 2: the record-shape fields ({@code entityName}, {@code idField},
 * {@code indexedFields}, {@code parameters}) live inline on the source row and the input DTO; the destination workflow
 * JSON carries only {@code sourceId} and {@code mode}.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@ContextConfiguration(classes = JacksonConfiguration.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class ContextStoreWorkflowGeneratorTest {

    @Test
    void testGenerateProducesExpectedShape() {
        ContextStoreSource source = newSource(
            "HubSpot Production", 42L, "hubspot", 1, "@hourly", "searchContacts", "contacts",
            Map.of("portalId", 12345));

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "HubSpot Production", "contacts", "HubSpot contacts", "hubspot", 1, "searchContacts", null, "@hourly",
            null, null, "id", null, Map.of("email", "STRING", "name", "STRING"), null, Map.of("portalId", 12345));

        String definition = ContextStoreWorkflowGenerator.generate(source, input);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        assertThat(parsed.get("label"))
            .isEqualTo("Context Store sync — HubSpot Production");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) parsed.get("metadata");

        assertThat(metadata.get("contextStoreSourceId"))
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

        assertThat(destinationElement.get("name")).isEqualTo("contextStore_1");
        assertThat(destinationElement.get("type")).isEqualTo("contextStore/v1/writeToReplica");

        @SuppressWarnings("unchecked")
        Map<String, Object> destinationParameters = (Map<String, Object>) destinationElement.get("parameters");

        assertThat(destinationParameters.get("sourceId")).isEqualTo(42);
        // Auto-generated workflow emits mode explicitly so it stays correct if the cluster element default ever flips.
        assertThat(destinationParameters.get("mode")).isEqualTo("FULL_REPLACE");
        // Source absorbed the entity layer — the destination cluster element resolves the record shape via sourceId
        // alone; there is no entityName parameter on the workflow definition anymore.
        assertThat(destinationParameters).doesNotContainKey("entityName");
    }

    @Test
    void testGenerateWithCustomCronExpression() {
        ContextStoreSource source = newSource("Custom", 1L, "x", 1, "*/15 * * * *", "reader", "e", null);

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "Custom", "e", null, "x", 1, "reader", null, "*/15 * * * *", null, null, "id", null, Map.of(), null,
            null);

        String definition = ContextStoreWorkflowGenerator.generate(source, input);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        @SuppressWarnings("unchecked")
        Map<String, Object> triggerParameters = (Map<String, Object>) triggers.get(0)
            .get("parameters");

        assertThat(triggerParameters.get("expression")).isEqualTo("*/15 * * * *");
    }

    @Test
    void testGenerateWithManualCadenceUsesNeverFiresCron() {
        ContextStoreSource source = newSource("Manual", 1L, "x", 1, "@manual", "reader", "e", null);

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "Manual", "e", null, "x", 1, "reader", null, "@manual", null, null, "id", null, Map.of(), null, null);

        String definition = ContextStoreWorkflowGenerator.generate(source, input);

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
        ContextStoreSource source = newSource("HubSpot", 1L, "hubspot", 1, "@hourly", "reader", "e", null);

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "HubSpot", "e", null, "hubspot", 1, "reader", null, "@hourly", null, null, "id", null, Map.of(), null,
            null);

        String original = ContextStoreWorkflowGenerator.generate(source, input);

        String updated = ContextStoreWorkflowGenerator.updateCadence(original, "@daily");

        Map<String, Object> originalParsed = JsonUtils.read(original, new TypeReference<>() {});
        Map<String, Object> updatedParsed = JsonUtils.read(updated, new TypeReference<>() {});

        // Tasks unchanged.
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

        assertThatThrownBy(() -> ContextStoreWorkflowGenerator.updateCadence(malformed, "@daily"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGenerateThrowsWhenSourceClusterElementNameUnresolved() {
        ContextStoreSource source = newSource("Unresolved", 1L, "x", 1, "@hourly", null, "e", null);

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "Unresolved", "e", null, "x", 1, null, null, "@hourly", null, null, "id", null, Map.of(), null, null);

        assertThatThrownBy(() -> ContextStoreWorkflowGenerator.generate(source, input))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("sourceClusterElementName");
    }

    @Test
    void testGenerateWithFullReplaceCadenceEmitsPairedTriggers() {
        ContextStoreSource source = newSource(
            "HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts", "e", null);

        source.setFullReplaceCadence("@daily");

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "HubSpot", "e", null, "hubspot", 1, "searchContacts", null, "@hourly", "@daily", null, "id", null,
            Map.of(), null, null);

        String definition = ContextStoreWorkflowGenerator.generate(source, input);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(2);

        // Order: incremental first (so the user-facing "Cadence" picker maps to trigger[0]).
        assertThat(triggers.get(0)
            .get("name")).isEqualTo("scheduledIncrementalSync");

        @SuppressWarnings("unchecked")
        Map<String, Object> incrementalJobParams = (Map<String, Object>) triggers.get(0)
            .get("jobParameters");

        assertThat(incrementalJobParams.get("datastream.mode")).isEqualTo("PARTIAL");

        assertThat(triggers.get(1)
            .get("name")).isEqualTo("scheduledFullSync");

        @SuppressWarnings("unchecked")
        Map<String, Object> fullSyncJobParams = (Map<String, Object>) triggers.get(1)
            .get("jobParameters");

        assertThat(fullSyncJobParams.get("datastream.mode")).isEqualTo("FULL_REPLACE");
    }

    @Test
    void testUpdateFullReplaceCadenceSingleToPairedAddsSecondTrigger() {
        ContextStoreSource source = newSource(
            "HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts", "e", null);

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "HubSpot", "e", null, "hubspot", 1, "searchContacts", null, "@hourly", null, null, "id", null,
            Map.of(), null, null);

        String singleTrigger = ContextStoreWorkflowGenerator.generate(source, input);

        String paired = ContextStoreWorkflowGenerator.updateFullReplaceCadence(singleTrigger, "@hourly", "@daily");

        Map<String, Object> parsed = JsonUtils.read(paired, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(2);
        assertThat(triggers.get(0)
            .get("name")).isEqualTo("scheduledIncrementalSync");
        assertThat(triggers.get(1)
            .get("name")).isEqualTo("scheduledFullSync");
    }

    @Test
    void testUpdateFullReplaceCadencePairedToSingleDropsSecondTrigger() {
        ContextStoreSource source = newSource(
            "HubSpot", 1L, "hubspot", 1, "@hourly", "searchContacts", "e", null);

        source.setFullReplaceCadence("@daily");

        CreateContextStoreSourceInput input = new CreateContextStoreSourceInput(
            1L, "HubSpot", "e", null, "hubspot", 1, "searchContacts", null, "@hourly", "@daily", null, "id", null,
            Map.of(), null, null);

        String paired = ContextStoreWorkflowGenerator.generate(source, input);

        String dropped = ContextStoreWorkflowGenerator.updateFullReplaceCadence(paired, "@hourly", null);

        Map<String, Object> parsed = JsonUtils.read(dropped, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) parsed.get("triggers");

        assertThat(triggers).hasSize(1);
        assertThat(triggers.get(0)
            .get("name")).isEqualTo("scheduledSync");
        // Critical: dropping the paired cadence also strips jobParameters so the runtime stops forcing PARTIAL mode.
        assertThat(triggers.get(0)).doesNotContainKey("jobParameters");
    }

    private static ContextStoreSource newSource(
        String name, long id, String component, int version, String cadence, String clusterElementName,
        String entityName, Map<String, ?> parameters) {

        ContextStoreSource source = new ContextStoreSource();

        source.setId(id);
        source.setContextStoreId(1L);
        source.setName(name);
        source.setEntityName(entityName);
        source.setIdField("id");
        source.setIndexedFields(Map.of());
        source.setParameters(parameters);
        source.setSourceComponentName(component);
        source.setSourceComponentVersion(version);
        source.setSourceClusterElementName(clusterElementName);
        source.setCadence(cadence);
        source.setStatus(ContextStoreSourceStatus.BUILDING_PREVIEW);

        return source;
    }
}
