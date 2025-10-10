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

package com.bytechef.component.coda.action;

import static com.bytechef.component.coda.constant.CodaConstants.DOC_ID;
import static com.bytechef.component.coda.constant.CodaConstants.ROW_VALUES;
import static com.bytechef.component.coda.constant.CodaConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.coda.util.CodaPropertiesUtils;
import com.bytechef.component.coda.util.CodaUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class CodaInsertRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("insertRow")
        .title("Insert Row")
        .description("Inserts row into a table.")
        .properties(
            string(DOC_ID)
                .label("Doc ID")
                .description("ID of the doc.")
                .required(true)
                .options((OptionsFunction<String>) CodaUtils::getDocIdOptions),
            string(TABLE_ID)
                .label("Table ID")
                .description("ID of the table.")
                .required(true)
                .options((OptionsFunction<String>) CodaUtils::getTableIdOptions)
                .optionsLookupDependsOn(DOC_ID),
            dynamicProperties(ROW_VALUES)
                .properties(CodaPropertiesUtils::createPropertiesForRowValues)
                .propertiesLookupDependsOn(DOC_ID, TABLE_ID)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("requestId")
                            .description("An arbitrary unique identifier for this request."),
                        array("addedRowIds")
                            .description("Row IDs for rows that will be added.")
                            .items(
                                string()
                                    .description("Row IDs for rows that will be added.")))))
        .perform(CodaInsertRowAction::perform);

    private CodaInsertRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> codaRowValues = convertPropertyToCodaRowValue(inputParameters.getMap(ROW_VALUES));

        return context.http(
            http -> http.post("/docs/%s/tables/%s/rows".formatted(
                inputParameters.getRequiredString(DOC_ID), inputParameters.getRequiredString(TABLE_ID))))
            .body(Body.of(codaRowValues))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static Map<String, Object> convertPropertyToCodaRowValue(Map<String, ?> rowValuesInput) {
        List<Map<String, Object>> cells = rowValuesInput.entrySet()
            .stream()
            .map(entry -> Map.of("column", entry.getKey(), "value", entry.getValue()))
            .toList();

        return Map.of("rows", List.of(Map.of("cells", cells)));
    }
}
