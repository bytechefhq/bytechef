
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.Authorization.ApplyConsumer;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.Authorization.ClientSecretFunction;
import com.bytechef.hermes.component.definition.Authorization.TokenUrlFunction;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition.BaseUriFunction;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationUrlFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientIdFunction;
import com.bytechef.hermes.component.definition.Authorization.ScopesFunction;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.util.AuthorizationUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bytechef.hermes.component.util.HttpClientUtils.responseFormat;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionServiceImpl implements ConnectionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public ConnectionDefinitionServiceImpl(ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public boolean connectionExists(String componentName, int connectionVersion) {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .anyMatch(componentDefinition -> {
                ConnectionDefinition connectionDefinition = OptionalUtils.get(componentDefinition.getConnection());

                return componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion;
            });
    }

    @Override
    public void executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName,
        AuthorizationContext authorizationContext) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        ApplyConsumer applyConsumer = OptionalUtils.orElse(
            authorization.getApply(), AuthorizationUtils.getDefaultApply(authorization.getType()));

        applyConsumer.accept(connectionParameters, authorizationContext);
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName,
        String redirectUri) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        AuthorizationCallbackFunction authorizationCallbackFunction =
            OptionalUtils.orElse(
                authorization.getAuthorizationCallback(),
                getDefaultAuthorizationCallback(
                    OptionalUtils.orElse(authorization.getClientId(), AuthorizationUtils::getDefaultClientId),
                    OptionalUtils.orElse(authorization.getClientSecret(), AuthorizationUtils::getDefaultClientSecret),
                    OptionalUtils.orElse(authorization.getTokenUrl(), AuthorizationUtils::getDefaultTokenUrl)));

        Authorization.PkceFunction pkceFunction = OptionalUtils.orElse(
            authorization.getPkce(), AuthorizationUtils.getDefaultPkce());

        String verifier = null;

        if (authorization.getType() == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

            // TODO pkce
            Authorization.Pkce pkce = pkceFunction.apply(null, null, "SHA256");

            verifier = pkce.verifier();
        }

        return authorizationCallbackFunction.apply(
            connectionParameters, MapValueUtils.getString(connectionParameters, Authorization.CODE), redirectUri,
            verifier);
    }

    private AuthorizationCallbackFunction getDefaultAuthorizationCallback(
        ClientIdFunction clientIdFunction, ClientSecretFunction clientSecretFunction,
        TokenUrlFunction tokenUrlFunction) {

        return (connectionParameters, code, redirectUri, codeVerifier) -> {
            Map<String, Object> payload = new HashMap<>() {
                {
                    put("client_id", clientIdFunction.apply(connectionParameters));
                    put("client_secret", clientSecretFunction.apply(connectionParameters));
                    put("code", code);
                    put("grant_type", "authorization_code");
                    put("redirect_uri", redirectUri);
                }
            };

            if (codeVerifier != null) {
                payload.put("code_verifier", codeVerifier);
            }

            HttpClientUtils.Response response = HttpClientUtils.post(tokenUrlFunction.apply(connectionParameters))
                .body(
                    HttpClientUtils.Body.of(payload, HttpClientUtils.BodyContentType.FORM_URL_ENCODED))
                .configuration(responseFormat(HttpClientUtils.ResponseFormat.JSON))
                .execute();

            if (response.statusCode() != 200) {
                throw new ComponentExecutionException("Invalid claim");
            }

            if (response.body() == null) {
                throw new ComponentExecutionException("Invalid claim");
            }

            Map<?, ?> body = (Map<?, ?>) response.body();

            return new AuthorizationCallbackResponse(
                (String) body.get(Authorization.ACCESS_TOKEN), (String) body.get(Authorization.REFRESH_TOKEN));
        };
    }

    @Override
    public Optional<String> fetchBaseUri(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters) {

        ConnectionDefinition connectionDefinition = componentDefinitionRegistry.getComponentConnectionDefinition(
            componentName, connectionVersion);

        BaseUriFunction baseUriFunction =
            OptionalUtils.orElse(connectionDefinition.getBaseUri(), AuthorizationUtils::getDefaultBaseUri);

        return Optional.ofNullable(baseUriFunction.apply(connectionParameters));
    }

    @Override
    public AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        return authorization.getType();
    }

    @Override
    public ConnectionDefinitionDTO getConnectionDefinition(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return toConnectionDefinitionDTO(OptionalUtils.get(componentDefinition.getConnection()));
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getConnection()))
            .map(componentDefinition -> toConnectionDefinitionDTO(
                OptionalUtils.get(componentDefinition.getConnection())))
            .toList();
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, ?> connectionInputParameters,
        String authorizationName) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        AuthorizationUrlFunction authorizationUrlFunction = OptionalUtils.orElse(
            authorization.getAuthorizationUrl(), AuthorizationUtils::getDefaultAuthorizationUrl);
        ClientIdFunction clientIdFunction = OptionalUtils.orElse(
            authorization.getClientId(), AuthorizationUtils::getDefaultClientId);
        ScopesFunction scopesFunction = OptionalUtils.orElse(
            authorization.getScopes(), AuthorizationUtils::getDefaultScopes);

        return new OAuth2AuthorizationParametersDTO(
            authorizationUrlFunction.apply(connectionInputParameters),
            clientIdFunction.apply(connectionInputParameters),
            scopesFunction.apply(connectionInputParameters));
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions(String componentName, int componentVersion) {
        return componentDefinitionRegistry.getConnectionDefinitions(componentName, componentVersion)
            .stream()
            .map(this::toConnectionDefinitionDTO)
            .toList();
    }

    private ConnectionDefinitionDTO toConnectionDefinitionDTO(ConnectionDefinition connectionDefinition) {
        return new ConnectionDefinitionDTO(connectionDefinition);
    }
}
