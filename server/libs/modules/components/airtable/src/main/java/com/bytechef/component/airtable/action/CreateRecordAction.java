
/*
 * Copyright 2021 <your company/name>.
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
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.oneOf;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class CreateRecordAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createRecord")
        .display(
            display("Creates a record.")
                .description("Adds a record into an Airtable table."))
        .metadata(
            Map.of(
                "requestMethod", "POST",
                "path", "/{baseId}/{tableId}", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("baseId").label("BaseId")
            .description("The base id.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("tableId").label("TableId")
                .description("The table id.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            object().properties(object("fields").additionalProperties(oneOf())
                .placeholder("Add")
                .label("Fields")
                .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object().properties(dateTime("createdTime").label("CreatedTime")
            .required(false),
            object("fields").additionalProperties(oneOf())
                .placeholder("Add")
                .label("Fields")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)));
}
