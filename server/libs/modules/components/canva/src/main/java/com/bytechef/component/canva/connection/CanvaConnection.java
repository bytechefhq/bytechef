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
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class CanvaConnection {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CODE_VERIFIER = generateCodeVerifier();

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.canva.com/rest/v1")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
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

                    map.put("asset:read", true);
                    map.put("asset:write", true);
                    map.put("design:content:read", true);
                    map.put("design:content:write", true);
                    map.put("design:meta:read", true);

                    return map;
                })
                .oAuth2AuthorizationExtraQueryParameters((connection, context) -> {
                    Map<String, String> params = new LinkedHashMap<>();

                    String codeChallenge = generateCodeChallenge();

                    params.put("code_challenge", codeChallenge);
                    params.put("code_challenge_method", "S256");
                    params.put("response_type", "code");

                    return params;
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
                                        "code_verifier", CODE_VERIFIER,
                                        "code", code,
                                        "redirect_uri", redirectUri),
                                    BodyContentType.FORM_URL_ENCODED))
                            .configuration(Http.responseType(Http.ResponseType.JSON))
                            .execute();

                    return new AuthorizationCallbackResponse(response.getBody(new TypeReference<>() {}));
                }))
        .version(1)
        .help("", "https://docs.bytechef.io/reference/components/canva_v1#connection-setup");

    private static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    private static String generateCodeChallenge() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(CODE_VERIFIER.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(hash);
    }

    private CanvaConnection() {
    }
}
