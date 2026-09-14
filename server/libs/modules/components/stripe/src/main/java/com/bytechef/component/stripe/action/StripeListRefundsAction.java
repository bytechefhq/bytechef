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

package com.bytechef.component.stripe.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.stripe.constant.StripeConstants.LIMIT;
import static com.bytechef.component.stripe.constant.StripeConstants.PAYMENT_INTENT_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.REFUND_OUTPUT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class StripeListRefundsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listRefunds")
        .title("List Refunds")
        .description("Lists refunds, most recent first, optionally only those for one payment intent.")
        .help("", "https://docs.bytechef.io/reference/components/stripe_v1#list-refunds")
        .properties(
            string(PAYMENT_INTENT_ID)
                .label("Payment Intent ID")
                .description("Only return refunds for the payment intent with this ID.")
                .required(false),
            integer(LIMIT)
                .label("Limit")
                .description("Maximum number of refunds to return, between 1 and 100. Defaults to 10.")
                .minValue(1)
                .maxValue(100)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("object")
                            .description("String representing the object's type, always `list`."),
                        array("data")
                            .description("The refunds, most recent first.")
                            .items(REFUND_OUTPUT),
                        bool("has_more")
                            .description("True if there are more refunds after this page."),
                        string("url")
                            .description("The URL where this list can be accessed."))))
        .perform(StripeListRefundsAction::perform);

    private StripeListRefundsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, List<String>> queryParameters = new HashMap<>();

        String paymentIntentId = inputParameters.getString(PAYMENT_INTENT_ID);

        if (paymentIntentId != null) {
            queryParameters.put(PAYMENT_INTENT_ID, List.of(paymentIntentId));
        }

        Integer limit = inputParameters.getInteger(LIMIT);

        if (limit != null) {
            queryParameters.put(LIMIT, List.of(String.valueOf(limit)));
        }

        return context.http(http -> http.get("/refunds"))
            .queryParameters(queryParameters)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
