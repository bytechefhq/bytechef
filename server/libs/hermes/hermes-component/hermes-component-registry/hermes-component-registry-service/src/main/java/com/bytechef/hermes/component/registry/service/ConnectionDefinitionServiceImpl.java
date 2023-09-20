
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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.ApplyFunction;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationUrlFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientIdFunction;
import com.bytechef.hermes.component.definition.Authorization.PkceFunction;
import com.bytechef.hermes.component.definition.Authorization.ScopesFunction;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.registry.ComponentDefinitionRegistry;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.util.ConnectionDefinitionUtils;
import com.bytechef.hermes.component.definition.ParameterMapImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.bytechef.hermes.component.definition.ConnectionDefinition.BaseUriFunction;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.CODE;

/**
 * @author Ivica Cardic
 */
@Service("connectionDefinitionService")
public class ConnectionDefinitionServiceImpl implements ConnectionDefinitionService, RemoteConnectionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public ConnectionDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ObjectMapper objectMapper) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.objectMapper = objectMapper;
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
    public ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.version(), connection.authorizationName());

        ApplyFunction applyFunction = OptionalUtils.orElse(
            authorization.getApply(), ConnectionDefinitionUtils.getDefaultApply(authorization.getType()));

        return applyFunction.apply(new ParameterMapImpl(connection.parameters()), context);
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context,
        @NonNull String redirectUri) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.version(), connection.authorizationName());
        String verifier = null;

        if (authorization.getType() == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {
            PkceFunction pkceFunction = OptionalUtils.orElse(
                authorization.getPkce(), ConnectionDefinitionUtils.getDefaultPkce());

            // TODO pkce
            Authorization.Pkce pkce = pkceFunction.apply(null, null, "SHA256", context);

            verifier = pkce.verifier();
        }

        AuthorizationCallbackFunction authorizationCallbackFunction = OptionalUtils.orElse(
            authorization.getAuthorizationCallback(),
            ConnectionDefinitionUtils.getDefaultAuthorizationCallbackFunction(
                OptionalUtils.orElse(
                    authorization.getClientId(),
                    (connectionParameters, context1) -> ConnectionDefinitionUtils.getDefaultClientId(
                        connectionParameters)),
                OptionalUtils.orElse(
                    authorization.getClientSecret(),
                    (connectionParameters, context1) -> ConnectionDefinitionUtils.getDefaultClientSecret(
                        connectionParameters)),
                OptionalUtils.orElse(
                    authorization.getTokenUrl(),
                    (connectionParameters, context1) -> ConnectionDefinitionUtils.getDefaultTokenUrl(
                        connectionParameters)),
                objectMapper));

        return authorizationCallbackFunction.apply(
            new ParameterMapImpl(connection.parameters()), MapUtils.getString(connection.parameters(), CODE),
            redirectUri, verifier, context);
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        com.bytechef.hermes.component.definition.ConnectionDefinition connectionDefinition =
            componentDefinitionRegistry.getConnectionDefinition(componentName, connection.version());

        BaseUriFunction baseUriFunction =
            OptionalUtils.orElse(
                connectionDefinition.getBaseUri(),
                (connectionParameters, context1) -> ConnectionDefinitionUtils.getDefaultBaseUri(connectionParameters));

        return Optional.ofNullable(
            baseUriFunction.apply(
                new ParameterMapImpl(
                    connection.parameters()),
                context));
    }

    @Override
    public AuthorizationType getAuthorizationType(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        return authorization.getType();
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(
        @NonNull String componentName, int componentVersion) {

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
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connection.version(), connection.authorizationName());

        AuthorizationUrlFunction authorizationUrlFunction = OptionalUtils.orElse(
            authorization.getAuthorizationUrl(),
            (connectionParameters, context1) -> ConnectionDefinitionUtils.getDefaultAuthorizationUrl(
                connectionParameters));
        ClientIdFunction clientIdFunction = OptionalUtils.orElse(
            authorization.getClientId(),
            (connectionParameters, context1) -> ConnectionDefinitionUtils.getDefaultClientId(connectionParameters));
        ScopesFunction scopesFunction = OptionalUtils.orElse(
            authorization.getScopes(),
            (connectionParameters, context1) -> ConnectionDefinitionUtils.getDefaultScopes(connectionParameters));

        ParameterMapImpl connectionParameters = new ParameterMapImpl(connection.parameters());

        return new OAuth2AuthorizationParameters(
            authorizationUrlFunction.apply(connectionParameters, context),
            clientIdFunction.apply(connectionParameters, context), scopesFunction.apply(connectionParameters, context));
    }

    @Override
    public List<ConnectionDefinition>
        getConnectionDefinitions(@NonNull String componentName, @NonNull Integer componentVersion) {
        return componentDefinitionRegistry.getConnectionDefinitions(componentName, componentVersion)
            .stream()
            .map(ConnectionDefinition::new)
            .toList();
    }
}
