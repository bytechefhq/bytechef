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

package com.bytechef.component.pipedrive;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.pipedrive.trigger.PipedriveNewActivityTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewDealTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewOrganizationTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewPersonTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedDealTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedOrganizationTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedPersonTrigger;
import com.bytechef.component.pipedrive.unified.PipedriveUnifiedApi;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 * @generated
 */
@AutoService(OpenApiComponentHandler.class)
public class PipedriveComponentHandler extends AbstractPipedriveComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(
        ModifiableComponentDefinition modifiableComponentDefinition) {

        return modifiableComponentDefinition
            .description("The first CRM designed by salespeople, for salespeople. Do more to grow your business.")
            .customAction(true)
            .icon("path:assets/pipedrive.svg")
            .categories(ComponentCategory.CRM)
            .unifiedApi(PipedriveUnifiedApi.UNIFIED_API_DEFINITION);
    }

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            PipedriveNewActivityTrigger.TRIGGER_DEFINITION,
            PipedriveNewDealTrigger.TRIGGER_DEFINITION,
            PipedriveNewOrganizationTrigger.TRIGGER_DEFINITION,
            PipedriveNewPersonTrigger.TRIGGER_DEFINITION,
            PipedriveUpdatedDealTrigger.TRIGGER_DEFINITION,
            PipedriveUpdatedOrganizationTrigger.TRIGGER_DEFINITION,
            PipedriveUpdatedPersonTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "lost_reason")) {
                    ((ModifiableStringProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("status", "lost"));
                }
            }
        }

        return modifiableProperty;
    }
}
