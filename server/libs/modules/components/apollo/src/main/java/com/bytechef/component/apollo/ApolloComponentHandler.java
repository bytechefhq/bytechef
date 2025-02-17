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

package com.bytechef.component.apollo;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.apollo.util.ApolloUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.google.auto.service.AutoService;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class ApolloComponentHandler extends AbstractApolloComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/apollo.svg")
            .categories(ComponentCategory.CRM);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "opportunity_id")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(ApolloUtils.getOptions("/opportunities/search", "opportunities"));
        } else if (Objects.equals(modifiableProperty.getName(), "owner_id")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(ApolloUtils.getOptions("/users/search", "users"));
        } else if (Objects.equals(modifiableProperty.getName(), "account_id")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(ApolloUtils.getOptions("/mixed_companies/search", "organizations"));
        }

        return modifiableProperty;
    }
}
