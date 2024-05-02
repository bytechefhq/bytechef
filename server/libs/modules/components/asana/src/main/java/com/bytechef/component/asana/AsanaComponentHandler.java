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

package com.bytechef.component.asana;

import static com.bytechef.component.asana.constant.AsanaConstants.ASSIGNEE;
import static com.bytechef.component.asana.constant.AsanaConstants.PROJECT;
import static com.bytechef.component.asana.constant.AsanaConstants.TAGS;
import static com.bytechef.component.asana.constant.AsanaConstants.TEAM;
import static com.bytechef.component.asana.constant.AsanaConstants.WORKSPACE;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.asana.util.AsanaUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class AsanaComponentHandler extends AbstractAsanaComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/asana.svg");
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "data")) {
                    Optional<List<? extends ValueProperty<?>>> propertiesOptional2 =
                        ((ModifiableObjectProperty) baseProperty).getProperties();

                    for (BaseProperty baseProperty2 : propertiesOptional2.get()) {
                        if (Objects.equals(baseProperty2.getName(), WORKSPACE)) {
                            ((ModifiableStringProperty) baseProperty2)
                                .options((ActionOptionsFunction<String>) AsanaUtils::getWorkspaceIdOptions);
                        } else if (Objects.equals(baseProperty2.getName(), PROJECT)) {
                            ((ModifiableStringProperty) baseProperty2)
                                .optionsLookupDependsOn(WORKSPACE)
                                .options((ActionOptionsFunction<String>) AsanaUtils::getProjectIdOptions);
                        } else if (Objects.equals(baseProperty2.getName(), ASSIGNEE)) {
                            ((ModifiableStringProperty) baseProperty2)
                                .optionsLookupDependsOn(WORKSPACE)
                                .options((ActionOptionsFunction<String>) AsanaUtils::getAssigneeOptions);
                        } else if (Objects.equals(baseProperty2.getName(), TEAM)) {
                            ((ModifiableStringProperty) baseProperty2)
                                .optionsLookupDependsOn("__item.data." + WORKSPACE)
                                .options((ActionOptionsFunction<String>) AsanaUtils::getTeamOptions);
                        } else if (Objects.equals(baseProperty2.getName(), TAGS)) {
                            ((ModifiableArrayProperty) baseProperty2)
                                .items(
                                    string()
                                        .options((ActionOptionsFunction<String>) AsanaUtils::getTagOptions));
                        }
                    }
                }
            }
        }

        return modifiableProperty;
    }
}
