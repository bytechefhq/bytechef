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
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
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
public class AirtableUpdateRecordAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("updateRecord")
        .title("Update Record")
        .description("Update an existing record in an Airtable table.")
        .metadata(
            Map.of(
                "method", "PATCH",
                "path", "/{baseId}/{tableId}/{recordId}", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json", "responseType", ResponseType.JSON

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
            string("recordId").label("Row ID")
                .description("ID of the record that will be retrieved.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getRecordIdOptions)
                .optionsLookupDependsOn("baseId", "tableId")
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            dynamicProperties("fields")
                .properties((ActionDefinition.PropertiesFunction) AirtableUtils::getFieldsProperties)
                .propertiesLookupDependsOn("baseId", "tableId", "recordId")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .output();

    private AirtableUpdateRecordAction() {
    }
}
