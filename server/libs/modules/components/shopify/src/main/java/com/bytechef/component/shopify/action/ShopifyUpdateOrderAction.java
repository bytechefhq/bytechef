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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.EMAIL;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.INPUT;
import static com.bytechef.component.shopify.constant.ShopifyConstants.NOTE;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.TAGS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.USER_ERRORS_PROPERTY;
import static com.bytechef.component.shopify.util.ShopifyUtils.executeGraphQlOperation;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyOptionsUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class ShopifyUpdateOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateOrder")
        .title("Update Order")
        .description("Update an existing order.")
        .properties(
            string(ORDER_ID)
                .label("Order ID")
                .description("ID of the order to update.")
                .required(true)
                .options((OptionsFunction<String>) ShopifyOptionsUtils::getOrderIdOptions),
            string(NOTE)
                .label("Note")
                .description("An optional note that a shop owner can attach to the order.")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("The customer's email address.")
                .required(false),
            array(TAGS)
                .label("Tags")
                .description("Tags attached to the order.")
                .items(
                    string("tag")
                        .label("Tag")
                        .description("Tag of the order.")
                        .required(false))
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("order")
                            .description("The updated order.")
                            .properties(
                                string("id")
                                    .description("ID of the updated order."),
                                string("note")
                                    .description("Note attached to order."),
                                string("email")
                                    .description("Customer email."),
                                array("tags")
                                    .description("Tags attached to the order.")
                                    .items(
                                        string()
                                            .description("Tag of the order."))),
                        USER_ERRORS_PROPERTY)))
        .perform(ShopifyUpdateOrderAction::perform);

    private ShopifyUpdateOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = """
            mutation OrderUpdate($input: OrderInput!) {
              orderUpdate(input: $input) {
                order {
                  id
                  note
                  email
                  tags
                }
                userErrors {
                  field
                  message
                }
              }
            }""";

        Map<String, Object> variables = Map.of(
            INPUT, Map.of(
                ID, inputParameters.getRequiredString(ORDER_ID),
                NOTE, inputParameters.getString(NOTE, ""),
                EMAIL, inputParameters.getString(EMAIL, ""),
                TAGS, inputParameters.getList(TAGS, List.of())));

        return executeGraphQlOperation(query, context, variables, "orderUpdate");
    }
}
