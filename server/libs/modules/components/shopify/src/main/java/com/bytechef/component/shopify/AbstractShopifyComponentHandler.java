/*
 * Copyright 2021 <your company/name>.
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

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.shopify.action.ProductsActions;
import com.bytechef.hermes.component.RestComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

public abstract class AbstractShopifyComponentHandler implements RestComponentHandler {
    private final ComponentDefinition componentDefinition = component("shopify")
            .display(display("Shopify")
                    .description(
                            "The REST Admin API lets you build apps and other integrations for the Shopify admin."))
            .actions(ProductsActions.ACTIONS);

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
