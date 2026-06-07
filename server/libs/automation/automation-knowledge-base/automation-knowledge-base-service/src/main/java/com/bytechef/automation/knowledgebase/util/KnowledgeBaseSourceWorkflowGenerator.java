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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;

/**
 * Generates the auto-managed Atlas workflow that backs a {@link KnowledgeBaseSource}. The workflow shape is fixed: one
 * or two {@code schedule/v1/cron} triggers drive a single {@code data-stream/v1/stream} task whose {@code SOURCE}
 * cluster element is the configured component reader and whose {@code DESTINATION} cluster element is
 * {@code knowledgeBase/v1/writeAsDocument}.
 *
 * <p>
 * The workflow's metadata carries {@code knowledgeBaseSourceId} for ownership marking — the editor uses this signal to
 * gate edits to auto-generated workflows. The destination's {@code mode} parameter is emitted explicitly as
 * {@code FULL_REPLACE} so a future change to the cluster element's default does not silently flip behavior on
 * pre-existing auto-generated workflows.
 *
 * <p>
 * Cadence values:
 * <ul>
 * <li>{@code "@hourly"}, {@code "@daily"} → translated to standard cron expressions.</li>
 * <li>{@code "@manual"} → the trigger is generated with a sentinel cron that effectively never fires (Feb 31). Manual
 * runs are launched via {@code WorkspaceKnowledgeBaseSourceFacade.refreshNow}.</li>
 * <li>Any other string is passed through as a cron expression.</li>
 * </ul>
 *
 * <p>
 * <b>Phase 17b paired cadence:</b> when {@code source.fullReplaceCadence != null}, the generator emits TWO triggers
 * instead of one — a frequent {@code scheduledIncrementalSync} on the source's {@link KnowledgeBaseSource#getCadence()}
 * and a rare {@code scheduledFullSync} on the source's {@link KnowledgeBaseSource#getFullReplaceCadence()}. Each
 * trigger declares {@code jobParameters.datastream.mode} so the destination writer can route on the trigger that fired.
 * The {@code jobParameters} block is a Phase 17b convention; the scheduler reads it at fire-time and injects the values
 * as Spring Batch JobParameters on the workflow execution. Single-trigger workflows (no {@code fullReplaceCadence})
 * carry no {@code jobParameters} and behave exactly as today.
 *
 * @author Ivica Cardic
 */
public final class KnowledgeBaseSourceWorkflowGenerator {

    private static final String DATA_STREAM_STREAM_TYPE = "dataStream/v1/stream";
    private static final String SCHEDULE_CRON_TRIGGER_TYPE = "schedule/v1/cron";

    private static final String KNOWLEDGE_BASE_COMPONENT_NAME = "knowledgeBase";
    private static final int KNOWLEDGE_BASE_COMPONENT_VERSION = 1;
    private static final String KNOWLEDGE_BASE_WRITE_AS_DOCUMENT = "writeAsDocument";

    private static final String MODE_FULL_REPLACE = "FULL_REPLACE";
    private static final String MODE_PARTIAL = "PARTIAL";

    private static final String UTC_TIMEZONE = "UTC";

    private static final String TRIGGER_NAME_SCHEDULED_SYNC = "scheduledSync";
    private static final String TRIGGER_NAME_SCHEDULED_INCREMENTAL_SYNC = "scheduledIncrementalSync";
    private static final String TRIGGER_NAME_SCHEDULED_FULL_SYNC = "scheduledFullSync";

    /**
     * JobParameter key the scheduler reads off a trigger's {@code jobParameters} block and injects on the workflow
     * execution. The destination writer's mode selector honors this override before falling back to the task's baked
     * {@code mode} parameter.
     */
    private static final String JOB_PARAMETER_DATASTREAM_MODE = "datastream.mode";

    /**
     * Sentinel cron used for {@code @manual} cadence: minute=0, hour=0, day=31, month=2, weekday=0 — Feb has no day 31,
     * so the trigger never fires. Manual runs are launched explicitly via {@code refreshNow}.
     */
    private static final String NEVER_FIRES_CRON = "0 0 31 2 0";

    private static final String METADATA_KNOWLEDGE_BASE_SOURCE_ID = "knowledgeBaseSourceId";

    private KnowledgeBaseSourceWorkflowGenerator() {
    }

    /**
     * Builds the workflow definition JSON for a freshly created {@link KnowledgeBaseSource}. Single trigger today (and
     * when {@code source.fullReplaceCadence} is null); two triggers when paired-cadence is configured.
     */
    public static String generate(KnowledgeBaseSource source) {
        Objects.requireNonNull(source, "source");

        Map<String, Object> definition = new LinkedHashMap<>();

        definition.put("label", "Knowledge Base sync — " + source.getName());
        definition.put(
            "description",
            "Auto-generated by Knowledge Base. Edits via the Knowledge Base Source UI.");
        definition.put("inputs", List.of());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(METADATA_KNOWLEDGE_BASE_SOURCE_ID, source.getId());

        definition.put("metadata", metadata);

        definition.put("triggers", buildTriggers(source));
        definition.put("tasks", List.of(buildSyncTask(source)));

        return JsonUtils.write(definition);
    }

    /**
     * Mutates the workflow definition JSON to update the incremental trigger's cron expression. All other workflow
     * content (jobParameters, the optional second {@code scheduledFullSync} trigger, tasks, metadata) is preserved
     * verbatim — surgical edit on the first trigger's {@code parameters.expression} only.
     */
    public static String updateCadence(String existingDefinition, String newCadence) {
        Objects.requireNonNull(existingDefinition, "existingDefinition");
        Objects.requireNonNull(newCadence, "newCadence");

        Map<String, Object> definition = JsonUtils.read(existingDefinition, new TypeReference<>() {});

        Object triggersObject = definition.get("triggers");

        if (!(triggersObject instanceof List<?> triggersList) || triggersList.isEmpty()) {
            throw new IllegalStateException(
                "Auto-generated Knowledge Base workflow is missing its trigger; cannot update cadence");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> firstTrigger = (Map<String, Object>) triggersList.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) firstTrigger.get("parameters");

        if (parameters == null) {
            throw new IllegalStateException(
                "Auto-generated Knowledge Base workflow's first trigger is missing its parameters block");
        }

        parameters.put("expression", translateCadenceToCron(newCadence));

        return JsonUtils.write(definition);
    }

    /**
     * Mutates the workflow definition JSON to update the {@code scheduledFullSync} trigger's cron expression — or to
     * add / remove that trigger entirely when transitioning between single-trigger and paired-cadence shapes.
     *
     * <p>
     * Phase 17b: called by the facade when {@code source.fullReplaceCadence} changes. Three transitions:
     * <ul>
     * <li>null → non-null: the existing single trigger is renamed to {@code scheduledIncrementalSync} (gains a
     * {@code datastream.mode = PARTIAL} jobParameters block), and a second {@code scheduledFullSync} trigger is
     * appended.</li>
     * <li>non-null → null: drop the {@code scheduledFullSync} trigger; rename the remaining
     * {@code scheduledIncrementalSync} back to {@code scheduledSync} and remove its jobParameters block.</li>
     * <li>non-null → non-null: rewrite only the {@code scheduledFullSync} trigger's cron expression.</li>
     * </ul>
     */
    public static String updateFullReplaceCadence(
        String existingDefinition, String incrementalCadence, @Nullable String newFullReplaceCadence) {

        Objects.requireNonNull(existingDefinition, "existingDefinition");
        Objects.requireNonNull(incrementalCadence, "incrementalCadence");

        Map<String, Object> definition = JsonUtils.read(existingDefinition, new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggersList = (List<Map<String, Object>>) definition.get("triggers");

        if (triggersList == null || triggersList.isEmpty()) {
            throw new IllegalStateException(
                "Auto-generated Knowledge Base workflow is missing its trigger; cannot update fullReplaceCadence");
        }

        Map<String, Object> firstTrigger = triggersList.get(0);
        boolean currentlyPaired = triggersList.size() >= 2;

        List<Map<String, Object>> rebuilt = new ArrayList<>();

        if (newFullReplaceCadence == null) {
            // Drop back to single-trigger shape. Strip jobParameters and rename so the editor / scheduler sees the
            // same node it would see for a freshly-created single-trigger source.
            firstTrigger.put("name", TRIGGER_NAME_SCHEDULED_SYNC);
            firstTrigger.put("label", "Scheduled sync");
            firstTrigger.remove("jobParameters");

            rebuilt.add(firstTrigger);
        } else if (!currentlyPaired) {
            // Transition single → paired. Rename the existing trigger to "scheduledIncrementalSync", add the
            // PARTIAL jobParameter, and append the FULL_REPLACE trigger.
            firstTrigger.put("name", TRIGGER_NAME_SCHEDULED_INCREMENTAL_SYNC);
            firstTrigger.put("label", "Scheduled incremental sync");

            Map<String, Object> jobParameters = new LinkedHashMap<>();
            jobParameters.put(JOB_PARAMETER_DATASTREAM_MODE, MODE_PARTIAL);

            firstTrigger.put("jobParameters", jobParameters);

            rebuilt.add(firstTrigger);
            rebuilt.add(
                buildTrigger(
                    TRIGGER_NAME_SCHEDULED_FULL_SYNC, "Scheduled full sync", newFullReplaceCadence,
                    MODE_FULL_REPLACE));
        } else {
            // Already paired — only rewrite the second trigger's cron. First trigger (incremental) untouched here;
            // changing the incremental cadence routes through updateCadence(...) instead.
            Map<String, Object> secondTrigger = triggersList.get(1);
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) secondTrigger.get("parameters");

            if (parameters != null) {
                parameters.put("expression", translateCadenceToCron(newFullReplaceCadence));
            }

            rebuilt.add(firstTrigger);
            rebuilt.add(secondTrigger);
        }

        definition.put("triggers", rebuilt);

        return JsonUtils.write(definition);
    }

    private static List<Map<String, Object>> buildTriggers(KnowledgeBaseSource source) {
        String fullReplaceCadence = source.getFullReplaceCadence();

        if (fullReplaceCadence == null) {
            // Single-trigger MVP shape. No jobParameters block — the destination writer falls back to the task's
            // baked mode (FULL_REPLACE) on every run, identical to pre-17b behavior.
            return List.of(buildTrigger(TRIGGER_NAME_SCHEDULED_SYNC, "Scheduled sync", source.getCadence(), null));
        }

        // Paired-cadence shape. Each trigger declares jobParameters.datastream.mode so the runtime can route mode by
        // which trigger fired. The frequent trigger runs PARTIAL (incremental + tag-bump fast path) and the rare
        // trigger runs FULL_REPLACE (catches deletions via the tombstone sweep).
        return List.of(
            buildTrigger(
                TRIGGER_NAME_SCHEDULED_INCREMENTAL_SYNC, "Scheduled incremental sync", source.getCadence(),
                MODE_PARTIAL),
            buildTrigger(
                TRIGGER_NAME_SCHEDULED_FULL_SYNC, "Scheduled full sync", fullReplaceCadence, MODE_FULL_REPLACE));
    }

    private static Map<String, Object> buildTrigger(
        String name, String label, String cadence, @Nullable String datastreamMode) {

        Map<String, Object> trigger = new LinkedHashMap<>();

        trigger.put("name", name);
        trigger.put("label", label);
        trigger.put("type", SCHEDULE_CRON_TRIGGER_TYPE);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("expression", translateCadenceToCron(cadence));
        parameters.put("timezone", UTC_TIMEZONE);

        trigger.put("parameters", parameters);

        if (datastreamMode != null) {
            Map<String, Object> jobParameters = new LinkedHashMap<>();
            jobParameters.put(JOB_PARAMETER_DATASTREAM_MODE, datastreamMode);

            trigger.put("jobParameters", jobParameters);
        }

        return trigger;
    }

    private static Map<String, Object> buildSyncTask(KnowledgeBaseSource source) {
        Map<String, Object> task = new LinkedHashMap<>();

        task.put("name", "sync");
        task.put("label", "Sync from source");
        task.put("type", DATA_STREAM_STREAM_TYPE);
        task.put("parameters", new LinkedHashMap<>());

        // Cluster element map keys match ClusterElementType.key() — lowercase "source" / "destination". The runtime
        // resolver (ClusterElementMap.getClusterElement) looks up by key, so uppercase entries are unreachable and
        // surface as "Cluster element type ... not found" at Refresh-Now time.
        Map<String, Object> clusterElements = new LinkedHashMap<>();

        clusterElements.put("source", buildSourceClusterElement(source));
        clusterElements.put("destination", buildDestinationClusterElement(source));

        task.put("clusterElements", clusterElements);

        return task;
    }

    /**
     * Cluster element entries follow the workflow-node convention parsed by {@code ClusterElementMap.toClusterElement}:
     * a required {@code name} (workflow-local node identifier) and a required {@code type} of the form
     * {@code <componentName>/v<version>/<operation>}. The separate {@code componentName} / {@code componentVersion} /
     * {@code clusterElementName} fields the generator used to emit were silently dropped into the {@code extensions}
     * bucket and never reached the resolver — which surfaced as "Skipping malformed scalar cluster element entry ...
     * Unknown value for : name" at sync-run time.
     */
    private static Map<String, Object> buildSourceClusterElement(KnowledgeBaseSource source) {
        String elementName = Objects.requireNonNull(
            source.getSourceClusterElementName(),
            "sourceClusterElementName must be resolved before workflow generation");

        Map<String, Object> sourceElement = new LinkedHashMap<>();

        sourceElement.put("name", source.getSourceComponentName() + "_1");
        sourceElement.put(
            "type",
            "%s/v%d/%s".formatted(
                source.getSourceComponentName(), source.getSourceComponentVersion(), elementName));

        Map<String, ?> persistedParameters = source.getParameters();

        Map<String, Object> sourceParameters = persistedParameters != null
            ? new LinkedHashMap<>(persistedParameters)
            : new LinkedHashMap<>();

        sourceElement.put("parameters", sourceParameters);

        return sourceElement;
    }

    private static Map<String, Object> buildDestinationClusterElement(KnowledgeBaseSource source) {
        Map<String, Object> destination = new LinkedHashMap<>();

        destination.put("name", KNOWLEDGE_BASE_COMPONENT_NAME + "_1");
        destination.put(
            "type",
            "%s/v%d/%s".formatted(
                KNOWLEDGE_BASE_COMPONENT_NAME, KNOWLEDGE_BASE_COMPONENT_VERSION, KNOWLEDGE_BASE_WRITE_AS_DOCUMENT));

        Map<String, Object> destinationParameters = new LinkedHashMap<>();

        destinationParameters.put("sourceId", source.getId());
        destinationParameters.put("mode", MODE_FULL_REPLACE);

        destination.put("parameters", destinationParameters);

        return destination;
    }

    private static String translateCadenceToCron(@Nullable String cadence) {
        if (cadence == null) {
            return NEVER_FIRES_CRON;
        }

        return switch (cadence) {
            case "@hourly" -> "0 * * * *";
            case "@daily" -> "0 0 * * *";
            case "@manual" -> NEVER_FIRES_CRON;
            default -> cadence;
        };
    }
}
