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

package com.bytechef.component.active.campaign;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.active.campaign.util.ActiveCampaignUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class ActiveCampaignComponentHandler extends AbstractActiveCampaignComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/active-campaign.svg")
            .categories(ComponentCategory.CRM, ComponentCategory.MARKETING_AUTOMATION);
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

            properties.addFirst(
                string(USERNAME)
                    .label("Account name")
                    .description("Your account name, e.g. https://{youraccountname}.api-us1.com")
                    .required(true));

            modifiableAuthorization.properties(properties);
        }

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(USERNAME) +
                ".api-us1.com/api/3");
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(actionDefinition.getName(), "createTask")) {
            for (BaseProperty baseProperty : ((ModifiableObjectProperty) modifiableProperty).getProperties()
                .get()) {

                if (Objects.equals(baseProperty.getName(), "task")) {
                    Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                        ((ModifiableObjectProperty) baseProperty).getProperties();

                    for (BaseProperty baseProperty2 : propertiesOptional.get()) {
                        if (Objects.equals(baseProperty2.getName(), "relid")) {
                            ((ModifiableIntegerProperty) baseProperty2).options(
                                (OptionsFunction<String>) ActiveCampaignUtils::getContactIdOptions);
                        } else if (Objects.equals(baseProperty2.getName(), "dealTasktype")) {
                            ((ModifiableIntegerProperty) baseProperty2)
                                .options((OptionsFunction<String>) ActiveCampaignUtils::getTaskTypeIdOptions);
                        }
                    }
                }
            }
        }
        return modifiableProperty;
    }
}
