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

package com.bytechef.component.notion.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.util.NotionUtils.getAllItems;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.notion.util.NotionUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionNewDatabaseItemTrigger {

    public static final String LAST_TIME_CHECKED = "lastTimeChecked";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newDatabaseItem")
        .title("New Database Item")
        .description("Triggers when a new item is added to the database.")
        .type(TriggerType.POLLING)
        .properties(
            string(ID)
                .label("Database ID")
                .description("The ID of the database.")
                .options((OptionsFunction<String>) NotionUtils::getDatabaseIdOptions)
                .required(true))
        .output()
        .poll(NotionNewDatabaseItemTrigger::poll);

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        Instant now = Instant.now();

        boolean editorEnvironment = context.isEditorEnvironment();
        Instant start = closureParameters.get(
            LAST_TIME_CHECKED, Instant.class,
            editorEnvironment ? now.minus(Duration.ofHours(3)) : now);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(start);

        String url = "/databases/%s/query".formatted(inputParameters.getRequiredString(ID));

        List<Object> items = getAllItems(
            context, url, editorEnvironment,
            "filter", Map.of("timestamp", "created_time", "created_time", Map.of("on_or_after", timestamp)),
            "sorts", List.of(Map.of("timestamp", "created_time", "direction", "descending")));

        return new PollOutput(items, Map.of(LAST_TIME_CHECKED, now), false);
    }

    private NotionNewDatabaseItemTrigger() {
    }
}
