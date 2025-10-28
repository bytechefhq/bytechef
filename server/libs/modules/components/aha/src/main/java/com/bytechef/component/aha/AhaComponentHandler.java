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

package com.bytechef.component.aha;

import static com.bytechef.component.aha.constant.AhaConstants.PRODUCT_ID;
import static com.bytechef.component.aha.constant.AhaConstants.SUBDOMAIN;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.aha.util.AhaUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class AhaComponentHandler extends AbstractAhaComponentHandler {

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            if (Objects.equals(modifiableActionDefinition.getName(), "createFeature")) {
                Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();

                List<Property> properties = new ArrayList<>(propertiesOptional.orElse(List.of()));

                properties.addFirst(
                    string(PRODUCT_ID)
                        .label("Product ID")
                        .description("ID of the product to which the release belongs.")
                        .options((OptionsFunction<String>) AhaUtils::getProductIdOptions)
                        .required(true));

                modifiableActionDefinition.properties(properties);
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/aha.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://%s.aha.io/api/v1"
                .formatted(connectionParameters.getRequiredString(SUBDOMAIN)))
            .authorizations(authorization(Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(SUBDOMAIN)
                        .label("Subdomain")
                        .description(
                            "The subdomain of your Aha! account. For example, if your Aha! URL is " +
                                "https://mycompany.aha.io, then the subdomain is mycompany.")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://%s.aha.io/oauth/authorize"
                    .formatted(connectionParameters.getRequiredString(SUBDOMAIN)))
                .tokenUrl((connectionParameters, context) -> "https://%s.aha.io/oauth/token"
                    .formatted(connectionParameters.getRequiredString(SUBDOMAIN))));
    }
}
