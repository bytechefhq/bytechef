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

package com.bytechef.component.woocommerce.constants;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class WoocommerceConstants {

    public static final String ADDRESS_1 = "address_1";
    public static final String ADDRESS_2 = "address_2";
    public static final String AMOUNT = "amount";
    public static final String BILLING = "billing";
    public static final String CATEGORIES = "categories";
    public static final String CITY = "city";
    public static final String CODE = "code";
    public static final String COMPANY = "company";
    public static final String COUNTRY = "country";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String CUSTOMER_NOTE = "customer_note";
    public static final String DATE_EXPIRES = "date_expires";
    public static final String DESCRIPTION = "description";
    public static final String DIMENSIONS = "dimensions";
    public static final String DISCOUNT_TYPE = "discount_type";
    public static final String DOMAIN = "domain";
    public static final String EMAIL = "email";
    public static final String EXCLUDE_SALE_ITEMS = "exclude_sale_items";
    public static final String FIRST_NAME = "first_name";
    public static final String HEIGHT = "height";
    public static final String ID = "id";
    public static final String IMAGES = "images";
    public static final String INDIVIDUAL_USE = "individual_use";
    public static final String LAST_NAME = "last_name";
    public static final String LINE_ITEMS = "line_items";
    public static final String LENGTH = "length";
    public static final String MANAGE_STOCK = "manage_stock";
    public static final String MAXIMUM_AMOUNT = "maximum_amount";
    public static final String MINIMUM_AMOUNT = "minimum_amount";
    public static final String NAME = "name";
    public static final String PAYMENT_METHOD = "payment_method";
    public static final String PHONE = "phone";
    public static final String POSTCODE = "postcode";
    public static final String PRODUCT_ID = "product_id";
    public static final String PRODUCT_IDS = "product_ids";
    public static final String QUANTITY = "quantity";
    public static final String REGULAR_PRICE = "regular_price";
    public static final String SET_PAID = "set_paid";
    public static final String SHIPPING = "shipping";
    public static final String SRC = "src";
    public static final String STATE = "state";
    public static final String STATUS = "status";
    public static final String STOCK_QUANTITY = "stock_quantity";
    public static final String STOCK_STATUS = "stock_status";
    public static final String TAGS = "tags";
    public static final String TYPE = "type";
    public static final String USERNAME = "username";
    public static final String WEIGHT = "weight";
    public static final String WIDTH = "width";

    public static final ModifiableObjectProperty COUPON_OUTPUT_PROPERTY =
        object()
            .properties(
                integer("id")
                    .description("Unique identifier for the object."),
                string("code")
                    .description("Coupon code."),
                string("amount")
                    .description("The amount of discount."),
                string("date_created")
                    .description("The date the coupon was created, in the site's timezone."),
                string("date_created_gmt")
                    .description("The date the coupon was created, as GMT."),
                string("date_modified")
                    .description("The date the coupon was last modified, in the site's timezone."),
                string("date_modified_gmt")
                    .description("The date the coupon was last modified, as GMT."),
                string("discount_type")
                    .description("Determines the type of discount that will be applied."),
                string("description")
                    .description("Coupon description."),
                string("date_expires")
                    .description("The date the coupon expires, in the site's timezone."),
                string("date_expires_gmt")
                    .description("The date the coupon expires, as GMT."),
                integer("usage_count")
                    .description("Number of times the coupon has been used already."),
                bool("individual_use")
                    .description("If true, the coupon can only be used individually."),
                array("product_ids")
                    .items(integer())
                    .description("List of product IDs the coupon can be used on."),
                array("excluded_product_ids")
                    .items(integer())
                    .description("List of product IDs the coupon cannot be used on."),
                integer("usage_limit")
                    .description("How many times the coupon can be used in total."),
                integer("usage_limit_per_user")
                    .description("How many times the coupon can be used per customer."),
                integer("limit_usage_to_x_items")
                    .description("Max number of items in the cart the coupon can be applied to."),
                bool("free_shipping")
                    .description(
                        "If true and if the free shipping method requires a coupon, this coupon will enable free shipping."),
                array("product_categories")
                    .items(integer())
                    .description("List of category IDs the coupon applies to."),
                array("excluded_product_categories")
                    .items(integer())
                    .description("List of category IDs the coupon does not apply to."),
                bool("exclude_sale_items")
                    .description("If true, this coupon will not be applied to items that have sale prices."),
                string("minimum_amount")
                    .description("Minimum order amount that needs to be in the cart before coupon applies."),
                string("maximum_amount")
                    .description("Maximum order amount allowed when using the coupon."),
                array("email_restrictions")
                    .items(string())
                    .description("List of email addresses that can use this coupon."),
                array("used_by")
                    .items(string())
                    .description("List of user IDs (or guest email addresses) that have used the coupon."),
                array("meta_data")
                    .description("Meta data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("key"),
                                string("value"))),
                object("_links")
                    .description("")
                    .properties(
                        array("self").items(object().properties(string("href"))),
                        array("collection").items(object().properties(string("href")))));

    public static final ModifiableObjectProperty CUSTOMER_OUTPUT_PROPERTY =
        object()
            .properties(
                integer("id")
                    .description("Unique identifier for the resource."),
                string("date_created")
                    .description("The date the customer was created, in the site's timezone."),
                string("date_created_gmt")
                    .description("The date the customer was created, as GMT."),
                string("date_modified")
                    .description("The date the customer was last modified, in the site's timezone."),
                string("date_modified_gmt")
                    .description("The date the customer was last modified, as GMT."),
                string("email")
                    .description("The email address for the customer."),
                string("first_name")
                    .description("Customer first name."),
                string("last_name")
                    .description("Customer last name."),
                string("role")
                    .description("Customer role."),
                string("username")
                    .description("Customer login name."),
                object("billing")
                    .description("List of billing address data.")
                    .properties(
                        string("first_name"),
                        string("last_name"),
                        string("company"),
                        string("address_1"),
                        string("address_2"),
                        string("city"),
                        string("state"),
                        string("postcode"),
                        string("country"),
                        string("email"),
                        string("phone")),
                object("shipping")
                    .description("List of shipping address data.")
                    .properties(
                        string("first_name"),
                        string("last_name"),
                        string("company"),
                        string("address_1"),
                        string("address_2"),
                        string("city"),
                        string("state"),
                        string("postcode"),
                        string("country")),
                bool("is_paying_customer")
                    .description("Is the customer a paying customer?"),
                string("avatar_url")
                    .description("Avatar URL."),
                array("meta_data")
                    .description("Meta data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("key"),
                                string("value"))),
                object("_links")
                    .description("")
                    .properties(
                        array("self").items(object().properties(string("href"))),
                        array("collection").items(object().properties(string("href")))));

    public static final ModifiableObjectProperty ORDER_OUTPUT_PROPERTY =
        object()
            .properties(
                integer("id")
                    .description("Unique identifier for the resource."),
                string("parent_id")
                    .description("Parent order ID."),
                string("number")
                    .description("Order number."),
                string("order_key")
                    .description("Order key."),
                string("created_via")
                    .description("Shows where the order was created."),
                string("version")
                    .description("Version of WooCommerce which last updated the order."),
                string("status")
                    .description("Order status."),
                string("currency")
                    .description("Currency the order was created with, in ISO format."),
                string("date_created")
                    .description("The date the order was created, in the site's timezone."),
                string("date_created_gmt")
                    .description("The date the order was created, as GMT."),
                string("date_modified")
                    .description("The date the order was last modified, in the site's timezone."),
                string("date_modified_gmt")
                    .description("The date the order was last modified, as GMT."),
                string("discount_total")
                    .description("Total discount amount for the order."),
                string("discount_tax")
                    .description("Total discount tax amount for the order."),
                string("shipping_total")
                    .description("Total shipping amount for the order."),
                string("shipping_tax")
                    .description("Total shipping tax amount for the order."),
                string("cart_tax")
                    .description("Sum of line item taxes only."),
                string("total")
                    .description("Grand total."),
                string("total_tax")
                    .description("Sum of all taxes."),
                bool("prices_include_tax")
                    .description("True the prices included tax during checkout."),
                integer("customer_id")
                    .description("User ID who owns the order. 0 for guests. Default is 0."),
                string("customer_ip_address")
                    .description("Customer's IP address."),
                string("customer_user_agent")
                    .description("User agent of the customer."),
                string("customer_note")
                    .description("Note left by customer during checkout."),
                object("billing")
                    .description("Billing address.")
                    .properties(
                        string("first_name"),
                        string("last_name"),
                        string("company"),
                        string("address_1"),
                        string("address_2"),
                        string("city"),
                        string("state"),
                        string("postcode"),
                        string("country"),
                        string("email"),
                        string("phone")),
                object("shipping")
                    .description("Shipping address.")
                    .properties(
                        string("first_name"),
                        string("last_name"),
                        string("company"),
                        string("address_1"),
                        string("address_2"),
                        string("city"),
                        string("state"),
                        string("postcode"),
                        string("country")),
                string("payment_methods")
                    .description("Payment method ID."),
                string("payment_method_title")
                    .description("Payment method title."),
                string("transaction_id")
                    .description("Unique transaction ID."),
                string("date_paid")
                    .description("The date the order was paid, in the site's timezone."),
                string("date_paid_gmt")
                    .description("The date the order was paid, as GMT."),
                string("date_completed")
                    .description("The date the order was completed, in the site's timezone."),
                string("date_completed_gmt")
                    .description("The date the order was completed, as GMT."),
                string("cart_hash")
                    .description("Sum of line item taxes only."),
                array("meta_data")
                    .description("Meta data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("key"),
                                string("value"))),
                array("line_items")
                    .description("Line items data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("name"),
                                integer("product_id"),
                                integer("variation_id"),
                                integer("quantity"),
                                string("tax_class"),
                                string("subtotal"),
                                string("subtotal_tax"),
                                string("total"),
                                string("total_tax"),
                                array("taxes"),
                                array("meta_data"),
                                string("sku"),
                                string("price"))),
                array("tax_lines")
                    .description("Tax lines data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("rate_code"),
                                integer("rate_id"),
                                string("label"),
                                bool("compound"),
                                string("tax_total"),
                                string("shipping_tax_total"),
                                array("mata_data"))),
                array("shipping_lines")
                    .description("Shipping lines data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("method_title"),
                                string("method_id"),
                                string("total"),
                                string("total_tax"),
                                array("taxes"),
                                array("meta_data"))),
                array("fee_lines")
                    .description("Fee lines data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("name"),
                                string("tax_class"),
                                string("tax_status"),
                                string("total"),
                                string("total_tax"),
                                array("taxes"),
                                array("mata_data"))),
                array("coupon_lines")
                    .description("Coupons line data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("code"),
                                string("discount"),
                                string("discount_tax"),
                                array("meta_data"))),
                array("refunds")
                    .description("List of refunds.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("reason"),
                                string("total"))),
                object("_links")
                    .description("")
                    .properties(
                        array("self").items(object().properties(string("href"))),
                        array("collection").items(object().properties(string("href")))));

    public static final ModifiableObjectProperty PRODUCT_OUTPUT_PROPERTY =
        object()
            .properties(
                integer("id")
                    .description("Unique identifier for the resource."),
                string("name")
                    .description("Product name."),
                string("slug")
                    .description("Product slug."),
                string("permalink")
                    .description("Product URL."),
                string("date_created")
                    .description("The date the product was created, in the site's timezone."),
                string("date_created_gmt")
                    .description("The date the product was created, as GMT."),
                string("date_modified")
                    .description("The date the product was last modified, in the site's timezone."),
                string("date_modified_gmt")
                    .description("The date the product was last modified, as GMT."),
                string("type")
                    .description("Product type."),
                string("status")
                    .description("Product status."),
                bool("featured")
                    .description("Featured product."),
                string("catalog_visibility")
                    .description("Catalog visibility."),
                string("description")
                    .description("Product description."),
                string("short_description")
                    .description("Product short description."),
                string("sku")
                    .description("Unique identifier."),
                string("price")
                    .description("Current product price."),
                string("regular_price")
                    .description("Product regular price."),
                string("sale_price")
                    .description("Product sale price."),
                string("date_on_sale_from")
                    .description("Start date of sale price, in the site's timezone."),
                string("date_on_sale_from_gmt")
                    .description("Start date of sale price, as GMT."),
                string("date_on_sale_to")
                    .description("End date of sale price, in the site's timezone."),
                string("date_on_sale_to_gmt")
                    .description("End date of sale price, as GMT."),
                string("price_html")
                    .description("Price formatted in HTML."),
                bool("on_sale")
                    .description("Shows if the product is on sale."),
                bool("purchasable")
                    .description("Shows if the product can be bought."),
                integer("total_sales")
                    .description("Amount of sales."),
                bool("virtual")
                    .description("If the product is virtual. Default is false."),
                bool("downloadable")
                    .description("If the product is downloadable. Default is false."),
                array("downloads")
                    .description("List of downloadable files.")
                    .items(
                        object()
                            .properties(
                                string("id"),
                                string("name"),
                                string("file"))),
                integer("download_limit")
                    .description("Number of times downloadable files can be downloaded after purchase."),
                integer("download_expiry")
                    .description("Number of days until access to downloadable files expires."),
                string("external_url")
                    .description("Product external URL. Only for external products."),
                string("button_text")
                    .description("Product external button text. Only for external products."),
                string("tax_status")
                    .description("Tax status"),
                string("tax_class")
                    .description("Tax class."),
                bool("manage_stock")
                    .description("Stock management at product level."),
                integer("stock_quantity")
                    .description("Stock quantity."),
                string("stock_status")
                    .description("Controls the stock status of the product."),
                string("backorders")
                    .description("If managing stock, this controls if backorders are allowed."),
                bool("backorders_allowed")
                    .description("Shows if backorders are allowed."),
                bool("backordered")
                    .description("Shows if the product is on backordered."),
                bool("sold_individually")
                    .description("Allow one item to be bought in a single order."),
                string("weight")
                    .description("Product weight."),
                object("dimensions")
                    .description("Product dimensions.")
                    .properties(
                        string("length"),
                        string("width"),
                        string("height")),
                bool("shipping_required")
                    .description("Shows if the product need to be shipped."),
                bool("shipping_taxable")
                    .description("Shows whether or not the product shipping is taxable."),
                string("shipping_class")
                    .description("Shipping class slug."),
                integer("shipping_class_id")
                    .description("Shipping class ID."),
                bool("reviews_allowed")
                    .description("Allow reviews."),
                string("average_rating")
                    .description("Reviews average rating."),
                integer("rating_count")
                    .description("Amount of reviews that the product have."),
                array("related_ids")
                    .description("List of related products IDs.")
                    .items(integer()),
                array("upsell_ids")
                    .description("List of up-sell products IDs.")
                    .items(integer()),
                array("cross_sell_ids")
                    .description("List of cross-sell products IDs.")
                    .items(integer()),
                integer("parent_id")
                    .description("Product parent ID."),
                string("purchase_note")
                    .description("Optional note to send the customer after purchase."),
                array("categories")
                    .description("List of categories.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("name"),
                                string("slug"))),
                array("tags")
                    .description("List of tags.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("name"),
                                string("slug"))),
                array("images")
                    .description("List of images.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("date_created"),
                                string("date_created_gmt"),
                                string("date_modified"),
                                string("date_modified_gmt"),
                                string("src"),
                                string("name"),
                                string("alt"))),
                array("attributes")
                    .description("List of attributes.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("name"),
                                integer("position"),
                                bool("visible"),
                                bool("variation"),
                                array("options"))),
                array("default_attributes")
                    .description("Defaults variation attributes.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("name"),
                                string("option"))),
                array("variations")
                    .description("List of variations IDs."),
                array("grouped_products")
                    .description("List of grouped products ID."),
                integer("menu_order")
                    .description("Menu order, used to custom sort products."),
                array("meta_data")
                    .description("Meta data.")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                string("key"),
                                string("value"))),
                object("_links")
                    .description("")
                    .properties(
                        array("self").items(object().properties(string("href"))),
                        array("collection").items(object().properties(string("href")))));

    public static final ModifiableObjectProperty TRIGGER_OUTPUT_PROPERTY =
        object()
            .properties(
                integer("id")
                    .description("Unique identifier for the resource."),
                string("name")
                    .description("A friendly name for the webhook."),
                string("status")
                    .description("Webhook status."),
                string("topic")
                    .description("Webhook topic."),
                string("resource")
                    .description("Webhook resource."),
                string("event")
                    .description("Webhook event."),
                array("hooks")
                    .description("WooCommerce action names associated with the webhook.")
                    .items(string()),
                string("delivery_url")
                    .description("The URL where the webhook payload is delivered."),
                string("date_created")
                    .description("The date the webhook was created, in the site's timezone."),
                string("date_created_gmt")
                    .description("The date the webhook was created, as GMT."),
                string("date_modified")
                    .description("The date the webhook was last modified, in the site's timezone."),
                string("date_modified_gmt")
                    .description("The date the webhook was last modified, as GMT."),
                object("_links")
                    .description("")
                    .properties(
                        array("self").items(object().properties(string("href"))),
                        array("collection").items(object().properties(string("href")))));

    private WoocommerceConstants() {
    }

}
