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

package com.bytechef.component.shopify;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.shopify.action.ShopifyCancelOrderAction;
import com.bytechef.component.shopify.action.ShopifyCloseOrderAction;
import com.bytechef.component.shopify.action.ShopifyCreateOrderAction;
import com.bytechef.component.shopify.action.ShopifyDeleteOrderAction;
import com.bytechef.component.shopify.action.ShopifyUpdateOrderAction;
import com.bytechef.component.shopify.connection.ShopifyConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractShopifyComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("shopify")
            .title("Shopify")
            .description(
                "Shopify is an e-commerce platform that allows businesses to create online stores and sell products."))
                    .actions(modifyActions(ShopifyCreateOrderAction.ACTION_DEFINITION,
                        ShopifyDeleteOrderAction.ACTION_DEFINITION, ShopifyCancelOrderAction.ACTION_DEFINITION,
                        ShopifyUpdateOrderAction.ACTION_DEFINITION, ShopifyCloseOrderAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ShopifyConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
