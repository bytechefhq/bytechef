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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.BASE_ID_PROPERTY;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.FIELDS;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.SORT;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.TABLE_ID_PROPERTY;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.WHERE;
import static com.bytechef.component.nocodb.constant.NocoDbConstants.WORKSPACE_ID_PROPERTY;
import static java.util.stream.Collectors.joining;

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
public class NocoDbSearchRecords {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchRecords")
        .title("Search Records")
        .description("Searches for records in the specified table.")
        .properties(
            WORKSPACE_ID_PROPERTY,
            BASE_ID_PROPERTY,
            TABLE_ID_PROPERTY,
            array(FIELDS)
                .label("Fields")
                .description(
                    "Fields to include in the response. By default, all the fields are included in the response.")
                .items(string())
                .options((OptionsFunction<String>) NocoDbUtils::getFieldNameOptions)
                .optionsLookupDependsOn(TABLE_ID)
                .required(false),
            array(SORT)
                .label("Sort By")
                .description("Fields by which you want to sort the records in your response.")
                .items(
                    object()
                        .properties(
                            string("field")
                                .label("Field")
                                .description("Field to sort by.")
                                .options((OptionsFunction<String>) NocoDbUtils::getFieldNameOptions)
                                .optionsLookupDependsOn(TABLE_ID)
                                .required(true),
                            string("order")
                                .label("Order")
                                .description("Order in which to sort the records.")
                                .options(
                                    option("Ascending", "asc"),
                                    option("Descending", "desc"))
                                .defaultValue("asc")
                                .required(true)))
                .required(false),
            string(WHERE)
                .label("Where")
                .description(
                    "Specific conditions for filtering records in your response. Multiple conditions can be " +
                        "combined using logical operators such as 'and' and 'or'. Each condition consists of " +
                        "three parts: a field name, a comparison operator, and a value.")
                .exampleValue("(field1,eq,value1)~and(field2,eq,value2)")
                .required(false))
        .output()
        .perform(NocoDbSearchRecords::perform);

    private NocoDbSearchRecords() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String sort = null;
        List<SortRecord> sortRecords = inputParameters.getList(SORT, SortRecord.class);

        if (sortRecords != null && !sortRecords.isEmpty()) {
            sort = sortRecords.stream()
                .map(sortRecord -> sortRecord.order()
                    .equals("asc") ? sortRecord.field() : "-" + sortRecord.field())
                .collect(joining(","));
        }

        return context.http(
            http -> http.get("/api/v2/tables/%s/records".formatted(inputParameters.getRequiredString(TABLE_ID))))
            .queryParameters(
                FIELDS, String.join(",", inputParameters.getList(FIELDS, String.class, List.of())),
                SORT, sort,
                WHERE, inputParameters.getString(WHERE))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    record SortRecord(String field, String order) {
    }
}
