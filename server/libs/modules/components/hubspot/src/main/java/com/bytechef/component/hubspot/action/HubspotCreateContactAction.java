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
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.hubspot.property.HubspotSimplePublicObjectInputForCreateProperties;
import com.bytechef.component.hubspot.property.HubspotSimplePublicObjectProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class HubspotCreateContactAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create")
        .description(
            "Create a contact with the given properties and return a copy of the object, including the ID. Documentation and examples for creating standard contacts is provided.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/crm/v3/objects/contacts", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(object("simplePublicObjectInputForCreate")
            .properties(HubspotSimplePublicObjectInputForCreateProperties.PROPERTIES)
            .label("Simple Public Object Input For Create")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(HubspotSimplePublicObjectProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
