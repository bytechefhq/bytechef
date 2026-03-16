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

package com.bytechef.component.microsoft.todo.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.OUTPUT_TASK_PROPERTY;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.TASK_LIST_ID;
import static com.bytechef.microsoft.commons.MicrosoftConstants.LAST_TIME_CHECKED;
import static com.bytechef.microsoft.commons.MicrosoftConstants.ODATA_NEXT_LINK;
import static com.bytechef.microsoft.commons.MicrosoftConstants.VALUE;
import static com.bytechef.microsoft.commons.MicrosoftUtils.getItemsFromNextPage;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.todo.util.MicrosoftToDoUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftToDoNewTaskTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newTask")
        .title("New Task")
        .description("Triggers when a new task is created in a specified task list.")
        .type(TriggerType.POLLING)
        .help("", "https://docs.bytechef.io/reference/components/microsoft-to-do_v1#new-task")
        .properties(
            string(TASK_LIST_ID)
                .label("Task List ID")
                .description("ID of the task list to monitor for new tasks.")
                .options((OptionsFunction<String>) MicrosoftToDoUtils::getTaskListIdOptions)
                .required(true))
        .output(outputSchema(OUTPUT_TASK_PROPERTY))
        .poll(MicrosoftToDoNewTaskTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftToDoNewTaskTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        List<Map<?, ?>> tasks = new ArrayList<>();

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(zoneId));

        Map<String, Object> body = context
            .http(
                http -> http.get("/me/todo/lists/%s/tasks".formatted(inputParameters.getRequiredString(TASK_LIST_ID))))
            .queryParameters(
                "$filter", "createdDateTime ge " + formattedStartDate,
                "$orderby", "createdDateTime desc")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    tasks.add(map);
                }
            }
        }

        tasks.addAll(getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), context));

        return new PollOutput(tasks, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
