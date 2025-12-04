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

package com.bytechef.component.shopify.action;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

public class ShopifyGetOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getOrder")
        .title("Get Order")
        .description("Get order by id.")
        .properties()
        .output(outputSchema(string()))
        .perform(ShopifyGetOrderAction::perform);

    private ShopifyGetOrderAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        // TODO

        return null;
    }
}
