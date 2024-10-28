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

package com.bytechef.component.accelo;

import static com.bytechef.component.accelo.constant.AcceloConstants.DEPLOYMENT;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.accelo.action.AcceloCreateTaskAction;
import com.bytechef.component.accelo.util.AcceloUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class AcceloComponentHandler extends AbstractAcceloComponentHandler {

    @Override
    public List<? extends ModifiableActionDefinition> getCustomActions() {
        return List.of(AcceloCreateTaskAction.ACTION_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/accelo.svg")
            .categories(ComponentCategory.CRM, ComponentCategory.PROJECT_MANAGEMENT);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(DEPLOYMENT)
                + ".api.accelo.com/api/v0")
            .authorizations(
                authorization(Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                    .title("OAuth2 Authorization Code")
                    .properties(
                        string(DEPLOYMENT)
                            .label("Deployment")
                            .description(
                                "Actual deployment identifier or name to target a specific deployment within the " +
                                    "Accelo platform.")
                            .required(true),
                        string(CLIENT_ID)
                            .label("Client Id")
                            .required(true),
                        string(CLIENT_SECRET)
                            .label("Client Secret")
                            .required(true))
                    .authorizationUrl((connection, context) -> "https://" + connection.getRequiredString(DEPLOYMENT) +
                        ".api.accelo.com/oauth2/v0/authorize")
                    .scopes((connection, context) -> List.of("write(all)"))
                    .tokenUrl((connection, context) -> "https://" + connection.getRequiredString(DEPLOYMENT) +
                        ".api.accelo.com/oauth2/v0/token"));
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends Property.ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "company_id")) {
                    ((ModifiableStringProperty) baseProperty)
                        .options((ActionOptionsFunction<String>) AcceloUtils::getCompanyIdOptions);
                }
            }
        }

        return modifiableProperty;
    }
}
