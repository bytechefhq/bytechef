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

package com.bytechef.automation.datasync.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = JacksonConfiguration.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class DataSyncWorkflowGeneratorTest {

    private static final UUID DATA_SYNC_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void testManualMatchesSnapshot() {
        String definition =
            DataSyncWorkflowGenerator.generate(newDataSync(TriggerType.MANUAL, Map.of()), fullElements());

        JsonFileAssert.assertEquals("definition/data_sync_manual.json", parse(definition));
    }

    @Test
    void testScheduleMatchesSnapshot() {
        DataSync dataSync = newDataSync(
            TriggerType.SCHEDULE,
            Map.of("frequencyKind", "DAILY", "timeOfDay", "09:00", "expression", "0 9 * * ?", "timezone",
                "Europe/Zagreb"));

        JsonFileAssert.assertEquals(
            "definition/data_sync_schedule.json", parse(DataSyncWorkflowGenerator.generate(dataSync, fullElements())));
    }

    @Test
    void testMissingProcessorEmitsNoProcessorSlot() {
        List<DataSyncElement> elements = List.of(source(), destination());

        Map<String, Object> parsed =
            parse(DataSyncWorkflowGenerator.generate(newDataSync(TriggerType.MANUAL, Map.of()), elements));

        JsonFileAssert.assertEquals("definition/data_sync_no_processor.json", parsed);
    }

    @Test
    void testMissingDestinationEmitsNoDestinationSlot() {
        Map<String, Object> parsed = parse(
            DataSyncWorkflowGenerator.generate(newDataSync(TriggerType.MANUAL, Map.of()), List.of(source())));

        JsonFileAssert.assertEquals("definition/data_sync_no_destination.json", parsed);
    }

    @Test
    void testScheduleTriggerReadsExpressionAndTimezone() {
        DataSync dataSync = newDataSync(TriggerType.SCHEDULE, Map.of("expression", "0/5 * * * ?", "timezone", "UTC"));

        Map<String, Object> parsed = parse(DataSyncWorkflowGenerator.generate(dataSync, fullElements()));

        @SuppressWarnings("unchecked")
        Map<String, Object> trigger = ((List<Map<String, Object>>) parsed.get("triggers")).get(0);

        assertThat(trigger).containsEntry("name", "trigger_1")
            .containsEntry("type", "schedule/v1/cron");

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) trigger.get("parameters");

        assertThat(parameters).containsEntry("expression", "0/5 * * * ?")
            .containsEntry("timezone", "UTC");
    }

    @Test
    void testGenerateIsDeterministic() {
        DataSync dataSync = newDataSync(TriggerType.MANUAL, Map.of());

        assertThat(DataSyncWorkflowGenerator.generate(dataSync, fullElements()))
            .isEqualTo(DataSyncWorkflowGenerator.generate(dataSync, fullElements()));
    }

    /**
     * String equality between two calls in one JVM (the check above) has no power against ordering regressions: Java's
     * {@code String.hashCode()} is not randomised, so a {@code HashMap} would iterate identically both times within the
     * same run, and the snapshot tests use JSONAssert, which is order-independent. This test instead feeds
     * {@code elements} in a SCRAMBLED order and pins the emitted {@code clusterElements} key order directly — proving
     * the generator's own {@code Kind.values()} traversal, not the caller's list order, decides the order. Ordering is
     * load-bearing: {@code toDataSyncDTO}'s {@code unpublishedChanges} compares two generated definition strings.
     */
    @Test
    void testClusterElementKeyOrderIsSourceDestinationProcessorRegardlessOfElementOrder() {
        DataSync dataSync = newDataSync(TriggerType.MANUAL, Map.of());
        List<DataSyncElement> scrambledElements = List.of(processor(), destination(), source());

        Map<String, Object> parsed = parse(DataSyncWorkflowGenerator.generate(dataSync, scrambledElements));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) parsed.get("tasks");

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) tasks.get(0)
            .get("clusterElements");

        assertThat(clusterElements.keySet()).containsExactly("source", "destination", "processor");
    }

    private static Map<String, Object> parse(String definition) {
        return JsonUtils.read(definition, new TypeReference<>() {});
    }

    private static DataSync newDataSync(TriggerType triggerType, Map<String, ?> triggerParameters) {
        DataSync dataSync = new DataSync();

        dataSync.setId(42L);
        dataSync.setName("crm-to-db");
        dataSync.setTitle("CRM to DB");
        dataSync.setUuid(DATA_SYNC_UUID);
        dataSync.setTriggerType(triggerType);
        dataSync.setTriggerParameters(triggerParameters);

        return dataSync;
    }

    private static List<DataSyncElement> fullElements() {
        return List.of(source(), destination(), processor());
    }

    private static DataSyncElement source() {
        DataSyncElement element = new DataSyncElement(42L, Kind.SOURCE);

        element.setId(1L);
        element.setComponentName("csvFile");
        element.setComponentVersion(1);
        element.setOperationName("read");
        element.setParameters(Map.of("fileEntry", "${trigger_1.file}"));
        element.setConnectionId(7L);

        return element;
    }

    private static DataSyncElement destination() {
        DataSyncElement element = new DataSyncElement(42L, Kind.DESTINATION);

        element.setId(2L);
        element.setComponentName("postgresql");
        element.setComponentVersion(1);
        element.setOperationName("insert");
        element.setParameters(Map.of("table", "contacts", "mode", "FULL_REPLACE"));
        element.setConnectionId(8L);

        return element;
    }

    private static DataSyncElement processor() {
        DataSyncElement element = new DataSyncElement(42L, Kind.PROCESSOR);

        element.setId(3L);
        element.setComponentName(DataSyncElement.PROCESSOR_COMPONENT_NAME);
        element.setComponentVersion(DataSyncElement.PROCESSOR_COMPONENT_VERSION);
        element.setOperationName(DataSyncElement.PROCESSOR_OPERATION_NAME);
        element.setParameters(
            Map.of("mappings", List.of(Map.of("sourceField", "email", "destinationField", "email_address"))));

        return element;
    }
}
