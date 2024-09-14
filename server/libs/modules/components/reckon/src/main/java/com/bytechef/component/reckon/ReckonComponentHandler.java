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

package com.bytechef.component.reckon;

import static com.bytechef.component.reckon.constant.ReckonConstants.BOOK_ID;

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
import com.bytechef.component.reckon.trigger.ReckonNewInvoiceTrigger;
import com.bytechef.component.reckon.trigger.ReckonNewPaymentTrigger;
import com.bytechef.component.reckon.util.ReckonUtils;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class ReckonComponentHandler extends AbstractReckonComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            ReckonNewInvoiceTrigger.TRIGGER_DEFINITION,
            ReckonNewPaymentTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/reckon.svg")
            .categories(ComponentCategory.ACCOUNTING);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), BOOK_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) ReckonUtils::getBookIdOptions);
        } else if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "customer")) {
                    ((ModifiableStringProperty) baseProperty)
                        .options((ActionOptionsFunction<String>) ReckonUtils::getCustomerOptions);
                } else if (Objects.equals(baseProperty.getName(), "supplier")) {
                    ((ModifiableStringProperty) baseProperty)
                        .options((ActionOptionsFunction<String>) ReckonUtils::getSupplierOptions);
                }
            }
        }

        return modifiableProperty;
    }

}
