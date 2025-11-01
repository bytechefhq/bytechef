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

package com.bytechef.component.woocommerce.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.AMOUNT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CODE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.COUPON_OUTPUT_PROPERTY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DATE_EXPIRES;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DESCRIPTION;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DISCOUNT_TYPE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.EXCLUDE_SALE_ITEMS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.INDIVIDUAL_USE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.MAXIMUM_AMOUNT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.MINIMUM_AMOUNT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PRODUCT_IDS;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.woocommerce.util.WoocommerceUtils;

/**
 * @author Marija Horvat
 */
public class WoocommerceCreateCouponAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCoupon")
        .title("Create Coupon")
        .description("Create a new coupon.")
        .properties(
            string(CODE)
                .label("Code")
                .description("Coupon code.")
                .required(true),
            string(AMOUNT)
                .label("Amount")
                .description("The amount of discount. Should always be numeric, even if setting a percentage.")
                .required(true),
            string(DISCOUNT_TYPE)
                .label("Discount Type")
                .description("Determines the type of discount that will be applied.")
                .options(
                    option("Percent", "percent"),
                    option("Fixed_cart", "fixed_cart"),
                    option("Fixed_product", "fixed_product"))
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Coupon description.")
                .required(false),
            dateTime(DATE_EXPIRES)
                .label("Date Expires")
                .description("The date the coupon expires, in the site's timezone.")
                .required(false),
            bool(INDIVIDUAL_USE)
                .label("Individual Use")
                .description(
                    "If true, the coupon can only be used individually. Other applied coupons will be removed from " +
                        "the cart.")
                .required(false),
            array(PRODUCT_IDS)
                .label("Product Ids")
                .description("List of product IDs the coupon can be used on.")
                .required(false)
                .options((OptionsFunction<String>) WoocommerceUtils::getProductIdOptions)
                .items(integer()),
            bool(EXCLUDE_SALE_ITEMS)
                .label("Exclude Sale Items")
                .description("If true, this coupon will not be applied to items that have sale prices.")
                .required(false),
            string(MINIMUM_AMOUNT)
                .label("Minimum Amount")
                .description("Minimum order amount that needs to be in the cart before coupon applies.")
                .required(false),
            string(MAXIMUM_AMOUNT)
                .label("Maximum Amount")
                .description("Maximum order amount allowed when using the coupon.")
                .required(false))
        .output(outputSchema(COUPON_OUTPUT_PROPERTY))
        .perform(WoocommerceCreateCouponAction::perform);

    private WoocommerceCreateCouponAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        return context.http(http -> http.post("/coupons"))
            .body(
                Body.of(
                    CODE, inputParameters.getRequiredString(CODE),
                    AMOUNT, inputParameters.getRequiredString(AMOUNT),
                    DISCOUNT_TYPE, inputParameters.getRequiredString(DISCOUNT_TYPE),
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    DATE_EXPIRES, inputParameters.getDate(DATE_EXPIRES),
                    INDIVIDUAL_USE, inputParameters.getBoolean(INDIVIDUAL_USE),
                    PRODUCT_IDS, inputParameters.getList(PRODUCT_IDS),
                    EXCLUDE_SALE_ITEMS, inputParameters.getBoolean(EXCLUDE_SALE_ITEMS),
                    MINIMUM_AMOUNT, inputParameters.getString(MINIMUM_AMOUNT),
                    MAXIMUM_AMOUNT, inputParameters.getString(MAXIMUM_AMOUNT)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
