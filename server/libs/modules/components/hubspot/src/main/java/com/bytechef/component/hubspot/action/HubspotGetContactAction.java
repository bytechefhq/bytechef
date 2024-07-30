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

package com.bytechef.component.hubspot.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class HubspotGetContactAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getContact")
        .title("Get Contact")
        .description("Get contact details.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/crm/v3/objects/contacts/{contactId}"

            ))
        .properties(string("contactId").label("Contact")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false),
                    object("properties")
                        .properties(string("firstname").required(false), string("lastname").required(false),
                            string("email").required(false), string("phone").required(false),
                            string("company").required(false), string("website").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private HubspotGetContactAction() {
    }
}
