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

package com.bytechef.component.stripe.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class StripeConstants {

    public static final String AMOUNT = "amount";
    public static final String COLLECTION_METHOD = "collection_method";
    public static final String COUPON_ID = "coupon";
    public static final String CUSTOMER_ID = "customer";
    public static final String DAYS_UNTIL_DUE = "days_until_due";
    public static final String DEFAULT_PAYMENT_METHOD = "default_payment_method";
    public static final String ID = "id";
    public static final String ITEMS = "items";
    public static final String LIMIT = "limit";
    public static final String METADATA = "metadata";
    public static final String PAYMENT_INTENT_ID = "payment_intent";
    public static final String PRICE_ID = "price";
    public static final String QUANTITY = "quantity";
    public static final String REASON = "reason";
    public static final String SUBSCRIPTION_ID = "subscription";

    public static final ModifiableObjectProperty PAYMENT_INTENT_OUTPUT = object()
        .properties(
            string("id")
                .description("Unique identifier for the object."),
            string("object")
                .description("String representing the object's type."),
            integer("amount")
                .description("Amount intended to be collected, in the smallest currency unit."),
            integer("amount_received")
                .description("Amount that this payment intent collected, in the smallest currency unit."),
            integer("created")
                .description("Time at which the object was created, in seconds since the Unix epoch."),
            string("currency")
                .description("Three-letter ISO currency code in lowercase."),
            string("customer")
                .description("ID of the customer this payment intent belongs to, if one exists."),
            string("description")
                .description("An arbitrary string attached to the object."),
            string("latest_charge")
                .description("ID of the latest charge created by this payment intent."),
            bool("livemode")
                .description(
                    "If the object exists in live mode, the value is `true`. If the object exists in test mode, the value is `false`."),
            object("metadata")
                .description("Set of key-value pairs attached to the object."),
            string("payment_method")
                .description("ID of the payment method used in this payment intent."),
            string("receipt_email")
                .description("Email address that the receipt for the resulting payment will be sent to."),
            string("status")
                .description(
                    "Status of this payment intent, one of `requires_payment_method`, `requires_confirmation`, " +
                        "`requires_action`, `processing`, `requires_capture`, `canceled` or `succeeded`."));

    public static final ModifiableObjectProperty REFUND_OUTPUT = object()
        .properties(
            string("id")
                .description("Unique identifier for the object."),
            string("object")
                .description("String representing the object's type."),
            integer("amount")
                .description("Amount refunded, in the smallest currency unit."),
            string("charge")
                .description("ID of the charge that was refunded."),
            integer("created")
                .description("Time at which the object was created, in seconds since the Unix epoch."),
            string("currency")
                .description("Three-letter ISO currency code in lowercase."),
            object("metadata")
                .description("Set of key-value pairs attached to the object."),
            string("payment_intent")
                .description("ID of the payment intent that was refunded."),
            string("reason")
                .description("Reason for the refund, one of `duplicate`, `fraudulent` or `requested_by_customer`."),
            string("status")
                .description(
                    "Status of the refund, one of `pending`, `requires_action`, `succeeded`, `failed` or `canceled`."));

    public static final ModifiableObjectProperty SUBSCRIPTION_OUTPUT = object()
        .properties(
            object("automatic_tax")
                .description("Tax rates that apply to automatic tax calculations."),
            integer("billing_cycle_anchor")
                .description("The reference point that aligns future billing cycle dates."),
            object("billing_mode")
                .description("Billing mode of the subscription."),
            bool("cancel_at_period_end")
                .description("Whether this subscription is cancel at the end of the current billing period."),
            string("collection_method")
                .description("When charging automatically, Stripe will attempt to pay this subscription at the end of" +
                    " the cycle using the default source attached to the customer. When sending an invoice, Stripe " +
                    "will email your customer an invoice with payment instructions and mark the subscription as `active`."),
            integer("created")
                .description("Time at which the object was created."),
            string("currency")
                .description("Three-letter ISO currency code in lowercase."),
            string("customer")
                .description("ID of the customer who owns the subscription."),
            array("discounts")
                .description("The discounts applied to the subscription.")
                .items(string()),
            string("id")
                .description("Unique identifier for the object."),
            object("invoice_settings")
                .description("Invoice settings for the subscription."),
            object("items")
                .description("List of subscription items, each with an attached price.")
                .properties(
                    array("data")
                        .description("Details about each object.")
                        .items(object()),
                    bool("has_more")
                        .description("True if this list has another page of items after this one that can be fetched."),
                    string("object")
                        .description("String representing the object's type."),
                    string("url")
                        .description("The URL where this list can be accessed.")),
            bool("livemode")
                .description(
                    "If the object exists in live mode, the value is `true`. If the object exists in test mode, the value is `false`."),
            object("metadata")
                .description("Set of key-value pairs that you can attach to an object."),
            string("object")
                .description("String representing the object's type. Objects of the same type share the same value."),
            integer("start_date")
                .description("Date when the subscription was first created."),
            string("status")
                .description("Status of the subscription."));

    private StripeConstants() {
    }
}
