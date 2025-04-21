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

package com.bytechef.component.teamwork;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.teamwork.constant.TeamworkConstants.SITE_NAME;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class TeamworkComponentHandler extends AbstractTeamworkComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/teamwork.svg")
            .categories(ComponentCategory.CRM, ComponentCategory.PROJECT_MANAGEMENT);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.BASIC_AUTH)
                    .title("Basic Auth")
                    .properties(
                        string(SITE_NAME)
                            .label("Your site name")
                            .description("e.g. https://{yourSiteName}.teamwork.com")
                            .required(true),
                        string(USERNAME)
                            .label("API Key")
                            .required(true)))
            .baseUri((connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(SITE_NAME)
                + ".teamwork.com/projects/api/v3");
    }
}
