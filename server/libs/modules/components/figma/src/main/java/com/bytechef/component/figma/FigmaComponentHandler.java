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

package com.bytechef.component.figma;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import com.google.auto.service.AutoService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.naming.ConfigurationException;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class FigmaComponentHandler extends AbstractFigmaComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/figma.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://api.figma.com")
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
                    .authorizationUrl((connectionParameters, context) -> "https://www.figma.com/oauth")
                    .scopes((connection, context) -> List.of("file_comments:write", "files:read", "webhooks:write"))
                    .tokenUrl((connectionParameters, context) -> "https://api.figma.com/v1/oauth/token")
                    .refreshUrl((connectionParameters, context) -> "https://api.figma.com/v1/oauth/refresh")
                    .authorizationCallback((connectionParameters, code, redirectUri, codeVerifier, context) -> {
                        String clientId = connectionParameters.getString(CLIENT_ID);
                        String clientSecret = connectionParameters.getString(CLIENT_SECRET);
                        String valueToEncode = clientId + ":" + clientSecret;
                        String encode = context.encoder(
                            encoder -> encoder.base64EncodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8)));

                        Http.Response response = context.http(http -> http.post("https://api.figma.com/v1/oauth/token"))
                            .headers(
                                Map.of(
                                    "Accept", List.of("application/json"),
                                    "Content-Type", List.of("application/x-www-form-urlencoded"),
                                    AUTHORIZATION, List.of("Basic " + encode)))
                            .body(Http.Body.of(
                                Map.of(
                                    "code", code,
                                    "redirect_uri", redirectUri,
                                    "grant_type", "authorization_code")))
                            .configuration(Http.responseType(Http.ResponseType.JSON))
                            .execute();

                        if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
                            throw new ConfigurationException("Invalid claim");
                        }

                        if (response.getBody() == null) {
                            throw new ConfigurationException("Invalid claim");
                        }

                        return new AuthorizationCallbackResponse(response.getBody(new TypeReference<>() {}));
                    }));
    }
}
