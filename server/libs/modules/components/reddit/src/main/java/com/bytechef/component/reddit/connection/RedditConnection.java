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

package com.bytechef.component.reddit.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.ConfigurationException;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class RedditConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://oauth.reddit.com")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://www.reddit.com/api/v1/authorize")
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new HashMap<>();

                    map.put("identity", true);
                    map.put("edit", false);
                    map.put("flair", false);
                    map.put("history", false);
                    map.put("modconfig", false);
                    map.put("modflair", false);
                    map.put("modlog", false);
                    map.put("modposts", false);
                    map.put("modwiki", false);
                    map.put("mysubreddits", false);
                    map.put("privatemessages", false);
                    map.put("read", true);
                    map.put("report", false);
                    map.put("save", false);
                    map.put("submit", true);
                    map.put("subscribe", false);
                    map.put("vote", false);
                    map.put("wikiedit", false);
                    map.put("wikiread", false);

                    return map;
                })
                .tokenUrl((connectionParameters, context) -> "https://www.reddit.com/api/v1/access_token")
                .refreshUrl((connectionParameters, context) -> "https://www.reddit.com/api/v1/access_token")
                .authorizationCallback((connectionParameters, code, redirectUri, codeVerifier, context) -> {
                    String clientId = connectionParameters.getString(CLIENT_ID);
                    String clientSecret = connectionParameters.getString(CLIENT_SECRET);
                    String valueToEncode = clientId + ":" + clientSecret;
                    String encoded = context.encoder(
                        encoder -> encoder.base64Encode(valueToEncode.getBytes(StandardCharsets.UTF_8)));

                    Http.Response response =
                        context.http(http -> http.post("https://www.reddit.com/api/v1/access_token")
                            .queryParameters(
                                "grant_type", "authorization_code",
                                "code", code,
                                "redirect_uri", redirectUri)
                            .headers(
                                Map.of(
                                    "Accept", List.of("application/json"),
                                    "Content-Type", List.of("application/x-www-form-urlencoded"),
                                    "Authorization", List.of("Basic " + encoded)))
                            .configuration(responseType(ResponseType.JSON))
                            .execute());

                    if (response.getStatusCode() < 200 || response.getStatusCode() > 299
                        || response.getBody() == null) {

                        throw new ConfigurationException("Invalid claim");
                    }

                    return new AuthorizationCallbackResponse(response.getBody(new TypeReference<>() {}));
                }));

    private RedditConnection() {
    }
}
