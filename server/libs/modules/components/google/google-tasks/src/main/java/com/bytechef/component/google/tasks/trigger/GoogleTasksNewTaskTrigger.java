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
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleTasksNewTaskTrigger {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

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

        List<String> allTasks = closureParameters.getList(ALL_TASKS, String.class, List.of());
        List<String> allTasksUpdate = new ArrayList<>();

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, LocalDateTime.ofInstant(Instant.EPOCH, zoneId));
        String encode = URLEncoder.encode(
            startDate.format(DATE_TIME_FORMATTER.withZone(zoneId)), StandardCharsets.UTF_8);

        List<Map<?, ?>> newTasks = new ArrayList<>();

        Map<String, Object> response = triggerContext
            .http(http -> http.get(
                "https://tasks.googleapis.com/tasks/v1/lists/" + inputParameters.getRequiredString(LIST_ID) + "/tasks"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .queryParameter("updatedMin", encode)
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get("items") instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> task) {
                    String id = (String) task.get("id");

                    allTasksUpdate.add(id);

                    if (!allTasks.contains(id)) {
                        newTasks.add(task);
                    }
                }
            }
        }

        return new PollOutput(newTasks, Map.of(ALL_TASKS, allTasksUpdate, LAST_TIME_CHECKED, now), false);
    }
}
