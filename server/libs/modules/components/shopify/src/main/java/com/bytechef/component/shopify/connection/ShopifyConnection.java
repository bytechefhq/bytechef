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
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;

import javax.naming.ConfigurationException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ShopifyConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) ->
            "https://%s.myshopify.com/admin/api/2025-10/graphql.json".formatted(
                connectionParameters.getRequiredString(SHOP_NAME)))
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .properties(
                    string(SHOP_NAME)
                        .label("Shop Name")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) ->
                    "https://%s.myshopify.com/admin/oauth/authorize"
                        .formatted(
                            connectionParameters.getRequiredString(SHOP_NAME)))
                .scopes(
                    (connection, context) -> List.of("write_orders"))
                .tokenUrl((connectionParameters, context) -> "https://%s.myshopify.com/admin/oauth/access_token"
                    .formatted(connectionParameters.getRequiredString(SHOP_NAME)))
                .refreshUrl((connectionParameters, context) -> "https://%s.myshopify.com/admin/oauth/access_token"
                    .formatted(connectionParameters.getRequiredString(SHOP_NAME)))
                .authorizationCallback((connectionParameters, code, redirectUri, codeVerifier, context) -> {
                        String clientId = connectionParameters.getString(CLIENT_ID);
                        String clientSecret = connectionParameters.getString(CLIENT_SECRET);
                        String valueToEncode = clientId + ":" + clientSecret;
                        String encode = context.encoder(
                            encoder -> encoder.base64EncodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8)));

                        Http.Response httpResponse =
                            context.http(http -> http.post("https://%s.myshopify.com/admin/oauth/access_token"
                                    .formatted(connectionParameters.getRequiredString(SHOP_NAME))))
                                .headers(
                                    Map.of(
                                        "Accept", List.of("application/json"),
                                        "Authorization", List.of("Basic " + encode)))
                                .queryParameters(
                                    "client_id", List.of(clientId),
                                    "nonce", List.of(code),
                                    "scope", List.of("write_orders"),
                                    "grant_options[]", List.of("per-user"),
                                    "redirect_uri", List.of(redirectUri))
                                .configuration(Http.responseType(Http.ResponseType.JSON))
                                .execute();

                        if (httpResponse.getStatusCode() < 200 || httpResponse.getStatusCode() > 299) {
                            throw new ConfigurationException("Invalid claim");
                        }

                        if (httpResponse.getBody() == null) {
                            throw new ConfigurationException("Invalid claim");
                        }

                        return new AuthorizationCallbackResponse(httpResponse.getBody(new TypeReference<>() {}));

                })
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of("X-Shopify-Access-Token", List.of(connectionParameters.getRequiredString(ACCESS_TOKEN))))));
//    http://127.0.0.1:5173/oauth.html?code=c0738328afec58c02b5de77944a0a0d2&hmac=bb62cffddee527201784cd6ac3851beb53360e1ed0b0a6a1025a9f335758d018&host=YWRtaW4uc2hvcGlmeS5jb20vc3RvcmUvYnl0ZWNoZWYtdGVzdC1zdG9yZQ&shop=bytechef-test-store.myshopify.com&timestamp=1765356985

    private ShopifyConnection() {
    }
}
