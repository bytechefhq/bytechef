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

package com.bytechef.component.productboard;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.productboard.action.ProductboardListNotesAction;
import com.bytechef.component.productboard.trigger.ProductboardNewNoteTrigger;
import com.bytechef.component.productboard.trigger.ProductboardUpdatedFeatureTrigger;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class ProductboardComponentHandler extends AbstractProductboardComponentHandler {

    @Override
    public List<ModifiableActionDefinition> getCustomActions() {
        return List.of(ProductboardListNotesAction.ACTION_DEFINITION);
    }

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            ProductboardNewNoteTrigger.TRIGGER_DEFINITION,
            ProductboardUpdatedFeatureTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/productboard.svg")
            .categories(ComponentCategory.PROJECT_MANAGEMENT);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        Optional<List<? extends Authorization>> optionalAuthorizations =
            modifiableConnectionDefinition.getAuthorizations();

        if (optionalAuthorizations.isPresent()) {
            List<? extends Authorization> authorizations = optionalAuthorizations.get();

            for (Authorization authorization : authorizations) {
                ModifiableAuthorization modifiableAuthorization = (ModifiableAuthorization) authorization;

                AuthorizationType type = modifiableAuthorization.getType();
                if (type.equals(AuthorizationType.BEARER_TOKEN)) {
                    modifiableAuthorization.apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                        Map.of("X-Version", List.of("1"),
                            "Authorization", List.of("Bearer " + connectionParameters.getRequiredString("token")))));
                } else {
                    modifiableAuthorization.apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                        Map.of("X-Version", List.of("1"),
                            "Authorization",
                            List.of("Bearer " + connectionParameters.getRequiredString("access_token")))));
                }
            }
        }

        return modifiableConnectionDefinition;
    }
}
