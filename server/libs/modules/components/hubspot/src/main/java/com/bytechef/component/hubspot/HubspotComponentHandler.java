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

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.hubspot.constant.HubspotConstants.HAPIKEY;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.hubspot.trigger.HubspotNewContactTrigger;
import com.bytechef.component.hubspot.trigger.HubspotNewDealTrigger;
import com.bytechef.component.hubspot.trigger.HubspotNewTicketTrigger;
import com.bytechef.component.hubspot.unified.HubspotUnifiedApi;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        Optional<List<? extends Authorization>> optionalAuthorizations =
            modifiableConnectionDefinition.getAuthorizations();

        if (optionalAuthorizations.isPresent()) {
            List<? extends Authorization> authorizations = optionalAuthorizations.get();
            ModifiableAuthorization modifiableAuthorization = (ModifiableAuthorization) authorizations.getFirst();

            Optional<List<? extends Property>> optionalProperties = modifiableAuthorization.getProperties();
            List<Property> properties = new ArrayList<>(optionalProperties.orElse(List.of()));

            properties.addLast(
                string(HAPIKEY)
                    .label("Hubspot API Key")
                    .description("API Key is used for registering webhooks.")
                    .required(false));

            modifiableAuthorization.properties(properties);
        }

        return modifiableConnectionDefinition;
    }
}
