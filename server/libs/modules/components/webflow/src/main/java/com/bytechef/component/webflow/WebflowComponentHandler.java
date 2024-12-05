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

package com.bytechef.component.webflow;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.webflow.constant.WebflowConstants.COLLECTION_ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.ORDER_ID;
import static com.bytechef.component.webflow.constant.WebflowConstants.SITE_ID;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property;
import com.bytechef.component.webflow.util.WebflowUtils;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class WebflowComponentHandler extends AbstractWebflowComponentHandler {

    @Override
    public List<? extends ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {

        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            if (Objects.equals(modifiableActionDefinition.getName(), "getCollectionItem")) {
                Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();

                List<Property> properties = new ArrayList<>(propertiesOptional.get());

                properties.addFirst(
                    string(SITE_ID)
                        .label("Site")
                        .options((ActionOptionsFunction<String>) WebflowUtils::getSiteOptions)
                        .required(true));

                modifiableActionDefinition.properties(properties);
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/webflow.svg")
            .categories(ComponentCategory.DEVELOPER_TOOLS);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), SITE_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) WebflowUtils::getSiteOptions);
        } else if (Objects.equals(modifiableProperty.getName(), ORDER_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) WebflowUtils::getOrderOptions)
                .optionsLookupDependsOn(SITE_ID);
        } else if (Objects.equals(modifiableProperty.getName(), COLLECTION_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) WebflowUtils::getCollectionOptions)
                .optionsLookupDependsOn(SITE_ID);
        } else if (Objects.equals(modifiableProperty.getName(), "itemId")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) WebflowUtils::getCollectionItemOptions)
                .optionsLookupDependsOn(COLLECTION_ID, SITE_ID);
        }

        return modifiableProperty;
    }
}
