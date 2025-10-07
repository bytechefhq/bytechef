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

package com.bytechef.component.nocodb.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.BASE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.BASE_ID_PROPERTY;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.RECORD_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID_PROPERTY;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.WORKSPACE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.WORKSPACE_ID_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.nocodb.util.NocoDbUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NocoDbDeleteRecords {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRecords")
        .title("Delete Records")
        .description("Deletes existing records in the specified table.")
        .properties(
            WORKSPACE_ID_PROPERTY,
            BASE_ID_PROPERTY,
            TABLE_ID_PROPERTY,
            array(RECORD_ID)
                .label("Records ID")
                .description("ID of the records to delete.")
                .items(integer())
                .options((OptionsFunction<Long>) NocoDbUtils::getRecordIdOptions)
                .optionsLookupDependsOn(TABLE_ID, BASE_ID, WORKSPACE_ID)
                .minItems(1)
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(object()
                        .properties(integer("Id")
                            .description("Id of the deleted record.")))))
        .perform(NocoDbDeleteRecords::perform);

    private NocoDbDeleteRecords() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Map<String, String>> recordIds = new ArrayList<>();

        for (String recordId : inputParameters.getRequiredList(RECORD_ID, String.class)) {
            recordIds.add(Map.of("Id", recordId));
        }

        return context
            .http(http -> http.delete(
                "/api/v2/tables/%s/records".formatted(inputParameters.getRequiredString(TABLE_ID))))
            .body(Http.Body.of(recordIds))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
