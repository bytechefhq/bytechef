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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.stripe.constant.StripeConstants.AMOUNT;
import static com.bytechef.component.stripe.constant.StripeConstants.METADATA;
import static com.bytechef.component.stripe.constant.StripeConstants.PAYMENT_INTENT_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.REASON;
import static com.bytechef.component.stripe.constant.StripeConstants.REFUND_OUTPUT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class StripeCreateRefundAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRefund")
        .title("Create Refund")
        .description("Refunds a payment intent, in full or in part.")
        .help("", "https://docs.bytechef.io/reference/components/stripe_v1#create-refund")
        .properties(
            string(PAYMENT_INTENT_ID)
                .label("Payment Intent ID")
                .description("ID of the payment intent to refund.")
                .required(true),
            integer(AMOUNT)
                .label("Amount")
                .description(
                    "Amount to refund, in the smallest currency unit (for example 100 cents to refund $1.00). " +
                        "Defaults to the entire amount that has not been refunded yet.")
                .minValue(1)
                .required(false),
            string(REASON)
                .label("Reason")
                .description("Reason for the refund.")
                .options(
                    option("Duplicate", "duplicate"),
                    option("Fraudulent", "fraudulent"),
                    option("Requested by customer", "requested_by_customer"))
                .required(false),
            object(METADATA)
                .label("Metadata")
                .description("Set of key-value pairs that you can attach to the refund.")
                .required(false))
        .output(outputSchema(REFUND_OUTPUT))
        .perform(StripeCreateRefundAction::perform);

    private StripeCreateRefundAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> body = new HashMap<>();

        body.put(PAYMENT_INTENT_ID, inputParameters.getRequiredString(PAYMENT_INTENT_ID));

        Integer amount = inputParameters.getInteger(AMOUNT);

        if (amount != null) {
            body.put(AMOUNT, amount);
        }

        String reason = inputParameters.getString(REASON);

        if (reason != null) {
            body.put(REASON, reason);
        }

        Map<String, ?> metadata = inputParameters.getMap(METADATA);

        if (metadata != null) {
            metadata.forEach((key, value) -> body.put("metadata[" + key + "]", value));
        }

        return context.http(http -> http.post("/refunds"))
            .configuration(responseType(ResponseType.JSON))
            .body(Body.of(body, BodyContentType.FORM_URL_ENCODED))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
