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

package com.bytechef.component.shopify.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class ShopifyOrderProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(integer("id").label("Id")
        .required(false),
        string("currency").label("Currency")
            .required(false),
        string("note").label("Note")
            .required(false),
        string("email").label("Email")
            .required(false),
        string("name").label("Name")
            .required(false),
        string("phone").label("Phone")
            .required(false),
        string("tags").label("Tags")
            .required(false),
        array("line_items").items(object().properties(string("fulfillment_status").label("Fullfilment Status")
            .required(false),
            string("grams").label("Grams")
                .required(false),
            number("price").label("Price")
                .required(false),
            integer("product_id").label("Product")
                .required(false),
            integer("variant_id").label("Variant")
                .required(false),
            integer("quantity").label("Quantity")
                .required(false),
            string("title").label("Title")
                .required(false)))
            .placeholder("Add to Line Items")
            .label("Line Items")
            .required(false));

    private ShopifyOrderProperties() {
    }
}
