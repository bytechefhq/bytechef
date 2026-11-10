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

package com.bytechef.component.facebook.pages.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.ACCESS_TOKEN;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.BASE_URL;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.GRAPH_API_VERSION;

import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FacebookPagesConnection {

    protected static final String TOKEN_URL = BASE_URL + "/oauth/access_token";

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> BASE_URL)
        .help("", "https://docs.bytechef.io/reference/components/facebook-pages_v1#connection-setup")
        .authorizations(
            authorization(OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("App ID")
                        .description("The App ID of your Meta app.")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("App Secret")
                        .description("The App Secret of your Meta app.")
                        .required(true))
                .authorizationUrl(
                    (connectionParameters, context) -> "https://www.facebook.com/" + GRAPH_API_VERSION +
                        "/dialog/oauth")
                .scopes((connectionParameters, context) -> {
                    Map<String, Boolean> scopes = new LinkedHashMap<>();

                    scopes.put("pages_show_list", true);
                    scopes.put("pages_read_engagement", true);
                    scopes.put("pages_manage_posts", true);
                    scopes.put("pages_read_user_content", true);
                    scopes.put("pages_manage_engagement", true);

                    return scopes;
                })
                .tokenUrl((connectionParameters, context) -> TOKEN_URL)
                .authorizationCallback(FacebookPagesConnection::authorizationCallback))
        .version(1);

    private FacebookPagesConnection() {
    }

    protected static AuthorizationCallbackResponse authorizationCallback(
        Parameters connectionParameters, String code, String redirectUri, String codeVerifier, Context context) {

        String clientId = connectionParameters.getRequiredString(CLIENT_ID);
        String clientSecret = connectionParameters.getRequiredString(CLIENT_SECRET);

        Map<String, Object> shortLivedTokenBody = requestToken(
            context, "client_id", clientId, "client_secret", clientSecret, "redirect_uri", redirectUri, "code", code);

        Map<String, Object> longLivedTokenBody = requestToken(
            context, "grant_type", "fb_exchange_token", "client_id", clientId, "client_secret", clientSecret,
            "fb_exchange_token", shortLivedTokenBody.get(ACCESS_TOKEN));

        Long expiresIn = null;

        if (longLivedTokenBody.get("expires_in") instanceof Number number) {
            expiresIn = number.longValue();
        }

        return new AuthorizationCallbackResponse((String) longLivedTokenBody.get(ACCESS_TOKEN), null, expiresIn);
    }

    private static Map<String, Object> requestToken(Context context, Object... queryParameters) {
        Http.Response response = context.http(http -> http.get(TOKEN_URL))
            .queryParameters(queryParameters)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        Map<String, Object> body = response.getBody(new TypeReference<>() {});

        int statusCode = response.getStatusCode();

        if (statusCode >= 400 || body == null || !(body.get(ACCESS_TOKEN) instanceof String)) {
            throw new ProviderException("Unable to get a Facebook access token: " + body);
        }

        return body;
    }
}
