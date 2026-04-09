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

package com.bytechef.component.canva;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@AutoService(OpenApiComponentHandler.class)
public class CanvaComponentHandler extends AbstractCanvaComponentHandler {

    private static final String CODE_VERIFIER = generateCodeVerifier();

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        Optional<List<? extends Authorization>> optionalAuthorizations =
            modifiableConnectionDefinition.getAuthorizations();

        if (optionalAuthorizations.isPresent()) {
            List<? extends Authorization> authorizations = optionalAuthorizations.get();
            ModifiableAuthorization auth = (ModifiableAuthorization) authorizations.getFirst();

            auth
                .authorizationUrl((connection, context) -> "https://www.canva.com/api/oauth/authorize")
                .scopes((connection, context) -> {
                    Map<String, Boolean> scopes = new LinkedHashMap<>();
                    scopes.put("design:content:read", true);
                    scopes.put("design:content:write", true);
                    scopes.put("asset:read", true);
                    return scopes;
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
                });
        }

        return modifiableConnectionDefinition
            .help("", "https://docs.bytechef.io/reference/components/canva_v1#connection-setup")
            .version(1);
    }

    private static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    private static String generateCodeChallenge() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(CanvaComponentHandler.CODE_VERIFIER.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(hash);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        String actionDefinitionName = actionDefinition.getName();

        if (actionDefinitionName.equals("createDesign") && Objects.equals(modifiableProperty.getName(), "design_type")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "name")) {
                    ((ModifiableStringProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("design_type.type", "preset"));
                } else if (Objects.equals(baseProperty.getName(), "width")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("design_type.type", "custom"));
                } else if (Objects.equals(baseProperty.getName(), "height")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("design_type.type", "custom"));
                }
            }
        }

        return modifiableProperty;
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/canva.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
            .customActionHelp("Canva Web API documentation", "https://www.canva.dev/docs/connect/")
            .version(1);
    }
}
