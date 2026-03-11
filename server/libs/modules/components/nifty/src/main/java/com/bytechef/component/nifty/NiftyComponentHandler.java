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

package com.bytechef.component.nifty;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT_PROPERTY;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.nifty.trigger.NiftyNewTaskTrigger;
import com.google.auto.service.AutoService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.naming.ConfigurationException;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class NiftyComponentHandler extends AbstractNiftyComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(NiftyNewTaskTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();
            List<Property> properties = new ArrayList<>(propertiesOptional.orElse(Collections.emptyList()));

            if (Objects.equals(modifiableActionDefinition.getName(), "createTask")) {
                properties.addFirst(PROJECT_PROPERTY);
            }
            modifiableActionDefinition.properties(properties);
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/nifty.svg")
            .categories(ComponentCategory.PROJECT_MANAGEMENT, ComponentCategory.PRODUCTIVITY_AND_COLLABORATION);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        modifiableConnectionDefinition.help("",
            "https://docs.bytechef.io/reference/components/nifty_v1#connection-setup");

        Optional<List<? extends Authorization>> optionalAuthorizations =
            modifiableConnectionDefinition.getAuthorizations();

        if (optionalAuthorizations.isPresent()) {
            List<? extends Authorization> authorizations = optionalAuthorizations.get();
            ModifiableAuthorization modifiableAuthorization = (ModifiableAuthorization) authorizations.getFirst();

            modifiableAuthorization
                .authorizationCallback((connectionParameters, code, redirectUri, codeVerifier, context) -> {
                    String clientId = connectionParameters.getString(CLIENT_ID);
                    String clientSecret = connectionParameters.getString(CLIENT_SECRET);
                    String valueToEncode = clientId + ":" + clientSecret;
                    String encode = context.encoder(
                        encoder -> encoder.base64Encode(valueToEncode.getBytes(StandardCharsets.UTF_8)));

                    Http.Response response = context.http(http -> http.post("https://openapi.niftypm.com/oauth/token"))
                        .headers(
                            Map.of(
                                "Content-Type", List.of("application/json"),
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
                })
                .refresh((connectionParameters, context) -> {
                    String clientId = connectionParameters.getString(CLIENT_ID);
                    String clientSecret = connectionParameters.getString(CLIENT_SECRET);
                    String valueToEncode = clientId + ":" + clientSecret;
                    String encode = context.encoder(
                        encoder -> encoder.base64Encode(valueToEncode.getBytes(StandardCharsets.UTF_8)));


                    Http.Response response = context.http(http -> http.post("https://openapi.niftypm.com/oauth/token"))
                        .headers(
                            Map.of(
                                "Content-Type", List.of("application/json"),
                                AUTHORIZATION, List.of("Basic " + encode)))
                        .body(
                            Http.Body.of(
                                "grant_type", "refresh_token",
                                "refresh_token", connectionParameters.getString("refresh_token")))
                        .configuration(Http.responseType(Http.ResponseType.JSON))
                        .execute();

                    if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
                        throw new ConfigurationException(
                            "OAuth provider rejected token refresh request");
                    }

                    if (response.getBody() == null) {
                        throw new ConfigurationException("Invalid claim");
                    }

                    Map<String, Object> responseMap = response.getBody(new TypeReference<>() {});

                    return new Authorization.RefreshTokenResponse(
                        (String) responseMap.get(Authorization.ACCESS_TOKEN),
                        (String) responseMap.get(Authorization.REFRESH_TOKEN),
                        responseMap.containsKey(Authorization.EXPIRES_IN)
                            ? Long.valueOf((Integer) responseMap.get(Authorization.EXPIRES_IN)) : null);
                });
        }

        return modifiableConnectionDefinition;
    }
}
