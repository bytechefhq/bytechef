/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;

/**
 * @author Mario Cvjetojevic
 */
final class QuickbooksOutputPropertyDefinitions {

    static final ComponentDSL.ModifiableValueProperty<?, ?>[] PHYSICAL_ADDRESS_PROPERTIES =
        {
            string("line1")
                .label("Line 1"),
            string("line2")
                .label("Line 2"),
            string("city")
                .label("City"),
            string("country")
                .label("Country"),
            string("countryCode")
                .label("Country code"),
            string("postalCode")
                .label("Postal code")
        };

    static final ComponentDSL.ModifiableValueProperty<?, ?>[] CREDIT_CHARGE_INFO_PROPERTIES =
        {
            string("number")
                .label("Number"),
            string("nameOnAcct")
                .label("Name on account"),
            integer("ccExpiryMonth")
                .label("Expiry month"),
            integer("ccExpiryYear")
                .label("Expiry year"),
            string("billAddrStreet")
                .label("Billing address street"),
            string("postalCode")
                .label("Postal code"),
            number("amount")
                .label("Amount")
        };

    private QuickbooksOutputPropertyDefinitions() {
    }
}
