
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
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.InputParametersImpl;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.definition.registry.dto.AuthorizationDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

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
        this.connectionDefinitions = CollectionUtils.mapDistinct(
            componentDefinitions,
            ComponentDefinition::getConnection,
            Objects::nonNull);
    }

    @Override
    public boolean connectionExists(String componentName, int connectionVersion) {
        return componentDefinitions.stream()
            .anyMatch(componentDefinition -> {
                ConnectionDefinition connectionDefinition = componentDefinition.getConnection();

                return componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion;
            });
    }

    @Override
    public void executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters, String authorizationName,
        AuthorizationContext authorizationContext) {

        Authorization authorization = getAuthorization(componentName, connectionVersion, authorizationName);

        Authorization.ApplyConsumer applyConsumer = authorization.getApply();

        // TODO Add url and httpVerb values
        applyConsumer.accept(new InputParametersImpl(connectionParameters), authorizationContext, null, null);
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters, String authorizationName,
        String redirectUri) {

        Authorization authorization = getAuthorization(componentName, connectionVersion, authorizationName);

        Authorization.AuthorizationCallbackFunction authorizationCallbackFunction = authorization
            .getAuthorizationCallback();

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
    public Mono<ConnectionDefinitionDTO> getComponentConnectionDefinitionMono(
        String componentName, int componentVersion) {
        return Mono.just(
            toConnectionDefinitionDTO(
                CollectionUtils.getFirst(
                    componentDefinitions,
                    componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                        componentDefinition.getVersion() == componentVersion,
                    ComponentDefinition::getConnection)));
    }

    @Override
    public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono() {
        return Mono.just(CollectionUtils.map(connectionDefinitions, this::toConnectionDefinitionDTO));
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters,
        String authorizationName) {

        Authorization authorization = getAuthorization(componentName, connectionVersion, authorizationName);

        Authorization.AuthorizationUrlFunction authorizationUrlFunction = authorization.getAuthorizationUrl();
        Authorization.ClientIdFunction clientIdFunction = authorization.getClientId();
        Authorization.ScopesFunction scopesFunction = authorization.getScopes();

        return new OAuth2AuthorizationParametersDTO(
            authorizationUrlFunction.apply(new InputParametersImpl(connectionParameters)),
            clientIdFunction.apply(new InputParametersImpl(connectionParameters)),
            scopesFunction.apply(new InputParametersImpl(connectionParameters)));
    }

    @Override
    public Mono<List<ConnectionDefinitionDTO>> getComponentConnectionDefinitionsMono(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = CollectionUtils.getFirst(
            componentDefinitions,
            curComponentDefinition -> Objects.equals(curComponentDefinition.getName(), componentName) &&
                curComponentDefinition.getVersion() == componentVersion);

        return Mono.just(
            CollectionUtils.map(
                CollectionUtils.concatDistinct(
                    componentDefinition.applyFilterCompatibleConnectionDefinitions(
                        componentDefinition, connectionDefinitions),
                    List.of(componentDefinition.getConnection())),
                this::toConnectionDefinitionDTO));
    }

    private Authorization getAuthorization(String componentName, int connectionVersion, String authorizationName) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(componentName, connectionVersion);

        return connectionDefinition.getAuthorization(authorizationName);
    }

    private ConnectionDefinition getComponentConnectionDefinition(String componentName, int connectionVersion) {
        return CollectionUtils.getFirst(
            componentDefinitions,
            componentDefinition -> {
                ConnectionDefinition connectionDefinition = componentDefinition.getConnection();

                return componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion;
            },
            ComponentDefinition::getConnection);
    }

    private List<AuthorizationDTO> toAuthorizationDTOs(List<? extends Authorization> authorizations) {
        return CollectionUtils.map(
            authorizations,
            authorization -> new AuthorizationDTO(
                authorization.getDisplay(), authorization.getName(), authorization.getProperties(),
                authorization.getType()));
    }

    private ConnectionDefinitionDTO toConnectionDefinitionDTO(ConnectionDefinition connectionDefinition) {
        return new ConnectionDefinitionDTO(
            connectionDefinition.isAuthorizationRequired(),
            toAuthorizationDTOs(connectionDefinition.getAuthorizations()), connectionDefinition.getDisplay(),
            connectionDefinition.getName(), connectionDefinition.getProperties(), connectionDefinition.getResources(),
            connectionDefinition.getVersion());
    }
}
