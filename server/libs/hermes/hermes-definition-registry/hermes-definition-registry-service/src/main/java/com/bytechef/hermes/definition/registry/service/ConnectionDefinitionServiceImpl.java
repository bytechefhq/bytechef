
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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.ApplyFunction;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationUrlFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientIdFunction;
import com.bytechef.hermes.component.definition.Authorization.PkceFunction;
import com.bytechef.hermes.component.definition.Authorization.ScopesFunction;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.definition.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.definition.registry.util.AuthorizationUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            .map(ComponentDefinition::getConnection)
            .flatMap(Optional::stream)
            .anyMatch(connectionDefinition -> componentName.equalsIgnoreCase(connectionDefinition.getComponentName()) &&
                connectionDefinition.getVersion() == connectionVersion);
    }

    @Override
    public Authorization.ApplyResponse executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        ApplyFunction applyFunction = OptionalUtils.orElse(
            authorization.getApply(), AuthorizationUtils.getDefaultApply(authorization.getType()));

        return applyFunction.apply(connectionParameters);
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName,
        String redirectUri) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);
        String verifier = null;

        if (authorization.getType() == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {
            PkceFunction pkceFunction = OptionalUtils.orElse(
                authorization.getPkce(), AuthorizationUtils.getDefaultPkce());

            // TODO pkce
            Authorization.Pkce pkce = pkceFunction.apply(null, null, "SHA256");

            verifier = pkce.verifier();
        }

        AuthorizationCallbackFunction authorizationCallbackFunction = OptionalUtils.orElse(
            authorization.getAuthorizationCallback(),
            AuthorizationUtils.getDefaultAuthorizationCallbackFunction(
                OptionalUtils.orElse(authorization.getClientId(), AuthorizationUtils::getDefaultClientId),
                OptionalUtils.orElse(authorization.getClientSecret(), AuthorizationUtils::getDefaultClientSecret),
                OptionalUtils.orElse(authorization.getTokenUrl(), AuthorizationUtils::getDefaultTokenUrl)));

        return authorizationCallbackFunction.apply(
            connectionParameters, MapUtils.getString(connectionParameters, Authorization.CODE), redirectUri,
            verifier);
    }

    @Override
    public Optional<String> executeBaseUri(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters) {

        com.bytechef.hermes.component.definition.ConnectionDefinition connectionDefinition =
            componentDefinitionRegistry.getComponentConnectionDefinition(componentName, connectionVersion);

        com.bytechef.hermes.component.definition.ConnectionDefinition.BaseUriFunction baseUriFunction =
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
    public ConnectionDefinition getConnectionDefinition(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return new ConnectionDefinition(OptionalUtils.get(componentDefinition.getConnection()));
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getConnection()))
            .map(componentDefinition -> new ConnectionDefinition(
                OptionalUtils.get(componentDefinition.getConnection())))
            .toList();
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters,
        String authorizationName) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        AuthorizationUrlFunction authorizationUrlFunction = OptionalUtils.orElse(
            authorization.getAuthorizationUrl(), AuthorizationUtils::getDefaultAuthorizationUrl);
        ClientIdFunction clientIdFunction = OptionalUtils.orElse(
            authorization.getClientId(), AuthorizationUtils::getDefaultClientId);
        ScopesFunction scopesFunction = OptionalUtils.orElse(
            authorization.getScopes(), AuthorizationUtils::getDefaultScopes);

        return new OAuth2AuthorizationParameters(
            authorizationUrlFunction.apply(connectionParameters),
            clientIdFunction.apply(connectionParameters),
            scopesFunction.apply(connectionParameters));
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(String componentName, Integer componentVersion) {
        return componentDefinitionRegistry.getConnectionDefinitions(componentName, componentVersion)
            .stream()
            .map(ConnectionDefinition::new)
            .toList();
    }
}
