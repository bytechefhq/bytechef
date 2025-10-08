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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.BASE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.BASE_ID_PROPERTY;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.FIELDS;
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
import java.util.List;

/**
 * @author Monika Kušter
 */
public class NocoDbGetRecord {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getRecord")
        .title("Get Record")
        .description("Gets a record from the specified table.")
        .properties(
            WORKSPACE_ID_PROPERTY,
            BASE_ID_PROPERTY,
            TABLE_ID_PROPERTY,
            string(RECORD_ID)
                .label("Record ID")
                .description("ID of the record to retrieve.")
                .options((OptionsFunction<Long>) NocoDbUtils::getRecordIdOptions)
                .optionsLookupDependsOn(TABLE_ID, BASE_ID, WORKSPACE_ID)
                .required(true),
            array(FIELDS)
                .label("Fields")
                .description(
                    "Fields to include in the response. By default, all the fields are included in the response.")
                .items(string())
                .required(false))
        .output()
        .perform(NocoDbGetRecord::perform);

    private NocoDbGetRecord() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get(
                "/api/v2/tables/%s/records/%s".formatted(
                    inputParameters.getRequiredString(TABLE_ID), inputParameters.getRequiredString(RECORD_ID))))
            .queryParameter(FIELDS, String.join(",", inputParameters.getList(FIELDS, String.class, List.of())))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
