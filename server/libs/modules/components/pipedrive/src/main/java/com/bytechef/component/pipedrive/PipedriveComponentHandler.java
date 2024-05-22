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

package com.bytechef.component.pipedrive;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.pipedrive.trigger.PipedriveNewActivityTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewDealTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewOrganizationTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewPersonTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedDealTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedOrganizationTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedPersonTrigger;
import com.bytechef.component.pipedrive.util.PipedriveUtils;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;

/**
 * @generated
 * @author Ivica Cardic
 */
@AutoService(OpenApiComponentHandler.class)
public class PipedriveComponentHandler extends AbstractPipedriveComponentHandler {

    @Override
    public ComponentDSL.ModifiableComponentDefinition modifyComponent(
        ComponentDSL.ModifiableComponentDefinition modifiableComponentDefinition) {

        return modifiableComponentDefinition
            .description("The first CRM designed by salespeople, for salespeople. Do more to grow your business.")
            .customAction(true)
            .icon("path:assets/pipedrive.svg")
            .categories(ComponentCategory.CRM);
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

        if (Objects.equals(modifiableProperty.getName(), "owner_id")
            || Objects.equals(modifiableProperty.getName(), "user_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/users", null));
        } else if (Objects.equals(modifiableProperty.getName(), "org_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/organizations", null));
        } else if (Objects.equals(modifiableProperty.getName(), "person_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/persons", null));
        } else if (Objects.equals(modifiableProperty.getName(), "pipeline_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/pipelines", null));
        } else if (Objects.equals(modifiableProperty.getName(), "stage_id")) {
            ((ModifiableIntegerProperty) modifiableProperty)
                .options(PipedriveUtils.getOptions("/stages", "pipeline_id"));
        } else if (Objects.equals(modifiableProperty.getName(), "currency")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/currencies", null));
        } else if (Objects.equals(modifiableProperty.getName(), "filter_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/filters", null));
        }

        return modifiableProperty;
    }
}
