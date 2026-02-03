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

package com.bytechef.component.accelo;

import static com.bytechef.component.accelo.constant.AcceloConstants.DEPLOYMENT;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.accelo.action.AcceloCreateTaskAction;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class AcceloComponentHandler extends AbstractAcceloComponentHandler {

    @Override
    public List<ModifiableActionDefinition> getCustomActions() {
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

        Optional<List<? extends Authorization>> optionalAuthorizations =
            modifiableConnectionDefinition.getAuthorizations();

        if (optionalAuthorizations.isPresent()) {
            List<? extends Authorization> authorizations = optionalAuthorizations.get();
            ModifiableAuthorization modifiableAuthorization = (ModifiableAuthorization) authorizations.getFirst();

            Optional<List<? extends Property>> optionalProperties = modifiableAuthorization.getProperties();
            List<Property> properties = new ArrayList<>(optionalProperties.orElse(List.of()));

            properties.addFirst(
                string(DEPLOYMENT)
                    .label("Deployment")
                    .description(
                        "Actual deployment identifier or name to target a specific deployment within the " +
                            "Accelo platform.")
                    .required(true));

            modifiableAuthorization.properties(properties);
            modifiableAuthorization.authorizationUrl(
                (connection, context) -> "https://" + connection.getRequiredString(DEPLOYMENT) +
                    ".api.accelo.com/oauth2/v0/authorize");
            modifiableAuthorization.scopes((connection, context) -> Map.of("write(all)", true));
            modifiableAuthorization
                .tokenUrl((connection, context) -> "https://" + connection.getRequiredString(DEPLOYMENT) +
                    ".api.accelo.com/oauth2/v0/token");
        }

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(DEPLOYMENT)
                + ".api.accelo.com/api/v0");
    }
}
