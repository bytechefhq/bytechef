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

package com.bytechef.component.shopify.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.SHOP_NAME;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class ShopifyConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://%s/admin/api"
            .formatted(connectionParameters.getRequiredString(SHOP_NAME)))
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(SHOP_NAME)
                        .label("Shop Name")
                        .description("Shopify shop name e.g. name.myshopify.com.")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .description("Client ID can be found in Dev Dashboard.")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .description("Client secret can be found in Dev Dashboard.")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://%s/admin/oauth/authorize"
                    .formatted(connectionParameters.getRequiredString(SHOP_NAME)))
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new HashMap<>();

                    map.put("read_all_orders", false);
                    map.put("write_app_proxy", false);
                    map.put("read_assigned_fulfillment_orders", false);
                    map.put("write_assigned_fulfillment_orders", false);
                    map.put("read_merchant_managed_fulfillment_orders", false);
                    map.put("write_merchant_managed_fulfillment_orders", false);
                    map.put("read_third_party_fulfillment_orders", false);
                    map.put("write_third_party_fulfillment_orders", false);
                    map.put("read_marketplace_fulfillment_orders", false);
                    map.put("read_cart_transforms", false);
                    map.put("write_cart_transforms", false);
                    map.put("read_checkout_branding_settings", false);
                    map.put("write_checkout_branding_settings", false);
                    map.put("read_content", false);
                    map.put("write_content", false);
                    map.put("read_online_store_pages", false);
                    map.put("read_customer_events", false);
                    map.put("write_pixels", false);
                    map.put("read_customer_merge", false);
                    map.put("write_customer_merge", false);
                    map.put("read_customer_payment_methods", false);
                    map.put("read_customers", false);
                    map.put("write_customers", false);
                    map.put("read_delivery_customizations", false);
                    map.put("write_delivery_customizations", false);
                    map.put("read_discounts", false);
                    map.put("write_discounts", false);
                    map.put("read_draft_orders", false);
                    map.put("write_draft_orders", false);
                    map.put("read_files", false);
                    map.put("write_files", false);
                    map.put("read_fulfillments", false);
                    map.put("write_fulfillments", false);
                    map.put("read_gift_cards", false);
                    map.put("write_gift_cards", false);
                    map.put("read_inventory", false);
                    map.put("write_inventory", false);
                    map.put("read_legal_policies", false);
                    map.put("read_locales", false);
                    map.put("write_locales", false);
                    map.put("read_locations", false);
                    map.put("write_locations", false);
                    map.put("read_markets", false);
                    map.put("write_markets", false);
                    map.put("read_marketing_events", false);
                    map.put("write_marketing_events", false);
                    map.put("read_merchant_approval_signals", false);
                    map.put("read_metaobject_definitions", false);
                    map.put("write_metaobject_definitions", false);
                    map.put("read_metaobjects", false);
                    map.put("write_metaobjects", false);
                    map.put("read_online_store_navigation", false);
                    map.put("write_online_store_navigation", false);
                    map.put("read_order_edits", false);
                    map.put("write_order_edits", false);
                    map.put("read_orders", false);
                    map.put("write_orders", true);
                    map.put("read_own_subscription_contracts", false);
                    map.put("write_own_subscription_contracts", false);
                    map.put("read_payment_customizations", false);
                    map.put("write_payment_customizations", false);
                    map.put("read_payment_gateways", false);
                    map.put("write_payment_gateways", false);
                    map.put("read_payment_mandate", false);
                    map.put("write_payment_mandate", false);
                    map.put("write_payment_sessions", false);
                    map.put("read_payment_terms", false);
                    map.put("write_payment_terms", false);
                    map.put("read_price_rules", false);
                    map.put("write_price_rules", false);
                    map.put("write_privacy_settings", false);
                    map.put("read_privacy_settings", false);
                    map.put("read_products", false);
                    map.put("write_products", false);
                    map.put("read_purchase_options", false);
                    map.put("write_purchase_options", false);
                    map.put("read_reports", false);
                    map.put("read_returns", false);
                    map.put("write_returns", false);
                    map.put("read_script_tags", false);
                    map.put("write_script_tags", false);
                    map.put("read_shipping", false);
                    map.put("write_shipping", false);
                    map.put("read_shopify_payments_disputes", false);
                    map.put("read_shopify_payments_dispute_evidences", false);
                    map.put("read_shopify_payments_payouts", false);
                    map.put("read_store_credit_accounts", false);
                    map.put("read_store_credit_account_transactions", false);
                    map.put("write_store_credit_account_transactions", false);
                    map.put("read_themes", false);
                    map.put("write_themes", false);
                    map.put("read_translations", false);
                    map.put("write_translations", false);
                    map.put("read_users", false);
                    map.put("read_validations", false);
                    map.put("write_validations", false);

                    return map;
                })
                .tokenUrl((connectionParameters, context) -> "https://%s/admin/oauth/access_token"
                    .formatted(connectionParameters.getRequiredString(SHOP_NAME)))
                .refreshUrl((connectionParameters, context) -> "https://%s/admin/oauth/access_token"
                    .formatted(connectionParameters.getRequiredString(SHOP_NAME)))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of("X-Shopify-Access-Token", List.of(connectionParameters.getRequiredString(ACCESS_TOKEN))))))
        .help("", "https://docs.bytechef.io/reference/components/hopify_v1#connection-setup")
        .version(1);

    private ShopifyConnection() {
    }
}
