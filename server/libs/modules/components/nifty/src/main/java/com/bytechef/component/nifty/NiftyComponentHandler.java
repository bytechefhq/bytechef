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

package com.bytechef.component.nifty;

import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT_PROPERTY;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.nifty.trigger.NiftyNewTaskTrigger;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class NiftyComponentHandler extends AbstractNiftyComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(NiftyNewTaskTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public List<? extends ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();
            List<Property> properties = new ArrayList<>(propertiesOptional.orElse(Collections.emptyList()));

            if (Objects.equals(modifiableActionDefinition.getName(), "createTask")) {
                properties.addFirst(PROJECT_PROPERTY);
            }
            modifiableActionDefinition.properties(properties);
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/nifty.svg")
            .categories(ComponentCategory.PROJECT_MANAGEMENT, ComponentCategory.PRODUCTIVITY_AND_COLLABORATION);
    }

}
