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

package com.bytechef.component.instagram.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.BEARER;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.instagram.constant.InstagramConstants.BASE_URL;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class InstagramConnection {

    protected static final String AUTHORIZATION_URL = "https://www.instagram.com/oauth/authorize";
    protected static final String LONG_LIVED_TOKEN_URL = "https://graph.instagram.com/access_token";
    protected static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> BASE_URL)
        .help("", "https://docs.bytechef.io/reference/components/instagram_v1#connection-setup")
        .authorizations(
            authorization(OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Instagram App ID")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Instagram App Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> AUTHORIZATION_URL)
                .scopes((connectionParameters, context) -> {
                    Map<String, Boolean> scopes = new LinkedHashMap<>();

                    scopes.put("instagram_business_basic", true);
                    scopes.put("instagram_business_content_publish", true);
                    scopes.put("instagram_business_manage_comments", true);

                    return scopes;
                })
                .tokenUrl((connectionParameters, context) -> TOKEN_URL)
                .authorizationCallback(InstagramConnection::authorizationCallback)
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        AUTHORIZATION,
                        List.of(BEARER + " " + connectionParameters.getRequiredString(ACCESS_TOKEN))))))
        .version(1);

    private InstagramConnection() {
    }

    protected static AuthorizationCallbackResponse authorizationCallback(
        Parameters connectionParameters, String code, String redirectUri, String codeVerifier, Context context) {

        String clientSecret = connectionParameters.getRequiredString(CLIENT_SECRET);

        Map<String, Object> shortLivedTokenMap = context.http(http -> http.post(TOKEN_URL))
            .body(
                Body.of(
                    Map.of(
                        "client_id", connectionParameters.getRequiredString(CLIENT_ID),
                        "client_secret", clientSecret,
                        "code", code,
                        "grant_type", "authorization_code",
                        "redirect_uri", redirectUri),
                    BodyContentType.FORM_URL_ENCODED))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String shortLivedAccessToken = getAccessToken(shortLivedTokenMap);

        Map<String, Object> longLivedTokenMap = context.http(http -> http.get(LONG_LIVED_TOKEN_URL))
            .queryParameters(
                "grant_type", "ig_exchange_token",
                "client_secret", clientSecret,
                ACCESS_TOKEN, shortLivedAccessToken)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String longLivedAccessToken = getAccessToken(longLivedTokenMap);

        Long expiresIn = null;

        if (longLivedTokenMap.get("expires_in") instanceof Number number) {
            expiresIn = number.longValue();
        }

        return new AuthorizationCallbackResponse(longLivedAccessToken, null, expiresIn);
    }

    private static String getAccessToken(Map<String, Object> tokenMap) {
        if (tokenMap == null || !(tokenMap.get("access_token") instanceof String accessToken)) {
            throw new ProviderException("Instagram did not return an access token: " + tokenMap);
        }

        return accessToken;
    }
}
