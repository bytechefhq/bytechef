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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class StripeCreatePayoutAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createPayout")
        .title("Create Payout")
        .description("Create a payout.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/payouts", "bodyContentType", BodyContentType.FORM_URL_ENCODED, "mimeType",
                "application/x-www-form-urlencoded"

            ))
        .properties(integer("amount").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Amount")
            .description("A positive integer in cents representing how much to payout.")
            .required(true),
            string("currency").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Currency")
                .description("Three-letter ISO currency code in lowercase. Must be a currency supported by Stripe.")
                .required(true),
            string("method").maxLength(5000)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Method")
                .description("The method used to send this payout, which is `standard` or `instant`.")
                .options(option("Instant", "instant"), option("Standard", "standard"))
                .required(false),
            object("metadata").additionalProperties(string())
                .placeholder("Add to Metadata")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Metadata")
                .description(
                    "Set of key-value pairs that you can attach to an object. This can be useful for storing additional information about the object in a structured format.")
                .required(false))
        .output(outputSchema(object().properties(
            integer("amount")
                .description(
                    "The amount (in cents (or local equivalent)) that transfers to your bank account or debit card.")
                .required(false),
            integer("arrival_date").description(
                "Date that you can expect the payout to arrive in the bank. This factors in delays to account for weekends or bank holidays.")
                .required(false),
            bool("automatic").description(
                "Returns `true` if the payout is created by an automated payout schedule and `false` if it's requested manually.")
                .required(false),
            integer("created").description("Time at which the object was created.")
                .required(false),
            string("currency").description("Three-letter ISO currency code in lowercase.")
                .required(false),
            string("id").description("Unique identifier for the object.")
                .required(false),
            bool("livemode").description(
                "If the object exists in live mode, the value is `true`. If the object exists in test mode, the value is `false`.")
                .required(false),
            string("method").description("The method used to send this payout, which can be `standard` or `instant`.")
                .required(false),
            string("object")
                .description("String representing the object's type. Objects of the same type share the same value.")
                .required(false),
            string("reconciliation_status").description(
                "If `completed`, you can use the Balance Transactions API to list all balance transactions that are paid out in this payout.")
                .required(false),
            string("source_type").description(
                "The source balance this payout came from, which can be one of the following: `card`, `fpx` or `bank_account`.")
                .required(false),
            string("status").description("Current status of the payout.")
                .required(false),
            string("type").description("Can be `bank_account` or `card`.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/stripe_v1#create-payout");

    private StripeCreatePayoutAction() {
    }
}
