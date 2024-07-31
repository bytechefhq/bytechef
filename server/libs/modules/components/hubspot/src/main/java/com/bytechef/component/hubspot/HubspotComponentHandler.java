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

package com.bytechef.component.hubspot;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.hubspot.trigger.HubspotSubscribeTrigger;
import com.bytechef.component.hubspot.util.HubspotUtils;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class HubspotComponentHandler extends AbstractHubspotComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(HubspotSubscribeTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/hubspot.svg")
            .categories(ComponentCategory.MARKETING_AUTOMATION);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "contactId")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) HubspotUtils::getContactsOptions);
        } else if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {

                if (Objects.equals(baseProperty.getName(), "properties")) {
                    Optional<List<? extends ValueProperty<?>>> propertiesOptional1 =
                        ((ModifiableObjectProperty) baseProperty).getProperties();

                    for (BaseProperty baseProperty1 : propertiesOptional1.get()) {
                        if (Objects.equals(baseProperty1.getName(), "pipeline")) {
                            ((ModifiableStringProperty) baseProperty1)
                                .options((ActionOptionsFunction<String>) HubspotUtils::getPipelineDealOptions);
                        } else if (Objects.equals(baseProperty1.getName(), "dealstage")) {
                            ((ModifiableStringProperty) baseProperty1)
                                .options((ActionOptionsFunction<String>) HubspotUtils::getDealStageOptions)
                                .optionsLookupDependsOn("pipeline");
                        } else if (Objects.equals(baseProperty1.getName(), "hubspot_owner_id")) {
                            ((ModifiableStringProperty) baseProperty1)
                                .options((ActionOptionsFunction<String>) HubspotUtils::getOwnerOptions);
                        }
                    }
                }
            }
        }

        return modifiableProperty;
    }
}
