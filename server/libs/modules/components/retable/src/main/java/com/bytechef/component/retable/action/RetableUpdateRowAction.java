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

package com.bytechef.component.retable.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.retable.constant.RetableConstants.COLUMNS;
import static com.bytechef.component.retable.constant.RetableConstants.COLUMN_ID;
import static com.bytechef.component.retable.constant.RetableConstants.PROJECT_ID;
import static com.bytechef.component.retable.constant.RetableConstants.RETABLE_ID;
import static com.bytechef.component.retable.constant.RetableConstants.ROWS_IDS;
import static com.bytechef.component.retable.constant.RetableConstants.ROW_ID;
import static com.bytechef.component.retable.constant.RetableConstants.WORKSPACE_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.retable.util.RetablePropertiesUtils;
import com.bytechef.component.retable.util.RetableUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class RetableUpdateRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateRow")
        .title("Update Row")
        .description("Update a row.")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .description("ID of the workspace.")
                .options((OptionsFunction<String>) RetableUtils::getWorkspaceIdOptions)
                .required(true),
            string(PROJECT_ID)
                .label("Project ID")
                .description("ID of the project.")
                .optionsLookupDependsOn(WORKSPACE_ID)
                .options((OptionsFunction<String>) RetableUtils::getProjectIdOptions)
                .required(true),
            string(RETABLE_ID)
                .label("Retable ID")
                .description("ID of the retable.")
                .optionsLookupDependsOn(PROJECT_ID)
                .options((OptionsFunction<String>) RetableUtils::getRetableIdOptions)
                .required(true),
            integer(ROW_ID)
                .label("Row ID")
                .description("ID of the row to update.")
                .required(true),
            dynamicProperties(ROWS_IDS)
                .properties(RetablePropertiesUtils::createPropertiesForRowValues)
                .propertiesLookupDependsOn(PROJECT_ID)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("data")
                            .description("Row IDs.")
                            .items(integer()))))
        .perform(RetableUpdateRowAction::perform);

    private RetableUpdateRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Map<String, Object>> retableRowValues = convertPropertyToRetableRowValue(inputParameters.getMap(ROWS_IDS));

        return context
            .http(http -> http.put("/retable/" + inputParameters.getRequiredString(RETABLE_ID) + "/data"))
            .configuration(responseType(ResponseType.JSON))
            .body(
                Body.of(
                    Map.of(
                        "rows", List.of(
                            Map.of(
                                ROW_ID, inputParameters.getRequiredInteger(ROW_ID),
                                COLUMNS, retableRowValues)))))
            .execute()
            .getBody();
    }

    private static List<Map<String, Object>> convertPropertyToRetableRowValue(Map<String, ?> rowsIds) {
        return rowsIds
            .entrySet()
            .stream()
            .map(entry -> Map.of(COLUMN_ID, entry.getKey(), "update_cell_value", entry.getValue()))
            .toList();
    }
}
