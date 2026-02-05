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

package com.bytechef.component.insightly;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.insightly.constant.InsightlyConstants.URL;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class InsightlyComponentHandler extends AbstractInsightlyComponentHandler {

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            switch (name) {
                case "createContact" ->
                    actionDefinition.help("",
                        "https://docs.bytechef.io/reference/components/insightly_v1/createContact");
                case "createOrganization" ->
                    actionDefinition.help("",
                        "https://docs.bytechef.io/reference/components/insightly_v1/createOrganization");
                case "createTask" ->
                    actionDefinition.help("", "https://docs.bytechef.io/reference/components/insightly_v1/createTask");

                default -> {
                }
            }
        }
        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .customActionHelp("", "https://api.na1.insightly.com/v3.1/#!/Overview/Introduction")
            .icon("path:assets/insightly.svg")
            .categories(List.of(ComponentCategory.CRM))
            .version(1);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.BASIC_AUTH)
                    .title("Basic Auth")
                    .properties(
                        string(URL)
                            .label("API URL")
                            .required(true),
                        string(USERNAME)
                            .label("API Key")
                            .required(true)))
            .baseUri((connectionParameters, context) -> {
                String url = connectionParameters.getRequiredString(URL);

                return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            })
            .help("", "https://docs.bytechef.io/reference/components/insightly_v1#connection-setup")
            .version(1);
    }
}
