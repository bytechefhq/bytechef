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

package com.bytechef.component.stripe;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.stripe.trigger.StripeNewCustomerTrigger;
import com.bytechef.component.stripe.trigger.StripeNewInvoiceTrigger;
import com.bytechef.component.stripe.util.StripeUtils;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "customer")) {
                    ((ModifiableStringProperty) baseProperty)
                        .options((ActionOptionsFunction<String>) StripeUtils::getCustomerOptions);
                }
            }
        }

        return modifiableProperty;
    }

}
