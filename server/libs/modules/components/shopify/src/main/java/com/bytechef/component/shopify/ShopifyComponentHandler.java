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

package com.bytechef.component.shopify;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.SHOP_NAME;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.shopify.trigger.ShopifyNewCancelledOrderTrigger;
import com.bytechef.component.shopify.trigger.ShopifyNewOrderTrigger;
import com.bytechef.component.shopify.trigger.ShopifyNewPaidOrderTrigger;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class ShopifyComponentHandler extends AbstractShopifyComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            ShopifyNewCancelledOrderTrigger.TRIGGER_DEFINITION,
            ShopifyNewOrderTrigger.TRIGGER_DEFINITION,
            ShopifyNewPaidOrderTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/shopify.svg")
            .categories(ComponentCategory.E_COMMERCE);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.API_KEY)
                    .title("API Key")
                    .properties(
                        string(SHOP_NAME)
                            .label("Shop name")
                            .required(true),
                        string(KEY)
                            .label("Access token")
                            .required(true)
                            .defaultValue("X-Shopify-Access-Token")
                            .hidden(true),
                        string(VALUE)
                            .label("Access Token")
                            .required(true)))
            .baseUri((connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(SHOP_NAME)
                + ".myshopify.com/admin/api/2024-04");
    }
}
