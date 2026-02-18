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

package com.bytechef.component.http.client.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.ADD_TO;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION_URL;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.HEADER_PREFIX;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.SCOPES;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN_URL;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.definition.Context.Http.BodyContentType.FORM_URL_ENCODED;
import static com.bytechef.component.definition.Context.Http.ResponseType.JSON;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpClientConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(BASE_URI)
                .label("Base URI")
                .description("If set, it will be combined HTTP Client Component URI attribute value."))
        .authorizationRequired(false)
        .authorizations(
            authorization(AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(KEY)
                        .label("Key")
                        .required(true)
                        .defaultValue(Authorization.API_TOKEN),
                    string(VALUE)
                        .label("Value")
                        .required(true),
                    string(ADD_TO)
                        .label("Add to")
                        .required(true)
                        .options(
                            option("Header", ApiTokenLocation.HEADER.name()),
                            option("QueryParams", ApiTokenLocation.QUERY_PARAMETERS.name()))),
            authorization(AuthorizationType.BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)),
            authorization(AuthorizationType.DIGEST_AUTH)
                .title("Digest Auth")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)),
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(AUTHORIZATION_URL)
                        .label("Authorization URL")
                        .required(true),
                    string(TOKEN_URL)
                        .label("Token URL")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(HEADER_PREFIX)
                        .label("Header Prefix")
                        .defaultValue(Authorization.BEARER),
                    string(SCOPES)
                        .label("Scopes")
                        .description("Optional comma-delimited list of scopes")
                        .controlType(ControlType.TEXT_AREA)),
            authorization(AuthorizationType.OAUTH2_IMPLICIT_CODE)
                .title("OAuth2 Implicit Code")
                .properties(
                    string(AUTHORIZATION_URL)
                        .label("Authorization URL")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(HEADER_PREFIX)
                        .label("Header Prefix")
                        .defaultValue(Authorization.BEARER),
                    string(SCOPES)
                        .label("Scopes")
                        .description("Optional comma-delimited list of scopes")
                        .controlType(ControlType.TEXT_AREA)),
            authorization(AuthorizationType.OAUTH2_CLIENT_CREDENTIALS)
                .title("OAuth2 Client Credentials")
                .properties(
                    string(TOKEN_URL)
                        .label("Token URL")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(HEADER_PREFIX)
                        .label("Header Prefix")
                        .defaultValue(Authorization.BEARER),
                    string(SCOPES)
                        .label("Scopes")
                        .description("Optional comma-delimited list of scopes")
                        .controlType(ControlType.TEXT_AREA))
                .apply((Parameters connectionParameters, Context context) -> {
                    Map<String, String> formParameters = new LinkedHashMap<>();

                    formParameters.put("grant_type", "client_credentials");

                    String scopes = connectionParameters.getString(SCOPES);

                    if (scopes != null && !scopes.isBlank()) {
                        scopes = scopes.replace(",", " ");

                        formParameters.put("scope", scopes.trim());
                    }

                    String base64Credentials = context.encoder(
                        encoder -> encoder.base64Encode(
                            connectionParameters.getString(CLIENT_ID), ":",
                            connectionParameters.getString(CLIENT_SECRET)));

                    String accessToken = context.http(http -> {
                        Context.Http.Response tokenResponse =
                            http.post(connectionParameters.getRequiredString(TOKEN_URL))
                                .body(Body.of(formParameters, FORM_URL_ENCODED))
                                .header("Authorization", "Basic: " + base64Credentials)
                                .configuration(
                                    responseType(JSON)
                                        .disableAuthorization(true))
                                .execute();

                        Map<String, Object> responseBody = tokenResponse.getBody(Map.class);

                        return (String) responseBody.get(ACCESS_TOKEN);
                    });

                    return Authorization.ApplyResponse.ofHeaders(
                        Map.of(AUTHORIZATION, List.of(
                            connectionParameters.getString(HEADER_PREFIX, Authorization.BEARER) + " " + accessToken)));
                }));
}
