/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AirtableGetRecordAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getRecord")
        .title("Get Record")
        .description("Retrieves a single record.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/{baseId}/{tableId}/{recordId}"

            ))
        .properties(string("baseId").label("Base ID")
            .description("ID of the base where table is located.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("tableId").label("Table ID")
                .description("ID of the table where the record is located.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            string("recordId").label("Record ID")
                .description("ID of the record that will be deleted.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(string("id").required(false), dateTime("createdTime").required(false),
                object("fields").additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AirtableGetRecordAction() {
    }
}
