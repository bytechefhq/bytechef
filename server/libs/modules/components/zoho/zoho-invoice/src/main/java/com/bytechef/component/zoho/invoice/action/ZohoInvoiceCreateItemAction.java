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

package com.bytechef.component.zoho.invoice.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.DESCRIPTION;
import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.NAME;
import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.PRODUCT_TYPE;
import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.RATE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class ZohoInvoiceCreateItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createItem")
        .title("Create Item")
        .description("Create a item.")
        .properties(
            string(NAME)
                .label("Item Name")
                .description("Name of the item.")
                .maxLength(100)
                .required(true),
            number(RATE)
                .label("Rate")
                .description("Per unit price of an item.")
                .required(true),
            string(PRODUCT_TYPE)
                .label("Product Type")
                .description("Specify the type of an item.")
                .options(option("GOODS", "goods"), option("SERVICE", "service"))
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description for the item.")
                .maxLength(2000)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        number("code")
                            .description(
                                "Zoho Invoice error code. This will be zero for a success response and non-zero in " +
                                    "case of an error."),
                        string("message")
                            .description("Message for the invoked API."),
                        object("item")
                            .description("Created item."))))
        .perform(ZohoInvoiceCreateItemAction::perform);

    private ZohoInvoiceCreateItemAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        return context.http(http -> http.post("/items"))
            .body(
                Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    RATE, inputParameters.getRequiredDouble(RATE),
                    PRODUCT_TYPE, inputParameters.getRequiredString(PRODUCT_TYPE),
                    DESCRIPTION, inputParameters.getString(DESCRIPTION)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
