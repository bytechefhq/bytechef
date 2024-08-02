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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveAddOrganizationAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addOrganization")
        .title("Add an organization")
        .description("Adds a new organization.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/organizations", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("name").label("Name")
            .description("The name of the organization.")
            .required(true),
            integer("owner_id").label("Owner Id")
                .description("User who will be marked as the owner of this organization.")
                .required(false))
            .label("Organization")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(object("data")
                    .properties(integer("id").required(false), integer("company_id").required(false),
                        object("owner_id")
                            .properties(integer("id").required(false), string("name").required(false),
                                string("email").required(false))
                            .required(false),
                        string("name").required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private PipedriveAddOrganizationAction() {
    }
}
