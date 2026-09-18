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
import static com.bytechef.component.neon.constant.NeonConstants.filtersProperty;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.neon.util.NeonUtils;

public class NeonUpdateRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateRow")
        .title("Update Row")
        .description("Updates the row(s) matching the given filters.")
        .properties(
            string(TABLE)
                .label("Table")
                .description("Name of the table containing the row(s) to update.")
                .required(true),
            filtersProperty(true),
            object(ROW_DATA)
                .label("Row Data")
                .description("Column name/value pairs to update.")
                .required(true))
        .output()
        .perform(NeonUpdateRowAction::perform);

    private NeonUpdateRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.patch("/" + inputParameters.getRequiredString(TABLE)))
            .queryParameters(NeonUtils.getFilterQueryParameters(inputParameters))
            .header("Prefer", "return=representation")
            .body(Http.Body.of(inputParameters.getRequiredMap(ROW_DATA)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
