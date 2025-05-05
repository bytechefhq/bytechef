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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.util.GoogleTasksUtils.getTasks;

import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.google.tasks.util.GoogleTasksUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
                .description("ID of the list where tasks are stored.")
                .options((OptionsDataSource.TriggerOptionsFunction<String>) GoogleTasksUtils::getListsIdOptions)
                .required(true))
        .output()
        .poll(GoogleTasksNewTaskTrigger::poll);

    private GoogleTasksNewTaskTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        ZoneId zoneId = ZoneId.of("GMT");

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, now.minusHours(3));

        List<Map<?, ?>> customTasks = getTasks(
            triggerContext, inputParameters.getRequiredString(LIST_ID),
            startDate.format(DATE_TIME_FORMATTER.withZone(zoneId)));

        return new PollOutput(customTasks, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
