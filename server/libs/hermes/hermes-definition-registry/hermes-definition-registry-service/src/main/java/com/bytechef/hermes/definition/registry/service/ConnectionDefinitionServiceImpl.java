
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
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
    public void executeAuthorizationApply(
        Connection connection, Authorization.AuthorizationContext authorizationContext) {

        if (StringUtils.hasText(connection.getAuthorizationName())) {
            Authorization authorization = getAuthorization(
                connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

            BiConsumer<Authorization.AuthorizationContext, Context.Connection> applyConsumer = authorization
                .getApplyConsumer();

            applyConsumer.accept(authorizationContext, connection.toContextConnection());
        }
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        Connection connection, String redirectUri) {

        Authorization authorization = getAuthorization(
            connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

        Authorization.QuadFunction<Context.Connection, String, String, String, Authorization.AuthorizationCallbackResponse> authorizationCallbackFunction = authorization
            .getAuthorizationCallbackFunction();

        return authorizationCallbackFunction.apply(
            connection.toContextConnection(),
            connection.getParameter(Authorization.CODE),
            redirectUri,
            null // TODO pkce verifier
        );
    }

    @Override
    public Optional<String> fetchBaseUri(Connection connection) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(
            connection.getComponentName(), connection.getConnectionVersion());

        return Optional.ofNullable(
            connectionDefinition.getBaseUriFunction()
                .apply(connection.toContextConnection()));
    }

    @Override
    public Authorization getAuthorization(String authorizationName, String componentName, int connectionVersion) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(componentName, connectionVersion);

        return connectionDefinition.getAuthorization(authorizationName);
    }

    @Override
    public ConnectionDefinition getComponentConnectionDefinition(String componentName, int componentVersion) {
        return CollectionUtils.getFirst(
            componentDefinitions,
            componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                componentDefinition.getVersion() == componentVersion,
            ComponentDefinition::getConnection);
    }

    @Override
    public Mono<ConnectionDefinition> getComponentConnectionDefinitionMono(String componentName, int componentVersion) {
        return Mono.just(getComponentConnectionDefinition(componentName, componentVersion));
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono() {
        return Mono.just(connectionDefinitions);
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(Connection connection) {
        Authorization authorization = getAuthorization(
            connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

        Function<Context.Connection, String> authorizationUrlFunction = authorization.getAuthorizationUrlFunction();
        Function<Context.Connection, String> clientIdFunction = authorization.getClientIdFunction();
        Function<Context.Connection, List<String>> scopesFunction = authorization.getScopesFunction();

        return new OAuth2AuthorizationParametersDTO(
            authorizationUrlFunction.apply(connection.toContextConnection()),
            clientIdFunction.apply(connection.toContextConnection()),
            scopesFunction.apply(connection.toContextConnection()));
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
                componentDefinition.applyFilterCompatibleConnectionDefinitionsFunction(
                    componentDefinition, connectionDefinitions),
                List.of(componentDefinition.getConnection())));
    }
}
