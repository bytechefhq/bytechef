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

package com.bytechef.component.hubspot.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.hubspot.util.HubspotUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class HubspotCreateDealAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createDeal")
        .title("Create Deal")
        .description("Creates a new deal.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/crm/v3/objects/deals", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("properties").properties(string("dealname").label("Deal Name")
            .required(false),
            number("amount").label("Amount")
                .required(false),
            date("closedate").label("Close Date")
                .required(false),
            string("pipeline").label("Pipeline")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) HubspotUtils::getPipelineOptions),
            string("dealstage").label("Deal Stage")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) HubspotUtils::getDealstageOptions)
                .optionsLookupDependsOn("properties.pipeline"),
            string("hubspot_owner_id").label("Deal Owner")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) HubspotUtils::getHubspotOwnerIdOptions))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Properties")
            .required(false))
        .output(outputSchema(object().properties(string("id").description("ID of the deal.")
            .required(false),
            object("properties").properties(string("dealname").description("The name of the deal.")
                .required(false),
                number("amount").description("The monetary value associated with the deal.")
                    .required(false),
                date("closedate").description("he date when the deal is expected to close.")
                    .required(false),
                string("pipeline")
                    .description("The sales pipeline to which the deal belongs, indicating its workflow path.")
                    .required(false),
                string("dealstage")
                    .description("The current stage of the deal within its pipeline, reflecting its progress.")
                    .required(false),
                string("hubspot_owner_id").description("ID for the HubSpot user responsible for managing the deal.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private HubspotCreateDealAction() {
    }
}
