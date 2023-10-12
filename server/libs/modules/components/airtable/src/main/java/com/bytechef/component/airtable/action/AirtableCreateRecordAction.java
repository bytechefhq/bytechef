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

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.nullable;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.ComponentDSL.time;
import static com.bytechef.hermes.component.definition.Context.Http.BodyContentType;
import static com.bytechef.hermes.component.definition.Context.Http.ResponseType;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AirtableCreateRecordAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createRecord")
        .title("Creates a record")
        .description("Adds a record into an Airtable table.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/{baseId}/{tableId}", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("baseId").label("Base Id")
            .description("The base id.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("tableId").label("Table Id")
                .description("The table id.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            dynamicProperties("__item").label("Item")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(dateTime("createdTime").required(false), object("fields").additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
