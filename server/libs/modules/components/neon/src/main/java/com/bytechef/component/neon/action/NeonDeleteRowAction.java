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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.neon.constant.NeonConstants.TABLE;
import static com.bytechef.component.neon.constant.NeonConstants.criteriaFilterArrayProperty;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.neon.util.NeonUtils;

public class NeonDeleteRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRow")
        .title("Delete Row")
        .description(
            "Deletes the row(s) matching the given filters. Fails if no row matches.")
        .properties(
            string(TABLE)
                .label("Table")
                .description("Name of the table containing the row(s) to delete.")
                .required(true),
            criteriaFilterArrayProperty(true))
        .output()
        .perform(NeonDeleteRowAction::perform);

    private NeonDeleteRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String table = inputParameters.getRequiredString(TABLE);

        Object body = context
            .http(http -> http.delete("/" + table))
            .queryParameters(NeonUtils.getFilterQueryParameters(inputParameters))
            .header("Prefer", "return=representation")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();

        NeonUtils.requireRowsAffected(body, table);

        return body;
    }
}
