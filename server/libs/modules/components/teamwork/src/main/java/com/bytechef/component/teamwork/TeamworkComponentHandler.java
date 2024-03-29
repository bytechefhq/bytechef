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

package com.bytechef.component.teamwork;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.teamwork.constant.TeamworkConstants.SITE_NAME;
import static com.bytechef.component.teamwork.util.TeamworkUtils.getBaseUrl;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.teamwork.util.TeamworkUtils;
import com.google.auto.service.AutoService;
import java.util.Objects;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class TeamworkComponentHandler extends AbstractTeamworkComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/teamwork.svg");
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.BASIC_AUTH.toLowerCase(), AuthorizationType.BASIC_AUTH)
                    .title("Basic Auth")
                    .properties(
                        string(SITE_NAME)
                            .label("Your site name")
                            .description("e.g. https://{yourSiteName}.teamwork.com")
                            .required(true),
                        string(USERNAME)
                            .label("API Key")
                            .required(true)))
            .baseUri((connectionParameters, context) -> getBaseUrl(connectionParameters));
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(actionDefinition.getName(), "createTask") &&
            Objects.equals(modifiableProperty.getName(), "tasklistId")) {

            ((ModifiableIntegerProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) TeamworkUtils::getTaskListIdOptions);
        }

        return modifiableProperty;
    }
}
