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
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.figma.trigger.FigmaNewCommentTrigger;
import com.google.auto.service.AutoService;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.naming.ConfigurationException;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class FigmaComponentHandler extends AbstractFigmaComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(FigmaNewCommentTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            if (name.equals("getComments")) {
                actionDefinition.help("", "https://docs.bytechef.io/reference/components/figma_v1#get-comments");
            } else if (name.equals("createComment")) {
                actionDefinition.help("", "https://docs.bytechef.io/reference/components/figma_v1#post-comment");
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .customActionHelp("", "https://developers.figma.com/docs/rest-api/")
            .icon("path:assets/figma.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        modifiableConnectionDefinition.version(1);
        modifiableConnectionDefinition.help("",
            "https://docs.bytechef.io/reference/components/figma_v1#connection-setup");

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
                })
                .scopes((connectionParameters, context) -> {
                    Map<String, Boolean> scopeMap = new HashMap<>();

                    scopeMap.put("current_user:read", false);
                    scopeMap.put("file_comments:read", true);
                    scopeMap.put("file_comments:write", true);
                    scopeMap.put("file_content:read", false);
                    scopeMap.put("file_dev_resources:read", false);
                    scopeMap.put("file_dev_resources:write", false);
                    scopeMap.put("file_metadata:read", false);
                    scopeMap.put("file_variables:read", false);
                    scopeMap.put("file_variables:write", false);
                    scopeMap.put("file_versions:read", false);
                    scopeMap.put("library_analytics:read", false);
                    scopeMap.put("library_assets:read", false);
                    scopeMap.put("library_content:read", false);
                    scopeMap.put("org:activity_log_read", false);
                    scopeMap.put("org:discovery_read", false);
                    scopeMap.put("projects:read", false);
                    scopeMap.put("selections:read", false);
                    scopeMap.put("team_library_content:read", false);
                    scopeMap.put("webhooks:read", false);
                    scopeMap.put("webhooks:write", true);

                    return scopeMap;
                });
        }

        return modifiableConnectionDefinition;
    }
}
