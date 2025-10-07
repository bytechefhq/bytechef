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
import static com.bytechef.component.definition.ComponentDsl.*;

import com.bytechef.component.airtable.util.AirtableUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.PropertiesDataSource;
import java.util.Map;

public class AirtableUpdateRecordAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("updateRecord")
        .title("Update Record")
        .description("Updates a record in airtable table")
        .metadata(
            Map.of(
                "method", "PATCH",
                "path", "/{baseId}/{tableId}/{recordId}",
                "bodyContentType", Context.Http.BodyContentType.JSON,
                "mimeType", "application/json",
                "responseType", Context.Http.ResponseType.JSON))
        .properties(
            string("baseId").label("Base ID")
                .description("ID of base where table is located")
                .required(true)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AirtableUtils::getBaseIdOptions)
                .metadata(Map.of("type", PropertyType.PATH)),
            string("tableId").label("Table ID")
                .description("The table where the record will be created")
                .required(true)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AirtableUtils::getTableIdOptions)
                .optionsLookupDependsOn("baseId")
                .metadata(Map.of("type", PropertyType.PATH)),
            string("recordId").label("Record ID")
                .description("The ID of record inside the table")
                .required(true)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AirtableUtils::getRecordIdOptions)
                .optionsLookupDependsOn("tableId")
                .metadata(Map.of("type", PropertyType.PATH)),
            dynamicProperties("fields")
                .properties((PropertiesDataSource.ActionPropertiesFunction) AirtableUtils::getFieldsProperties)
                .propertiesLookupDependsOn("baseId", "tableId")
                .required(false)
                .metadata(Map.of("type", PropertyType.BODY)))
        .output();

    private AirtableUpdateRecordAction() {
    }

}
