
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.component.exception.ComponentExecutionException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

import static com.bytechef.hermes.definition.DefinitionDSL.string;

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
        .poll(AirtableNewRecordTrigger::poll);

    protected static PollOutput poll(
        ParameterMap inputParameters, ParameterMap closureParameters, TriggerContext context)
        throws ComponentExecutionException {

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, LocalDateTime.now());
        LocalDateTime endDate = LocalDateTime.now();

        List<Map<?, ?>> records = context.http(http -> http.get(
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
            .getBody();

        return new PollOutput(records, Map.of(LAST_TIME_CHECKED, endDate), false);
    }
}
