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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveGetDealDetailsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getDealDetails")
        .title("Get details of a deal")
        .description("Returns the details of a specific deal.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/deals/{id}"

            ))
        .properties(integer("id").label("Deal")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(object("body")
                .properties(object("data")
                    .properties(
                        integer("id").required(false),
                        object("user_id")
                            .properties(integer("id").required(false), string("name").required(false),
                                string("email").required(false))
                            .required(false),
                        object("person_id").properties(string("name").required(false))
                            .required(false),
                        object("org_id").properties(string("name").required(false), string("owner_id").required(false))
                            .required(false),
                        integer("stage_id").required(false), string("title").required(false),
                        integer("value").required(false), string("currency").required(false),
                        string("status").required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipedriveGetDealDetailsAction() {
    }
}
