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

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.shopify.action.ShopifyCancelOrderAction;
import com.bytechef.component.shopify.action.ShopifyCloseOrderAction;
import com.bytechef.component.shopify.action.ShopifyCreateOrderAction;
import com.bytechef.component.shopify.action.ShopifyCreateProductAction;
import com.bytechef.component.shopify.action.ShopifyDeleteOrderAction;
import com.bytechef.component.shopify.action.ShopifyGetAbandonedCartsAction;
import com.bytechef.component.shopify.action.ShopifyGetOrderAction;
import com.bytechef.component.shopify.action.ShopifyUpdateOrderAction;
import com.bytechef.component.shopify.connection.ShopifyConnection;
import com.bytechef.component.shopify.trigger.ShopifyNewCancelledOrderTrigger;
import com.bytechef.component.shopify.trigger.ShopifyNewOrderTrigger;
import com.bytechef.component.shopify.trigger.ShopifyNewPaidOrderTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class ShopifyComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("shopify")
        .title("Shopify")
        .description(
            "Shopify is an e-commerce platform that allows businesses to create online stores and sell products.")
        .icon("path:assets/shopify.svg")
        .categories(ComponentCategory.E_COMMERCE)
        .connection(ShopifyConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("Shopify API docs", "https://shopify.dev/docs/api/admin-graphql/latest")
        .actions(
            ShopifyCancelOrderAction.ACTION_DEFINITION,
            ShopifyCloseOrderAction.ACTION_DEFINITION,
            ShopifyCreateOrderAction.ACTION_DEFINITION,
            ShopifyCreateProductAction.ACTION_DEFINITION,
            ShopifyDeleteOrderAction.ACTION_DEFINITION,
            ShopifyGetAbandonedCartsAction.ACTION_DEFINITION,
            ShopifyGetOrderAction.ACTION_DEFINITION,
            ShopifyUpdateOrderAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ShopifyCancelOrderAction.ACTION_DEFINITION),
            tool(ShopifyCloseOrderAction.ACTION_DEFINITION),
            tool(ShopifyCreateOrderAction.ACTION_DEFINITION),
            tool(ShopifyCreateProductAction.ACTION_DEFINITION),
            tool(ShopifyDeleteOrderAction.ACTION_DEFINITION),
            tool(ShopifyGetAbandonedCartsAction.ACTION_DEFINITION),
            tool(ShopifyGetOrderAction.ACTION_DEFINITION),
            tool(ShopifyUpdateOrderAction.ACTION_DEFINITION))
        .triggers(
            ShopifyNewCancelledOrderTrigger.TRIGGER_DEFINITION,
            ShopifyNewOrderTrigger.TRIGGER_DEFINITION,
            ShopifyNewPaidOrderTrigger.TRIGGER_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
