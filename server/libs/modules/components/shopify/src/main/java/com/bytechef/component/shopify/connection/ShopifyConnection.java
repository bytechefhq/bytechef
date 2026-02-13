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
                .scopes((connection, context) -> List.of("write_orders"))
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
