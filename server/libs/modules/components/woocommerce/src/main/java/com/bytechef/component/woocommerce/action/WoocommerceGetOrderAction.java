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

package com.bytechef.component.woocommerce.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ORDER_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Ivica Cardic
 */
public class WoocommerceGetOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getOrder")
        .title("Get Order")
        .description("Get an order by its ID.")
        .properties(
            string(ID)
                .label("Order ID")
                .description("ID of the order to get.")
                .required(true))
        .output(outputSchema(ORDER_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/woocommerce_v1#get-order")
        .perform(WoocommerceGetOrderAction::perform);

    private WoocommerceGetOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String orderId = inputParameters.getRequiredString(ID);

        return context.http(http -> http.get("/orders/" + orderId))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
