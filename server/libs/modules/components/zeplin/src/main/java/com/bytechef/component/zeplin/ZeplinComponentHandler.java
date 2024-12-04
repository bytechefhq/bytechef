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

package com.bytechef.component.zeplin;

import static com.bytechef.component.zeplin.constant.ZeplinConstants.PROJECT_ID;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.zeplin.trigger.ZeplinProjectNoteTrigger;
import com.bytechef.component.zeplin.util.ZeplinUtils;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class ZeplinComponentHandler extends AbstractZeplinComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(ZeplinProjectNoteTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/zeplin.svg")
            .categories(ComponentCategory.COMMUNICATION);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), PROJECT_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) ZeplinUtils::getProjectIdOptions);
        }

        return modifiableProperty;
    }
}
