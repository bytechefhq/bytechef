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

package com.bytechef.component.hubspot;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.hubspot.constant.HubspotConstants.HAPIKEY;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.hubspot.trigger.HubspotNewContactTrigger;
import com.bytechef.component.hubspot.trigger.HubspotNewDealTrigger;
import com.bytechef.component.hubspot.trigger.HubspotNewTicketTrigger;
import com.bytechef.component.hubspot.unified.HubspotUnifiedApi;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class HubspotComponentHandler extends AbstractHubspotComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            HubspotNewContactTrigger.TRIGGER_DEFINITION, HubspotNewDealTrigger.TRIGGER_DEFINITION,
            HubspotNewTicketTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/hubspot.svg")
            .categories(ComponentCategory.MARKETING_AUTOMATION)
            .unifiedApi(HubspotUnifiedApi.UNIFIED_API_DEFINITION);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://api.hubapi.com")
            .authorizations(authorization(Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(HAPIKEY)
                        .label("Hubspot API Key")
                        .description("API Key is used for registering webhooks.")
                        .required(false))
                .authorizationUrl((connectionParameters, context) -> "https://app.hubspot.com/oauth/authorize")
                .scopes((connection, context) -> List.of("crm.objects.contacts.read", "crm.objects.contacts.write",
                    "crm.objects.deals.read", "crm.objects.deals.write", "crm.schemas.deals.read",
                    "crm.objects.owners.read", "tickets"))
                .tokenUrl((connectionParameters, context) -> "https://api.hubapi.com/oauth/v1/token")
                .refreshUrl((connectionParameters, context) -> "https://api.hubapi.com/oauth/v1/token"));
    }
}
