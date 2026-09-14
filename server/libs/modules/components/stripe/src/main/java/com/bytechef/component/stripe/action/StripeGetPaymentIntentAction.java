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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.stripe.constant.StripeConstants.PAYMENT_INTENT_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.PAYMENT_INTENT_OUTPUT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Ivica Cardic
 */
public class StripeGetPaymentIntentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getPaymentIntent")
        .title("Get Payment Intent")
        .description("Retrieves a payment intent, including its amount, currency, status and customer.")
        .help("", "https://docs.bytechef.io/reference/components/stripe_v1#get-payment-intent")
        .properties(
            string(PAYMENT_INTENT_ID)
                .label("Payment Intent ID")
                .description("ID of the payment intent to retrieve, for example pi_3MtwBwLkdIwHu7ix28a3tqPa.")
                .required(true))
        .output(outputSchema(PAYMENT_INTENT_OUTPUT))
        .perform(StripeGetPaymentIntentAction::perform);

    private StripeGetPaymentIntentAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/payment_intents/" + inputParameters.getRequiredString(PAYMENT_INTENT_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
