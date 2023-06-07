
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.airtable;

import com.bytechef.component.airtable.trigger.NewRecordTrigger;
import com.bytechef.component.airtable.util.AirtableUtils;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableDynamicPropertiesProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableStringProperty;
import com.google.auto.service.AutoService;

import java.util.List;
import java.util.Objects;

import static com.bytechef.component.airtable.constant.AirtableConstants.BASE_ID;
import static com.bytechef.component.airtable.constant.AirtableConstants.TABLE_ID;

/**
 * @author Ivica Cardic
 */
@AutoService(OpenApiComponentHandler.class)
public class AirtableComponentHandler extends AbstractAirtableComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(NewRecordTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/airtable.svg");
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> property) {

        if (Objects.equals(property.getName(), "__item")) {
            property.label("Record");
        }

        if (Objects.equals(property.getName(), BASE_ID)) {
            ((ModifiableStringProperty) property).options(AirtableUtils.getBaseIdOptions());
        }

        if (Objects.equals(property.getName(), "fields")) {
            ((ModifiableDynamicPropertiesProperty) property).loadPropertiesDependsOn(BASE_ID, TABLE_ID);
            ((ModifiableDynamicPropertiesProperty) property).properties(AirtableUtils.getFieldsProperties());
        }

        if (Objects.equals(property.getName(), TABLE_ID)) {
            ((ModifiableStringProperty) property).loadOptionsDependsOn(BASE_ID);
            ((ModifiableStringProperty) property).options(AirtableUtils.getTableIdOptions());
        }

        return property;
    }
}
