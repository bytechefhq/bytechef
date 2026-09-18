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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.neon.constant.NeonConstants.LIMIT;
import static com.bytechef.component.neon.constant.NeonConstants.OFFSET;
import static com.bytechef.component.neon.constant.NeonConstants.ORDER;
import static com.bytechef.component.neon.constant.NeonConstants.SELECT;
import static com.bytechef.component.neon.constant.NeonConstants.TABLE;
import static com.bytechef.component.neon.constant.NeonConstants.filtersProperty;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.neon.util.NeonUtils;
import java.util.List;
import java.util.Map;

public class NeonListRowsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listRows")
        .title("List Rows")
        .description("Lists rows from a table or view.")
        .properties(
            string(TABLE)
                .label("Table")
                .description("Name of the table or view to query.")
                .required(true),
            string(SELECT)
                .label("Select")
                .description("Comma-separated list of columns to return.")
                .exampleValue("id,name,email")
                .required(false),
            filtersProperty(false),
            string(ORDER)
                .label("Order By")
                .description("Column(s) to order by. Use .asc or .desc, e.g. created_at.desc.")
                .exampleValue("created_at.desc")
                .required(false),
            integer(LIMIT)
                .label("Limit")
                .description("Maximum number of rows to return.")
                .required(false),
            integer(OFFSET)
                .label("Offset")
                .description("Number of rows to skip before returning results.")
                .required(false))
        .output()
        .perform(NeonListRowsAction::perform);

    private NeonListRowsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, List<String>> queryParameters = NeonUtils.getFilterQueryParameters(inputParameters);

        String select = inputParameters.getString(SELECT);

        if (select != null) {
            queryParameters.put(SELECT, List.of(select));
        }

        String order = inputParameters.getString(ORDER);

        if (order != null) {
            queryParameters.put(ORDER, List.of(order));
        }

        Integer limit = inputParameters.getInteger(LIMIT);

        if (limit != null) {
            queryParameters.put(LIMIT, List.of(String.valueOf(limit)));
        }

        Integer offset = inputParameters.getInteger(OFFSET);

        if (offset != null) {
            queryParameters.put(OFFSET, List.of(String.valueOf(offset)));
        }

        return context
            .http(http -> http.get("/" + inputParameters.getRequiredString(TABLE)))
            .queryParameters(queryParameters)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
