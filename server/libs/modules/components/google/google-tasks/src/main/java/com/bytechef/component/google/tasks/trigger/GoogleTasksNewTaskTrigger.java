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

package com.bytechef.component.google.tasks.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.ALL_TASKS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.MAX_RESULTS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.PAGE_TOKEN;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TASK_OUTPUT_PROPERTY;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.tasks.util.GoogleTasksUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class GoogleTasksNewTaskTrigger {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newTask")
        .title("New Task")
        .description("Triggers when a new task is added.")
        .type(TriggerType.POLLING)
        .properties(
            string(LIST_ID)
                .label("List ID")
                .description("ID of the list where new task is added.")
                .options((OptionsFunction<String>) GoogleTasksUtils::getListsIdOptions)
                .required(true))
        .output(outputSchema(TASK_OUTPUT_PROPERTY))
        .poll(GoogleTasksNewTaskTrigger::poll)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleTasksNewTaskTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        List<String> previousTasksIds = closureParameters.getList(ALL_TASKS, String.class, List.of());
        List<String> tasksIds = new ArrayList<>();
        List<Map<?, ?>> tasks = new ArrayList<>();
        Instant now = Instant.now();

        boolean editorEnvironment = triggerContext.isEditorEnvironment();
        Instant start = closureParameters.get(
            LAST_TIME_CHECKED, Instant.class, editorEnvironment ? now.minus(Duration.ofHours(3)) : now);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(start);
        int maxResults = editorEnvironment ? 1 : 100;
        String listId = inputParameters.getRequiredString(LIST_ID);
        String nextToken = null;
        boolean initialLoad = previousTasksIds.isEmpty() && !editorEnvironment;

        do {
            Map<String, Object> response = fetchTasksPage(
                triggerContext, listId, nextToken, maxResults, initialLoad ? null : timestamp);

            if (response.get("items") instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> task) {
                        String id = (String) task.get(ID);

                        if (!tasksIds.contains(id)) {
                            tasksIds.add(id);
                        }

                        if (!initialLoad && !previousTasksIds.contains(id)) {
                            tasks.add(task);
                        }
                    }
                }
            }

            if (editorEnvironment) {
                break;
            }

            nextToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);
        } while (nextToken != null);

        return new PollOutput(
            initialLoad ? List.of() : tasks, Map.of(ALL_TASKS, tasksIds, LAST_TIME_CHECKED, now), false);
    }

    private static Map<String, Object> fetchTasksPage(
        TriggerContext triggerContext, String listId, String pageToken, int maxResults, String updatedMin) {

        return triggerContext
            .http(http -> http.get("https://tasks.googleapis.com/tasks/v1/lists/%s/tasks".formatted(listId)))
            .queryParameters(PAGE_TOKEN, pageToken, MAX_RESULTS, maxResults, "updatedMin", updatedMin)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
