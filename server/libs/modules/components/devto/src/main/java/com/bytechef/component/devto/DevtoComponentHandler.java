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

package com.bytechef.component.devto;

import static com.bytechef.component.definition.Authorization.VALUE;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class DevtoComponentHandler extends AbstractDevtoComponentHandler {

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            switch (name) {
                case "createArticle" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/devto_v1#create-article");
                case "getArticle" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/devto_v1#get-article");
                case "updateArticle" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/devto_v1#update-article");
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
            .customActionHelp("Dev.to API documentation", "https://developers.forem.com/api")
            .icon("path:assets/devto.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
            .version(1);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        Optional<List<? extends Authorization>> optionalAuthorizations =
            modifiableConnectionDefinition.getAuthorizations();

        if (optionalAuthorizations.isPresent()) {
            List<? extends Authorization> authorizations = optionalAuthorizations.get();
            ModifiableAuthorization modifiableAuthorization = (ModifiableAuthorization) authorizations.getFirst();

            modifiableAuthorization.apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                Map.of(
                    "accept", List.of("application/vnd.forem.api-v1+json"),
                    "api-key", List.of(connectionParameters.getRequiredString(VALUE)),
                    "User-Agent", List.of("ByteChef"))));
        }

        return modifiableConnectionDefinition;
    }
}
