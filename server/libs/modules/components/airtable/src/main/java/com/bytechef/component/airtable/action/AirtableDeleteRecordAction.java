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

package com.bytechef.component.airtable.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.airtable.util.AirtableUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AirtableDeleteRecordAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("deleteRecord")
        .title("Delete Record")
        .description("Deletes a single record from a table.")
        .metadata(
            Map.of(
                "method", "DELETE",
                "path", "/{baseId}/{tableId}/{recordId}"

            ))
        .properties(string("baseId").label("Base ID")
            .description("ID of the base where table is located.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getBaseIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("tableId").label("Table ID")
                .description("ID of the table where the record is located.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getTableIdOptions)
                .optionsLookupDependsOn("baseId")
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            string("recordId").label("Record ID")
                .description("ID of the record that will be deleted.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getRecordIdOptions)
                .optionsLookupDependsOn("tableId", "baseId")
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(string("id").description("The ID of the deleted record.")
            .required(false),
            bool("deleted").description("Indicates if the record was deleted.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AirtableDeleteRecordAction() {
    }
}
