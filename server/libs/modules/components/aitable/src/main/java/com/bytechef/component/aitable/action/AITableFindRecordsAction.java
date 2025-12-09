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

package com.bytechef.component.aitable.action;

import static com.bytechef.component.aitable.constant.AITableConstants.DATASHEET_ID;
import static com.bytechef.component.aitable.constant.AITableConstants.DATASHEET_ID_PROPERTY;
import static com.bytechef.component.aitable.constant.AITableConstants.FIELDS;
import static com.bytechef.component.aitable.constant.AITableConstants.MAX_RECORDS;
import static com.bytechef.component.aitable.constant.AITableConstants.RECORD_IDS;
import static com.bytechef.component.aitable.constant.AITableConstants.SPACE_ID_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.aitable.util.AITableUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class AITableFindRecordsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("findRecords")
        .title("Find Records")
        .description("Find records in datasheet")
        .properties(
            SPACE_ID_PROPERTY,
            DATASHEET_ID_PROPERTY,
            array(FIELDS)
                .label("Field Names")
                .description("The returned record results are limited to the specified fields.")
                .items(string())
                .options((OptionsFunction<String>) AITableUtils::getFieldNamesOptions)
                .optionsLookupDependsOn(DATASHEET_ID)
                .required(false),
            array(RECORD_IDS)
                .label("Record IDs")
                .description("The IDs of the records to find.")
                .items(string())
                .required(false),
            integer(MAX_RECORDS)
                .label("Max Records")
                .description("How many records are returned in total")
                .required(false))
        .output()
        .perform(AITableFindRecordsAction::perform);

    private AITableFindRecordsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<String> fields = inputParameters.getList(FIELDS, String.class, List.of());
        List<String> recordIds = inputParameters.getList(RECORD_IDS, String.class, List.of());
        int maxRecords = inputParameters.getInteger(MAX_RECORDS);

        return context
            .http(http -> http.get("/datasheets/" + inputParameters.getRequiredString(DATASHEET_ID) + "/records"))
            .queryParameters(
                FIELDS, String.join(",", fields),
                RECORD_IDS, String.join(",", recordIds),
                MAX_RECORDS, maxRecords)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
