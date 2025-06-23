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

package com.bytechef.component.woocommerce;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.woocommerce.action.WoocommerceCreateCouponAction;
import com.bytechef.component.woocommerce.action.WoocommerceCreateCustomerAction;
import com.bytechef.component.woocommerce.action.WoocommerceCreateOrderAction;
import com.bytechef.component.woocommerce.action.WoocommerceCreateProductAction;
import com.bytechef.component.woocommerce.connection.WoocommerceConnection;
import com.bytechef.component.woocommerce.trigger.WoocommerceNewCouponTrigger;
import com.bytechef.component.woocommerce.trigger.WoocommerceNewOrderTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class WoocommerceComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("woocommerce")
        .title("WooCommerce")
        .description(
            "WooCommerce is a e-commerce plugin for WordPress that allows you to turn a standard WordPress website " +
                "into a fully functional online store.")
        .icon("path:assets/woocommerce.svg")
        .categories(ComponentCategory.E_COMMERCE)
        .customAction(true)
        .connection(WoocommerceConnection.CONNECTION_DEFINITION)
        .actions(
            WoocommerceCreateCouponAction.ACTION_DEFINITION,
            WoocommerceCreateCustomerAction.ACTION_DEFINITION,
            WoocommerceCreateOrderAction.ACTION_DEFINITION,
            WoocommerceCreateProductAction.ACTION_DEFINITION)
        .clusterElements(
            tool(WoocommerceCreateCouponAction.ACTION_DEFINITION),
            tool(WoocommerceCreateCustomerAction.ACTION_DEFINITION),
            tool(WoocommerceCreateOrderAction.ACTION_DEFINITION),
            tool(WoocommerceCreateProductAction.ACTION_DEFINITION))
        .triggers(
            WoocommerceNewOrderTrigger.TRIGGER_DEFINITION,
            WoocommerceNewCouponTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
