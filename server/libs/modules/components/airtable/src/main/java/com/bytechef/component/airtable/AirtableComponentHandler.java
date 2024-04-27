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

package com.bytechef.component.airtable;

import static com.bytechef.component.airtable.constant.AirtableConstants.BASE_ID;
import static com.bytechef.component.airtable.constant.AirtableConstants.TABLE_ID;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.airtable.trigger.AirtableNewRecordTrigger;
import com.bytechef.component.airtable.util.AirtableUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableDynamicPropertiesProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.DataStreamItemReader;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@AutoService(OpenApiComponentHandler.class)
public class AirtableComponentHandler extends AbstractAirtableComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(AirtableNewRecordTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/airtable.svg")
            .dataStreamItemReader(new DataStreamItemReader() {});
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), BASE_ID)) {
            ((ModifiableStringProperty) modifiableProperty).options(
                (ActionOptionsFunction<String>) (
                    inputParameters, connectionParameters, loadDependsOnPaths, searchText,
                    context) -> AirtableUtils.getBaseIdOptions(context));
        }

        if (Objects.equals(modifiableProperty.getName(), "__item")) {
            ((ModifiableDynamicPropertiesProperty) modifiableProperty)
                .loadPropertiesDependsOn(BASE_ID, TABLE_ID)
                .properties(AirtableUtils.getFieldsProperties());
        }

        if (Objects.equals(modifiableProperty.getName(), TABLE_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .loadOptionsDependsOn(BASE_ID)
                .options(
                    (ActionOptionsFunction<String>) (
                        inputParameters, connectionParameters, loadDependsOnPaths, searchText,
                        context) -> AirtableUtils.getTableIdOptions(inputParameters, context));
        }

        return modifiableProperty;
    }
}
