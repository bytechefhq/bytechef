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

package com.bytechef.component.airtable.trigger;

import static com.bytechef.component.airtable.constant.AirtableConstants.BASE_ID;
import static com.bytechef.component.airtable.constant.AirtableConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.airtable.util.AirtableUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class AirtableNewRecordTrigger {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";
    protected static final String TRIGGER_FIELD = "triggerField";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRecord")
        .title("New Record")
        .description("Trigger off when a new entry is added to the table that you have selected.")
        .type(TriggerType.POLLING)
        .properties(
            string(BASE_ID)
                .label("Base ID")
                .description("ID of the base which contains the table that you want to monitor.")
                .options((OptionsFunction<String>) AirtableUtils::getBaseIdOptions)
                .required(true),
            string(TABLE_ID)
                .label("Table")
                .description("The table to monitor for new records.")
                .options((OptionsFunction<String>) AirtableUtils::getTableIdOptions)
                .optionsLookupDependsOn(BASE_ID)
                .required(true),
            string(TRIGGER_FIELD)
                .label("Trigger Field")
                .description(
                    "It is essential to have a field for Created Time or Last Modified Time in your schema since " +
                        "this field is used to sort records, and the trigger will not function correctly without it. " +
                        "Therefore, if you don't have such a field in your schema, please create one.")
                .required(true))
        .output()
        .poll(AirtableNewRecordTrigger::poll);

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, triggerContext.isEditorEnvironment() ? now.minusHours(3) : now);

        String filterByFormula = URLEncoder.encode(
            String.format(
                "IS_AFTER({%s}, DATETIME_PARSE('%s', 'YYYY-MM-DD HH:mm:ss'))",
                inputParameters.getRequiredString(TRIGGER_FIELD),
                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
            StandardCharsets.UTF_8);

        Http.Response response = triggerContext.http(http -> http.get(
            String.format(
                "/%s/%s", inputParameters.getRequiredString(BASE_ID), inputParameters.getRequiredString(TABLE_ID))))
            .queryParameter("filterByFormula", filterByFormula)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        Map<String, List<?>> body = response.getBody(new TypeReference<>() {});

        return new PollOutput(body.get("records"), Map.of(LAST_TIME_CHECKED, now), false);
    }
}
