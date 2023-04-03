
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
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
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
        this.connectionDefinitions = componentDefinitions.stream()
            .map(ComponentDefinition::getConnection)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
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
        Connection connection, Authorization.AuthorizationContext authorizationContext) {

        if (StringUtils.hasText(connection.getAuthorizationName())) {
            Authorization authorization = getAuthorization(
                connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

            Authorization.ApplyConsumer applyConsumer = authorization.getApply();

            applyConsumer.accept(authorizationContext, this.toContextConnection(connection));
        }
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        Connection connection, String redirectUri) {

        Authorization authorization = getAuthorization(
            connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

        Authorization.AuthorizationCallbackFunction authorizationCallbackFunction = authorization
            .getAuthorizationCallback();

        return authorizationCallbackFunction.apply(
            this.toContextConnection(connection), connection.getParameter(Authorization.CODE), redirectUri,
            null // TODO pkce verifier
        );
    }

    @Override
    public Optional<String> fetchBaseUri(Connection connection) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(
            connection.getComponentName(), connection.getConnectionVersion());

        ConnectionDefinition.BaseUriFunction baseUriFunction = connectionDefinition.getBaseUri();

        return Optional.ofNullable(baseUriFunction.apply(this.toContextConnection(connection)));
    }

    @Override
    public Authorization.AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {

        Authorization authorization = getAuthorization(authorizationName, componentName, connectionVersion);

        return authorization.getType();
    }

    public ConnectionDefinition getComponentConnectionDefinition(String componentName, int connectionVersion) {
        return CollectionUtils.getFirst(
            componentDefinitions,
            componentDefinition -> {
                ConnectionDefinition connectionDefinition = componentDefinition.getConnection();

                return componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion;
            },
            ComponentDefinition::getConnection);
    }

    @Override
    public Mono<ConnectionDefinition> getComponentConnectionDefinitionMono(String componentName, int componentVersion) {
        return Mono.just(CollectionUtils.getFirst(
            componentDefinitions,
            componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                componentDefinition.getVersion() == componentVersion,
            ComponentDefinition::getConnection));
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono() {
        return Mono.just(connectionDefinitions);
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(Connection connection) {
        Authorization authorization = getAuthorization(
            connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

        Authorization.AuthorizationUrlFunction authorizationUrlFunction = authorization.getAuthorizationUrl();
        Authorization.ClientIdFunction clientIdFunction = authorization.getClientId();
        Authorization.ScopesFunction scopesFunction = authorization.getScopes();

        Context.Connection contextConnection = toContextConnection(connection);

        return new OAuth2AuthorizationParametersDTO(
            authorizationUrlFunction.apply(contextConnection), clientIdFunction.apply(contextConnection),
            scopesFunction.apply(contextConnection));
    }

    @Override
    public Mono<List<ConnectionDefinition>> getComponentConnectionDefinitionsMono(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = CollectionUtils.getFirst(
            componentDefinitions,
            curComponentDefinition -> Objects.equals(curComponentDefinition.getName(), componentName) &&
                curComponentDefinition.getVersion() == componentVersion);

        return Mono.just(
            CollectionUtils.concatDistinct(
                componentDefinition.applyFilterCompatibleConnectionDefinitions(
                    componentDefinition, connectionDefinitions),
                List.of(componentDefinition.getConnection())));
    }

    @Override
    public Context.Connection toContextConnection(Connection connection) {
        return new ContextConnection(connection);
    }

    private Authorization getAuthorization(String authorizationName, String componentName, int connectionVersion) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(componentName, connectionVersion);

        return connectionDefinition.getAuthorization(authorizationName);
    }

    private class ContextConnection implements Context.Connection {

        private Connection connection;
        private final Map<String, Object> parameters;

        public ContextConnection(Connection connection) {
            this.connection = connection;
            this.parameters = connection.getParameters();
        }

        @Override
        public void applyAuthorization(Authorization.AuthorizationContext authorizationContext) {
            executeAuthorizationApply(this.connection, authorizationContext);
        }

        @Override
        public boolean containsParameter(String name) {
            connection.getParameters();

            return parameters.containsKey(name);
        }

        @Override
        public Optional<String> fetchBaseUri() {
            return ConnectionDefinitionServiceImpl.this.fetchBaseUri(connection);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getParameter(String name) {
            return (T) MapValueUtils.get(parameters, name);
        }

        @Override
        public <T> T getParameter(String name, T defaultValue) {
            return MapValueUtils.get(parameters, name, new ParameterizedTypeReference<>() {}, defaultValue);
        }

        @Override
        public String toString() {
            return "ContextConnection{" +
                ", connection=" + connection +
                '}';
        }
    }
}
