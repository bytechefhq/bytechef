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
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.number;
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
public class HubspotCreateDealAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createDeal")
        .title("Create Deal")
        .description("Creates a new deal.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/crm/v3/objects/deals", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(object("properties").properties(string("dealname").label("Deal   Name")
            .required(false),
            number("amount").label("Amount")
                .required(false),
            date("closedate").label("Closedate")
                .required(false),
            string("pipeline").label("Pipeline")
                .required(false),
            string("dealstage").label("Deal   Stage")
                .required(false),
            string("hubspot_owner_id").label("Deal   Owner")
                .required(false))
            .label("Properties")
            .required(false))
            .label("Deal")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false),
                    object("properties")
                        .properties(string("dealname").required(false), number("amount").required(false),
                            date("closedate").required(false), string("pipeline").required(false),
                            string("dealstage").required(false), string("hubspot_owner_id").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private HubspotCreateDealAction() {
    }
}
