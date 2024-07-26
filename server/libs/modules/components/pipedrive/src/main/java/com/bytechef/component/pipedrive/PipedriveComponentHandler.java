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

import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.ID;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.pipedrive.trigger.PipedriveNewActivityTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewDealTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewOrganizationTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveNewPersonTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedDealTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedOrganizationTrigger;
import com.bytechef.component.pipedrive.trigger.PipedriveUpdatedPersonTrigger;
import com.bytechef.component.pipedrive.util.PipedriveUtils;
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

        if (Objects.equals(modifiableProperty.getName(), ID)) {
            if (Objects.equals(actionDefinition.getName(), "deletePerson")
                || Objects.equals(actionDefinition.getName(), "getPersonDetails")) {
                ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/persons", null));
            } else if (Objects.equals(actionDefinition.getName(), "deleteOrganization")
                || Objects.equals(actionDefinition.getName(), "getOrganizationDetails")) {
                ((ModifiableIntegerProperty) modifiableProperty)
                    .options(PipedriveUtils.getOptions("/organizations", null));
            } else if (Objects.equals(actionDefinition.getName(), "deleteDeal")
                || Objects.equals(actionDefinition.getName(), "getDealDetails")) {
                ((ModifiableIntegerProperty) modifiableProperty)
                    .options(PipedriveUtils.getOptions("/deals", null));
            } else if (Objects.equals(actionDefinition.getName(), "deleteLead")
                || Objects.equals(actionDefinition.getName(), "getLeadDetails")) {
                ((ModifiableStringProperty) modifiableProperty).options(PipedriveUtils.getOptions("/leads", null));
            }
        } else if (Objects.equals(modifiableProperty.getName(), "owner_id")
            || Objects.equals(modifiableProperty.getName(), "user_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/users", null));
        } else if (Objects.equals(modifiableProperty.getName(), "filter_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/filters", null));
        } else if (Objects.equals(modifiableProperty.getName(), "stage_id")) {
            ((ModifiableIntegerProperty) modifiableProperty)
                .options(PipedriveUtils.getOptions("/stages", null));
        } else if (Objects.equals(modifiableProperty.getName(), "person_id")) {
            ((ModifiableIntegerProperty) modifiableProperty).options(PipedriveUtils.getOptions("/persons", null));
        } else if (Objects.equals(modifiableProperty.getName(), "org_id")
            || Objects.equals(modifiableProperty.getName(), "organization_id")) {
            ((ModifiableIntegerProperty) modifiableProperty)
                .options(PipedriveUtils.getOptions("/organizations", null));
        } else if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "owner_id")
                    || Objects.equals(baseProperty.getName(), "user_id")) {
                    ((ModifiableIntegerProperty) baseProperty).options(PipedriveUtils.getOptions("/users", null));
                } else if (Objects.equals(baseProperty.getName(), "org_id")
                    || Objects.equals(modifiableProperty.getName(), "organization_id")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .options(PipedriveUtils.getOptions("/organizations", null));
                } else if (Objects.equals(baseProperty.getName(), "person_id")) {
                    ((ModifiableIntegerProperty) baseProperty).options(PipedriveUtils.getOptions("/persons", null));
                } else if (Objects.equals(baseProperty.getName(), "pipeline_id")) {
                    ((ModifiableIntegerProperty) baseProperty).options(PipedriveUtils.getOptions("/pipelines", null));
                } else if (Objects.equals(baseProperty.getName(), "stage_id")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .options(PipedriveUtils.getOptions("/stages", "pipeline_id"));
                } else if (Objects.equals(baseProperty.getName(), "currency")) {
                    ((ModifiableStringProperty) baseProperty).options(PipedriveUtils.getOptions("/currencies", null));
                } else if (Objects.equals(baseProperty.getName(), "lost_reason")) {
                    ((ModifiableStringProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("status", "lost"));
                } else if (Objects.equals(baseProperty.getName(), "label_ids")) {
                    ((ModifiableArrayProperty) baseProperty)
                        .items(string()
                            .options(PipedriveUtils.getOptions("/leadLabels", null)));
                } else if (Objects.equals(baseProperty.getName(), "value")
                    && Objects.equals(actionDefinition.getName(), "addLead")) {
                    Optional<List<? extends ValueProperty<?>>> propertiesOptional1 =
                        ((ModifiableObjectProperty) baseProperty).getProperties();

                    for (BaseProperty baseProperty1 : propertiesOptional1.get()) {
                        if (Objects.equals(baseProperty1.getName(), "currency")) {
                            ((ModifiableStringProperty) baseProperty1)
                                .options(PipedriveUtils.getOptions("/currencies", null));
                        }
                    }
                }
            }
        }

        return modifiableProperty;
    }
}
