
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
import com.bytechef.hermes.definition.registry.component.InputParametersImpl;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackFunction;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationUrlFunction;
import com.bytechef.hermes.component.definition.Authorization.ClientIdFunction;
import com.bytechef.hermes.component.definition.Authorization.ScopesFunction;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.definition.registry.dto.AuthorizationDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionServiceImpl implements ConnectionDefinitionService {

    private final List<ComponentDefinition> componentDefinitions;
    private final List<ConnectionDefinition> connectionDefinitions;

    @SuppressFBWarnings("EI2")
    public ConnectionDefinitionServiceImpl(List<ComponentDefinition> componentDefinitions) {
        this.componentDefinitions = componentDefinitions;
        this.connectionDefinitions = componentDefinitions.stream()
            .map(componentDefinition -> OptionalUtils.orElse(componentDefinition.getConnection(), null))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @Override
    public boolean connectionExists(String componentName, int connectionVersion) {
        return componentDefinitions.stream()
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

        Authorization authorization = getAuthorization(componentName, connectionVersion, authorizationName);

        Authorization.ApplyConsumer applyConsumer = authorization.getApply();

        // TODO Add url and httpVerb values
        applyConsumer.accept(new InputParametersImpl(connectionParameters), authorizationContext, null, null);
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters, String authorizationName,
        String redirectUri) {

        Authorization authorization = getAuthorization(componentName, connectionVersion, authorizationName);

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

        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(componentName, connectionVersion);

        ConnectionDefinition.BaseUriFunction baseUriFunction = connectionDefinition.getBaseUri();

        return Optional.ofNullable(baseUriFunction.apply(new InputParametersImpl(connectionParameters)));
    }

    @Override
    public Authorization.AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {

        Authorization authorization = getAuthorization(componentName, connectionVersion, authorizationName);

        return authorization.getType();
    }

    @Override
    public Mono<ConnectionDefinitionDTO> getConnectionDefinitionMono(
        String componentName, int componentVersion) {
        return Mono.just(
            toConnectionDefinitionDTO(
                CollectionUtils.getFirst(
                    componentDefinitions,
                    componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                        componentDefinition.getVersion() == componentVersion,
                    componentDefinition -> OptionalUtils.get(componentDefinition.getConnection()))));
    }

    @Override
    public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono() {
        return Mono.just(connectionDefinitions.stream()
            .map(this::toConnectionDefinitionDTO)
            .toList());
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters,
        String authorizationName) {

        Authorization authorization = getAuthorization(componentName, connectionVersion, authorizationName);

        AuthorizationUrlFunction authorizationUrlFunction = authorization.getAuthorizationUrl();
        ClientIdFunction clientIdFunction = authorization.getClientId();
        ScopesFunction scopesFunction = authorization.getScopes();

        return new OAuth2AuthorizationParametersDTO(
            authorizationUrlFunction.apply(new InputParametersImpl(connectionParameters)),
            clientIdFunction.apply(new InputParametersImpl(connectionParameters)),
            scopesFunction.apply(new InputParametersImpl(connectionParameters)));
    }

    @Override
    public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = CollectionUtils.getFirst(
            componentDefinitions,
            curComponentDefinition -> Objects.equals(curComponentDefinition.getName(), componentName) &&
                curComponentDefinition.getVersion() == componentVersion);

        return Mono.just(
            CollectionUtils.map(
                CollectionUtils.concatDistinct(
                    applyFilterCompatibleConnectionDefinitions(componentDefinition, connectionDefinitions),
                    List.of(OptionalUtils.get(componentDefinition.getConnection()))),
                this::toConnectionDefinitionDTO));
    }

    private Authorization getAuthorization(String componentName, int connectionVersion, String authorizationName) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(componentName, connectionVersion);

        return connectionDefinition.getAuthorization(authorizationName);
    }

    private ConnectionDefinition getComponentConnectionDefinition(String componentName, int connectionVersion) {
        return CollectionUtils.getFirst(
            componentDefinitions,
            componentDefinition -> componentDefinition.getConnection()
                .map(connectionDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion)
                .orElse(false),
            componentDefinition -> OptionalUtils.get(componentDefinition.getConnection()));
    }

    public List<ConnectionDefinition> applyFilterCompatibleConnectionDefinitions(
        ComponentDefinition componentDefinition, List<ConnectionDefinition> connectionDefinitions) {

        return componentDefinition.getFilterCompatibleConnectionDefinitions()
            .map(filterCompatibleConnectionDefinitionsFunction -> filterCompatibleConnectionDefinitionsFunction
                .apply(componentDefinition, connectionDefinitions))
            .orElse(Collections.emptyList());
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
