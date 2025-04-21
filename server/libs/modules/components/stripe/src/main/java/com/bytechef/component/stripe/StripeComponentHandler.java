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

package com.bytechef.component.stripe;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.stripe.trigger.StripeNewCustomerTrigger;
import com.bytechef.component.stripe.trigger.StripeNewInvoiceTrigger;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class StripeComponentHandler extends AbstractStripeComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(StripeNewCustomerTrigger.TRIGGER_DEFINITION, StripeNewInvoiceTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/stripe.svg")
            .categories(ComponentCategory.PAYMENT_PROCESSING);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://api.stripe.com/v1")
            .authorizations(
                authorization(AuthorizationType.BEARER_TOKEN)
                    .title("Bearer Token")
                    .properties(
                        string(TOKEN)
                            .label("Token")
                            .required(true))
                    .apply((connectionParameters, context) -> ApplyResponse
                        .ofHeaders(
                            Map.of(
                                "Content-Type", List.of("application/x-www-form-urlencoded"),
                                AUTHORIZATION, List.of("Bearer " + connectionParameters.getRequiredString(TOKEN))))));
    }
}
