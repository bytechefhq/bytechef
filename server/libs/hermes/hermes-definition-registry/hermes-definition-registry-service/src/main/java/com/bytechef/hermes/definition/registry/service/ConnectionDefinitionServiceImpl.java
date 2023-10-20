
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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.InputParametersImpl;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationUrlFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientIdFunction;
import com.bytechef.hermes.component.definition.Authorization.ScopesFunction;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.definition.registry.dto.AuthorizationDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
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
            .anyMatch(componentDefinition -> {
                ConnectionDefinition connectionDefinition = OptionalUtils.get(componentDefinition.getConnection());

                return componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion;
            });
    }

    @Override
    public void executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters,
        String authorizationName, AuthorizationContext authorizationContext) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        Authorization.ApplyConsumer applyConsumer = authorization.getApply();

        // TODO Add url and httpVerb values
        applyConsumer.accept(new InputParametersImpl(connectionParameters), authorizationContext, null, null);
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters, String authorizationName,
        String redirectUri) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        AuthorizationCallbackFunction authorizationCallbackFunction = authorization.getAuthorizationCallback();

        InputParameters inputParameters = new InputParametersImpl(connectionParameters);

        return authorizationCallbackFunction.apply(
            inputParameters, inputParameters.getString(Authorization.CODE), redirectUri, null /*
                                                                                               * TODO pkce verifier
                                                                                               */);
    }

    @Override
    public Optional<String> fetchBaseUri(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters) {

        ConnectionDefinition connectionDefinition = componentDefinitionRegistry.getComponentConnectionDefinition(
            componentName, connectionVersion);

        ConnectionDefinition.BaseUriFunction baseUriFunction = connectionDefinition.getBaseUri();

        return Optional.ofNullable(baseUriFunction.apply(new InputParametersImpl(connectionParameters)));
    }

    @Override
    public Authorization.AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        return authorization.getType();
    }

    @Override
    public ConnectionDefinitionDTO getConnectionDefinition(
        String componentName, int componentVersion) {
        return toConnectionDefinitionDTO(
            componentDefinitionRegistry.getConnectionDefinition(componentName, componentVersion));
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions() {
        return componentDefinitionRegistry.getConnectionDefinitions()
            .stream()
            .map(this::toConnectionDefinitionDTO)
            .toList();
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters,
        String authorizationName) {

        Authorization authorization = componentDefinitionRegistry.getAuthorization(
            componentName, connectionVersion, authorizationName);

        AuthorizationUrlFunction authorizationUrlFunction = authorization.getAuthorizationUrl();
        ClientIdFunction clientIdFunction = authorization.getClientId();
        ScopesFunction scopesFunction = authorization.getScopes();

        return new OAuth2AuthorizationParametersDTO(
            authorizationUrlFunction.apply(new InputParametersImpl(connectionParameters)),
            clientIdFunction.apply(new InputParametersImpl(connectionParameters)),
            scopesFunction.apply(new InputParametersImpl(connectionParameters)));
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions(String componentName, int componentVersion) {
        return CollectionUtils.map(
            componentDefinitionRegistry.getConnectionDefinitions(componentName, componentVersion),
            this::toConnectionDefinitionDTO);
    }

    private List<AuthorizationDTO> toAuthorizationDTOs(List<? extends Authorization> authorizations) {
        return authorizations.stream()
            .map(authorization -> new AuthorizationDTO(
                OptionalUtils.orElse(authorization.getDescription(), null), authorization.getName(),
                authorization.getProperties(), authorization.getTitle(), authorization.getType()))
            .toList();
    }

    private ConnectionDefinitionDTO toConnectionDefinitionDTO(ConnectionDefinition connectionDefinition) {
        return new ConnectionDefinitionDTO(
            connectionDefinition.isAuthorizationRequired(),
            toAuthorizationDTOs(
                OptionalUtils.orElse(connectionDefinition.getAuthorizations(), Collections.emptyList())),
            OptionalUtils.orElse(connectionDefinition.getDescription(), null), connectionDefinition.getName(),
            OptionalUtils.orElse(connectionDefinition.getProperties(), Collections.emptyList()),
            connectionDefinition.getTitle(), connectionDefinition.getVersion());
    }
}
