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

package com.bytechef.component.neon.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.neon.constant.NeonConstants.LIMIT;
import static com.bytechef.component.neon.constant.NeonConstants.ORDER;
import static com.bytechef.component.neon.constant.NeonConstants.ORDER_BY_COLUMN;
import static com.bytechef.component.neon.constant.NeonConstants.ORDER_DIRECTION;
import static com.bytechef.component.neon.constant.NeonConstants.SELECT;
import static com.bytechef.component.neon.constant.NeonConstants.TABLE;
import static com.bytechef.component.neon.constant.NeonConstants.filtersProperty;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.neon.util.NeonUtils;
import java.util.List;
import java.util.Map;

public class NeonNewRowTrigger {

    private static final String LAST_VALUE = "lastValue";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRow")
        .title("New Row")
        .description("Triggers when a new row is added to a table.")
        .type(TriggerType.POLLING)
        .properties(
            string(TABLE)
                .label("Table")
                .description("Name of the table or view to poll.")
                .required(true),
            string(ORDER_BY_COLUMN)
                .label("Column to Order By")
                .description("Use something like a created timestamp or an auto-incrementing id.")
                .required(true),
            string(ORDER_DIRECTION)
                .label("Order Direction")
                .description("The direction new rows are expected to sort in for the column above.")
                .options(
                    option("Ascending", "ASC"),
                    option("Descending", "DESC"))
                .defaultValue("DESC")
                .required(true),
            string(SELECT)
                .label("Select")
                .description("Comma-separated list of columns to return.")
                .exampleValue("id,name,email")
                .required(false),
            filtersProperty(false))
        .output(
            outputSchema(
                array()
                    .items(object())
                    .description("The list of new rows.")),
            sampleOutput(List.of(Map.of("id", 1))))
        .poll(NeonNewRowTrigger::poll);

    private NeonNewRowTrigger() {
    }

    public static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        String table = inputParameters.getRequiredString(TABLE);
        String orderByColumn = inputParameters.getRequiredString(ORDER_BY_COLUMN);
        String orderDirection = inputParameters.getRequiredString(ORDER_DIRECTION);

        if (!orderDirection.equals("ASC") && !orderDirection.equals("DESC")) {
            throw new IllegalArgumentException("Invalid order direction: " + orderDirection);
        }

        Object lastValue = closureParameters.get(LAST_VALUE);

        Map<String, List<String>> queryParameters = NeonUtils.getFilterQueryParameters(inputParameters);

        queryParameters.put(ORDER, List.of(orderByColumn + "." + orderDirection.toLowerCase()));

        String select = inputParameters.getString(SELECT);

        if (select != null) {
            queryParameters.put(SELECT, List.of(select));
        }

        if (lastValue == null) {
            queryParameters.put(LIMIT, List.of("1"));
        } else {
            queryParameters.put(
                orderByColumn, List.of((orderDirection.equals("ASC") ? "gt." : "lt.") + lastValue));
        }

        Object body = context
            .http(http -> http.get("/" + table))
            .queryParameters(queryParameters)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();

        List<Map<String, Object>> rows = body instanceof List<?> list ? castRows(list) : List.of();

        if (rows.isEmpty()) {
            return new PollOutput(List.of(), closureParameters.toMap(), false);
        }

        Map<String, Object> edgeRow = orderDirection.equals("ASC") ? rows.getLast() : rows.getFirst();

        return new PollOutput(
            lastValue == null ? List.of() : rows, Map.of(LAST_VALUE, edgeRow.get(orderByColumn)), false);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castRows(List<?> list) {
        return (List<Map<String, Object>>) list;
    }
}
