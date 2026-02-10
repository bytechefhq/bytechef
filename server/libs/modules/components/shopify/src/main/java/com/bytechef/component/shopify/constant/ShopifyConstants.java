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

package com.bytechef.component.shopify.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class ShopifyConstants {

    public static final String EMAIL = "email";
    public static final String HAS_VARIANTS = "hasVariants";
    public static final String ID = "id";
    public static final String INPUT = "input";
    public static final String LINE_ITEMS = "lineItems";
    public static final String NAME = "name";
    public static final String NOTE = "note";
    public static final String NOTIFY_CUSTOMER = "notifyCustomer";
    public static final String ORDER = "order";
    public static final String ORDER_ID = "orderId";
    public static final String ORIGINAL_PAYMENT_METHODS_REFUND = "originalPaymentMethodsRefund";
    public static final String PRODUCT = "product";
    public static final String PRODUCTS = "products";
    public static final String PRODUCT_ID = "productId";
    public static final String PRODUCT_OPTIONS = "productOptions";
    public static final String QUANTITY = "quantity";
    public static final String QUERY = "query";
    public static final String REASON = "reason";
    public static final String REFUND_METHOD = "refundMethod";
    public static final String RESTOCK = "restock";
    public static final String SHOP_NAME = "shopName";
    public static final String STAFF_NOTE = "staffNote";
    public static final String STATUS = "status";
    public static final String TAGS = "tags";
    public static final String TITLE = "title";
    public static final String VALUES = "values";
    public static final String VARIABLES = "variables";
    public static final String VARIANT_ID = "variantId";

    public static final ModifiableArrayProperty USER_ERRORS_PROPERTY = array("userErrors")
        .description("The list of errors that occurred from executing the mutation.")
        .items(
            object()
                .properties(
                    string("field")
                        .description("The path to the input field that caused the error."),
                    string("message")
                        .description("The error message.")));

    private ShopifyConstants() {
    }
}
