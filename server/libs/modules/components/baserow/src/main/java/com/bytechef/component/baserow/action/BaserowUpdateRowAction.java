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

package com.bytechef.component.baserow.action;

import static com.bytechef.component.baserow.constant.BaserowConstants.FIELDS;
import static com.bytechef.component.baserow.constant.BaserowConstants.FIELDS_DYNAMIC_PROPERTY;
import static com.bytechef.component.baserow.constant.BaserowConstants.ROW_ID;
import static com.bytechef.component.baserow.constant.BaserowConstants.TABLE_ID;
import static com.bytechef.component.baserow.constant.BaserowConstants.UPDATE_ROW;
import static com.bytechef.component.baserow.constant.BaserowConstants.UPDATE_ROW_DESCRIPTION;
import static com.bytechef.component.baserow.constant.BaserowConstants.UPDATE_ROW_TITLE;
import static com.bytechef.component.baserow.constant.BaserowConstants.USER_FIELD_NAMES;
import static com.bytechef.component.baserow.constant.BaserowConstants.USER_FIELD_NAMES_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class BaserowUpdateRowAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        integer(TABLE_ID)
            .label("Table ID")
            .description("ID of the table containing the row to be updated.")
            .required(true),
        integer(ROW_ID)
            .label("Row ID")
            .description("ID of the row to be updated.")
            .required(true),
        USER_FIELD_NAMES_PROPERTY,
        FIELDS_DYNAMIC_PROPERTY
    };

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPDATE_ROW)
        .title(UPDATE_ROW_TITLE)
        .description(UPDATE_ROW_DESCRIPTION)
        .properties(PROPERTIES)
        .output()
        .perform(BaserowUpdateRowAction::perform);

    private BaserowUpdateRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.patch(
                "/database/rows/table/%s/%s/"
                    .formatted(inputParameters.getRequiredString(TABLE_ID), inputParameters.getRequiredString(ROW_ID))))
            .queryParameter(USER_FIELD_NAMES, inputParameters.getString(USER_FIELD_NAMES))
            .body(Http.Body.of(inputParameters.getRequiredMap(FIELDS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
