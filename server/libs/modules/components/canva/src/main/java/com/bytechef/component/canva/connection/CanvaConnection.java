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

package com.bytechef.component.canva.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class CanvaConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.canva.com/rest/v1")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://www.canva.com/api/oauth/authorize")
                .tokenUrl((connection, context) -> "https://api.canva.com/rest/v1/oauth/token")
                .refreshUrl((connection, context) -> "https://api.canva.com/rest/v1/oauth/token")
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new LinkedHashMap<>();

                    map.put("app:read", false);
                    map.put("app:write", false);
                    map.put("asset:read", true);
                    map.put("asset:write", true);
                    map.put("brandtemplate:content:read", false);
                    map.put("brandtemplate:content:write", false);
                    map.put("brandtemplate:meta:read", false);
                    map.put("brandtemplate:meta:write", false);
                    map.put("comment:read", false);
                    map.put("comment:write", false);
                    map.put("design:content:read", true);
                    map.put("design:content:write", true);
                    map.put("design:meta:read", true);
                    map.put("design:meta:write", false);
                    map.put("design:permission:read", false);
                    map.put("design:permission:write", false);
                    map.put("folder:read", false);
                    map.put("folder:write", false);
                    map.put("folder:permission:read", false);
                    map.put("folder:permission:write", false);
                    map.put("profile:read", false);
                    map.put("profile:write", false);

                    return map;
                })
                .authorizationCallback((connection, code, redirectUri, verifier, context) -> {
                    String clientId = connection.getString(CLIENT_ID);
                    String clientSecret = connection.getString(CLIENT_SECRET);
                    String valueToEncode = clientId + ":" + clientSecret;
                    String encode = context.encoder(
                        encoder -> encoder.base64Encode(valueToEncode.getBytes(StandardCharsets.UTF_8)));

                    Http.Response response =
                        context.http(http -> http.post("https://api.canva.com/rest/v1/oauth/token"))
                            .headers(Map.of(
                                "Content-Type", List.of("application/x-www-form-urlencoded"),
                                "Authorization", List.of("Basic " + encode)))
                            .body(
                                Body.of(
                                    Map.of(
                                        "grant_type", "authorization_code",
                                        "code_verifier", verifier,
                                        "code", code,
                                        "redirect_uri", redirectUri),
                                    BodyContentType.FORM_URL_ENCODED))
                            .configuration(Http.responseType(Http.ResponseType.JSON))
                            .execute();

                    return new AuthorizationCallbackResponse(response.getBody(new TypeReference<>() {}));
                }))
        .version(1)
        .help("", "https://docs.bytechef.io/reference/components/canva_v1#connection-setup");

    private CanvaConnection() {
    }
}
