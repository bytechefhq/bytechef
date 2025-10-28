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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.coda.util.CodaUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class CodaUpdateRowAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("updateRow")
        .title("Update Row")
        .description("Updates the specified row in the table.")
        .metadata(
            Map.of(
                "method", "PUT",
                "path", "/docs/{docId}/tables/{tableId}/rows/{rowId}", "bodyContentType", BodyContentType.JSON,
                "mimeType", "application/json"

            ))
        .properties(string("docId").label("Doc ID")
            .description("ID of the doc.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) CodaUtils::getDocIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("tableId").label("Table ID")
                .description("ID or name of the table.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) CodaUtils::getTableIdOptions)
                .optionsLookupDependsOn("docId")
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            string("rowId").label("Row ID")
                .description("ID or name of the row.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) CodaUtils::getRowIdOptions)
                .optionsLookupDependsOn("docId", "tableId")
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            object("row").properties(array("cells").items(object().properties(string("column").label("Column")
                .description("Column ID.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) CodaUtils::getColumnOptions)
                .optionsLookupDependsOn("docId", "tableId"),
                string("value").label("Value")
                    .description("A Coda result or entity expressed as a string.")
                    .required(true)))
                .placeholder("Add to Cells")
                .label("Cells")
                .required(true))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Row")
                .description("An edit made to a particular row.")
                .required(true))
        .output(outputSchema(object()
            .properties(string("requestId").description("An arbitrary unique identifier for this request.")
                .required(false),
                string("id").description("ID of the updated row.")
                    .required(false))
            .description("The result of a row update.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private CodaUpdateRowAction() {
    }
}
