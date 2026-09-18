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

package com.bytechef.component.neon.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.neon.constant.NeonConstants.ROW_DATA;
import static com.bytechef.component.neon.constant.NeonConstants.TABLE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.neon.util.NeonUtils;

public class NeonCreateRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRow")
        .title("Create Row")
        .description("Inserts a new row into a table.")
        .properties(
            string(TABLE)
                .label("Table")
                .description("Name of the table to insert the row into.")
                .required(true),
            object(ROW_DATA)
                .label("Row Data")
                .description("The row to insert, as column name/value pairs.")
                .required(true))
        .output()
        .perform(NeonCreateRowAction::perform);

    private NeonCreateRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Object body = context
            .http(http -> http.post("/" + inputParameters.getRequiredString(TABLE)))
            .header("Prefer", "return=representation")
            .body(Http.Body.of(inputParameters.getRequiredMap(ROW_DATA)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();

        return NeonUtils.getFirstRow(body);
    }
}
