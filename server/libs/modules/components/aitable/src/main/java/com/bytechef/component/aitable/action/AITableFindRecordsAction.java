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
import static com.bytechef.component.aitable.constant.AITableConstants.PAGE_SIZE;
import static com.bytechef.component.aitable.constant.AITableConstants.RECORD_IDS;
import static com.bytechef.component.aitable.constant.AITableConstants.SPACE_ID_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.aitable.util.AITableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class AITableFindRecordsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("findRecords")
        .title("Find Records")
        .description("Find records in datasheet")
        .help("", "https://docs.bytechef.io/reference/components/aitable_v1#find-records")
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
                .required(false))
        .output()
        .perform(AITableFindRecordsAction::perform);

    private AITableFindRecordsAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        int pageNum = 1;
        int totalReceived = 0;
        int total = 0;

        List<Map<?, ?>> records = new ArrayList<>();
        do {
            Map<String, ?> body = context
                .http(http -> http.get("/datasheets/" + inputParameters.getRequiredString(DATASHEET_ID) + "/records"))
                .queryParameters(
                    FIELDS, String.join(",", inputParameters.getList(FIELDS, String.class, List.of())),
                    RECORD_IDS, String.join(",", inputParameters.getList(RECORD_IDS, String.class, List.of())),
                    PAGE_SIZE, 1000,
                    "pageNum", pageNum)
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("data") instanceof Map<?, ?> data) {
                if (data.get("records") instanceof List<?> list) {
                    for (Object record : list) {
                        if (record instanceof Map<?, ?> map) {
                            records.add(map);
                        }
                    }
                }

                totalReceived += (Integer) data.get(PAGE_SIZE);
                pageNum++;
                total = (Integer) data.get("total");
            }
        } while (totalReceived < total);

        return records;
    }
}
