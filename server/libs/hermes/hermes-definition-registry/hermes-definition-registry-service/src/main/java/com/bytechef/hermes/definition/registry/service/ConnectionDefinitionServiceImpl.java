
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

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
    public void applyAuthorization(Connection connection, Authorization.AuthorizationContext authorizationContext) {
        if (StringUtils.hasText(connection.getAuthorizationName())) {
            ConnectionDefinition connectionDefinition = getConnectionDefinition(connection.getComponentName());

            List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

            authorizations.stream()
                .filter(authorization -> Objects.equals(authorization.getName(), connection.getAuthorizationName()))
                .findFirst()
                .map(Authorization::getApplyConsumer)
                .ifPresent(authorizationConsumer -> authorizationConsumer.accept(authorizationContext,
                    connection.toContextConnection()));
        }
    }

    @Override
    public Optional<String> fetchBaseUri(Connection connection) {
        ConnectionDefinition connectionDefinition = getConnectionDefinition(connection.getComponentName());

        return Optional.ofNullable(
            connectionDefinition.getBaseUriFunction()
                .apply(connection.toContextConnection()));
    }

    @Override
    public Mono<ConnectionDefinition> getConnectionDefinitionMono(String componentName) {
        return Mono.just(getConnectionDefinition(componentName));
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono() {
        return Mono.just(connectionDefinitions);
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(String componentName) {
        return Stream.concat(
            componentDefinitions.stream()
                .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), componentName))
                .map(ComponentDefinition::getFilterCompatibleConnectionDefinitionsFunction)
                .filter(Objects::nonNull)
                .flatMap(filterCompatibleConnectionDefinitionsFunction -> filterCompatibleConnectionDefinitionsFunction
                    .apply(
                        componentDefinitions.stream()
                            .map(ComponentDefinition::getConnection)
                            .filter(Objects::nonNull)
                            .toList())
                    .stream()),
            Stream.of(
                connectionDefinitions.stream()
                    .filter(
                        connectionDefinition -> Objects.equals(connectionDefinition.getComponentName(), componentName))
                    .findFirst()
                    .orElse(null)))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono(String componentName) {
        return Mono.just(getConnectionDefinitions(componentName));
    }

    private ConnectionDefinition getConnectionDefinition(String componentName) {
        return connectionDefinitions.stream()
            .filter(connectionDefinition -> componentName.equalsIgnoreCase(connectionDefinition.getComponentName()))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}
