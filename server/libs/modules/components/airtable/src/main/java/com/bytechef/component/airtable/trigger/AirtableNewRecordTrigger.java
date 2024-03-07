/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.airtable.trigger;

import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;

import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.PollTriggerOutputFunction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class AirtableNewRecordTrigger {

    private static final String BASE_ID = "baseId";
    private static final String TABLE_ID = "tableId";
    private static final String LAST_TIME_CHECKED = "lastTimeChecked";
    private static final String TRIGGER_FIELD = "triggerField";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRecord")
        .title("New Record")
        .description(
            "Trigger off when a new entry is added to the table that you have selected.")
        .type(TriggerDefinition.TriggerType.POLLING)
        .properties(
            string(BASE_ID)
                .label("BaseId")
                .description("The base id.")
                .required(true),
            string(TABLE_ID)
                .label("TableId")
                .description("The table id.")
                .required(true),
            string(TRIGGER_FIELD)
                .label("TriggerField")
                .description(
                    "It is essential to have a field for Created Time or Last Modified Time in your schema since this field is used to sort records, and the trigger will not function correctly without it. Therefore, if you don't have such a field in your schema, please create one.")
                .required(true))
        .output(getOutput())
        .poll(AirtableNewRecordTrigger::poll);

    protected static PollOutput poll(
        Parameters inputParameters, Parameters closureParameters, TriggerContext context) {

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, LocalDateTime.now());
        LocalDateTime endDate = LocalDateTime.now();

        List<Map<String, ?>> records = context.http(http -> http.get(
            "/{%s}/{%s}".formatted(
                inputParameters.getRequiredString(TABLE_ID),
                inputParameters.getRequiredString(BASE_ID))))
            .queryParameters(
                Map.of(
                    "filterByFormula",
                    List.of(
                        "IS_AFTER({%s}, DATETIME_PARSE(\"%s\", \"YYYY-MM-DD HH:mm:ss\"))"
                            .formatted(
                                inputParameters.getRequiredString(TRIGGER_FIELD),
                                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});

        return new PollOutput(records, Map.of(LAST_TIME_CHECKED, endDate), false);
    }

    protected static PollTriggerOutputFunction getOutput() {
        // TODO

        return (inputParameters, closureParameters, context) -> null;
    }
}
