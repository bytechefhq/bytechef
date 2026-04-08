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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.stripe.constant.StripeConstants.COLLECTION_METHOD;
import static com.bytechef.component.stripe.constant.StripeConstants.COUPON_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.CUSTOMER_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.DAYS_UNTIL_DUE;
import static com.bytechef.component.stripe.constant.StripeConstants.DEFAULT_PAYMENT_METHOD;
import static com.bytechef.component.stripe.constant.StripeConstants.ITEMS;
import static com.bytechef.component.stripe.constant.StripeConstants.METADATA;
import static com.bytechef.component.stripe.constant.StripeConstants.PRICE_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.QUANTITY;
import static com.bytechef.component.stripe.constant.StripeConstants.SUBSCRIPTION_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.SUBSCRIPTION_OUTPUT;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.stripe.util.StripeUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class StripeUpdateSubscriptionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateSubscription")
        .title("Update Subscription")
        .description("Updates an existing subscription.")
        .help("", "https://docs.bytechef.io/reference/components/stripe_v1#update-subscription")
        .properties(
            string(CUSTOMER_ID)
                .label("Customer ID")
                .description("ID of the customer to subscribe.")
                .options((ActionDefinition.OptionsFunction<String>) StripeUtils::getCustomerOptions)
                .required(true),
            string(SUBSCRIPTION_ID)
                .label("Subscription ID")
                .description("Unique identifier of the subscription to update.")
                .options((ActionDefinition.OptionsFunction<String>) StripeUtils::getSubscriptionIdOptions)
                .optionsLookupDependsOn(CUSTOMER_ID)
                .required(true),
            array(ITEMS)
                .label("Items")
                .description("A list of up to 20 subscription items.")
                .required(false)
                .items(
                    object()
                        .properties(
                            string(PRICE_ID)
                                .description("The ID of the price object.")
                                .options((ActionDefinition.OptionsFunction<String>) StripeUtils::getPriceOptions)
                                .required(false),
                            string(COUPON_ID)
                                .description("ID of the coupon to create a new discount for.")
                                .options((ActionDefinition.OptionsFunction<String>) StripeUtils::getCouponOptions)
                                .required(false),
                            integer(QUANTITY)
                                .description("Quantity for this item.")
                                .required(false))),
            string(COLLECTION_METHOD)
                .label("Collection Method")
                .description("When charging automatically, Stripe will attempt to pay this subscription at the end " +
                    "of the cycle using the default source attached to the customer. When sending an invoice, Stripe " +
                    "will email your customer an invoice with payment instructions and mark the subscription as `active`.")
                .options(
                    option("charge automatically", "charge_automatically"),
                    option("send invoice", "send_invoice"))
                .required(false),
            integer(DAYS_UNTIL_DUE)
                .label("Days Until Due")
                .description("Number of days a customer has to pay invoices generated by this subscription.")
                .displayCondition("%s == '%s'".formatted(COLLECTION_METHOD, "send_invoice"))
                .required(true),
            string(DEFAULT_PAYMENT_METHOD)
                .label("Default Payment Method")
                .description("ID of the default payment method for the subscription. It must belong to the customer " +
                    "associated with the subscription.")
                .options((ActionDefinition.OptionsFunction<String>) StripeUtils::getDefaultPaymentMethodOptions)
                .optionsLookupDependsOn(CUSTOMER_ID)
                .required(false),
            object(METADATA)
                .description("Set of key-value pairs that you can attach to an object.")
                .required(false))
        .output(outputSchema(SUBSCRIPTION_OUTPUT))
        .perform(StripeUpdateSubscriptionAction::perform);

    private StripeUpdateSubscriptionAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> body = new HashMap<>();

        if (inputParameters.getString(COLLECTION_METHOD) != null) {
            body.put(COLLECTION_METHOD, inputParameters.getString(COLLECTION_METHOD));
        }

        if (inputParameters.getInteger(DAYS_UNTIL_DUE) != null) {
            body.put(DAYS_UNTIL_DUE, inputParameters.getInteger(DAYS_UNTIL_DUE));
        }

        if (inputParameters.getString(DEFAULT_PAYMENT_METHOD) != null) {
            body.put(DEFAULT_PAYMENT_METHOD, inputParameters.getString(DEFAULT_PAYMENT_METHOD));
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) inputParameters.getList(ITEMS);

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                if (item.containsKey(COUPON_ID)) {
                    body.put("items[" + i + "][discounts][0][coupon]", item.get(COUPON_ID));
                }
                if (item.containsKey(PRICE_ID)) {
                    body.put("items[" + i + "][price]", item.get(PRICE_ID));
                }
                if (item.containsKey(QUANTITY)) {
                    body.put("items[" + i + "][quantity]", item.get(QUANTITY));
                }
            }
        }

        Map<String, ?> metadata = inputParameters.getMap(METADATA);
        if (metadata != null) {
            metadata.forEach((k, v) -> body.put("metadata[" + k + "]", v));
        }

        return context.http(http -> http.post("/subscriptions/" + inputParameters.getRequiredString(SUBSCRIPTION_ID)))
            .configuration(responseType(ResponseType.JSON))
            .body(Body.of(body, BodyContentType.FORM_URL_ENCODED))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
