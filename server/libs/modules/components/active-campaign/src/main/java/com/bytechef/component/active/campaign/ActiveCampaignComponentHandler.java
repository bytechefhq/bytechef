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

package com.bytechef.component.active.campaign;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.active.campaign.util.ActiveCampaignUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
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

        return modifiableConnectionDefinition
            .authorizations(
                authorization(
                    AuthorizationType.API_KEY.toLowerCase(), AuthorizationType.API_KEY)
                        .title("API Key")
                        .properties(
                            string(USERNAME)
                                .label("Account name")
                                .description("Your account name, e.g. https://{youraccountname}.api-us1.com")
                                .required(true),
                            string(KEY)
                                .label("Key")
                                .required(true)
                                .defaultValue("Api-Token")
                                .hidden(true),
                            string(VALUE)
                                .label("API Key")
                                .required(true)))
            .baseUri(
                (connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(USERNAME) +
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
                                (ActionOptionsFunction<String>) ActiveCampaignUtils::getContactIdOptions);
                        } else if (Objects.equals(baseProperty2.getName(), "dealTasktype")) {
                            ((ModifiableIntegerProperty) baseProperty2)
                                .options((ActionOptionsFunction<String>) ActiveCampaignUtils::getTaskTypeIdOptions);
                        }
                    }
                }
            }
        }
        return modifiableProperty;
    }
}
