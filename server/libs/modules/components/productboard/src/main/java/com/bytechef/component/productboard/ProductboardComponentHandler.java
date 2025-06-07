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

package com.bytechef.component.productboard;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.productboard.trigger.ProductboardNewNoteTrigger;
import com.bytechef.component.productboard.trigger.ProductboardUpdatedFeatureTrigger;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class ProductboardComponentHandler extends AbstractProductboardComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            ProductboardNewNoteTrigger.TRIGGER_DEFINITION,
            ProductboardUpdatedFeatureTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/productboard.svg")
            .categories(ComponentCategory.PROJECT_MANAGEMENT);
    }

    @Override
    public ComponentDsl.ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ComponentDsl.ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "X-Version")) {
            modifiableProperty.hidden(true);
        }

        return super.modifyProperty(actionDefinition, modifiableProperty);
    }
}
