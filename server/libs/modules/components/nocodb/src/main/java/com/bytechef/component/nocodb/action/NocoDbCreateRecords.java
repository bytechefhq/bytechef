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
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.BASE_ID_PROPERTY;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_COLUMNS;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID_PROPERTY;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.WORKSPACE_ID_PROPERTY;
import static com.bytechef.component.nocodb.util.NocoDbUtils.transformRecordsForInsertion;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.nocodb.util.NocoDbUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NocoDbCreateRecords {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRecords")
        .title("Create Records")
        .description("Creates a new records in the specified table.")
        .properties(
            WORKSPACE_ID_PROPERTY,
            BASE_ID_PROPERTY,
            TABLE_ID_PROPERTY,
            dynamicProperties(TABLE_COLUMNS)
                .propertiesLookupDependsOn(TABLE_ID)
                .properties(NocoDbUtils.createPropertiesForRecord(true)))
        .output(
            outputSchema(
                array()
                    .items(object()
                        .properties(
                            integer("Id")
                                .description("Id of the created record.")))))
        .perform(NocoDbCreateRecords::perform);

    private NocoDbCreateRecords() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Map<String, Object>> newRecords = transformRecordsForInsertion(inputParameters);

        return context
            .http(http -> http.post("/api/v2/tables/%s/records".formatted(inputParameters.getRequiredString(TABLE_ID))))
            .body(Http.Body.of(newRecords))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
