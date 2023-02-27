
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

package com.bytechef.hermes.definition.registry.facade.impl;

import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionFacadeImpl implements ComponentDefinitionFacade {

    private final ConnectionService connectionService;
    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionFacadeImpl(
        ConnectionService connectionService, ComponentDefinitionService componentDefinitionService) {

        this.connectionService = connectionService;
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public Mono<List<ComponentDefinition>> getComponentDefinitions(
        Boolean connectionDefinitions, Boolean connectionInstances) {

        List<Connection> connections = connectionService.getConnections(null, null);

        return componentDefinitionService.getComponentDefinitionsMono()
            .map(componentDefinitions -> componentDefinitions.stream()
                .filter(componentDefinition -> {
                    if (connectionDefinitions == null && connectionInstances == null) {
                        return true;
                    }
                    if (connectionDefinitions != null && connectionDefinitions &&
                        connectionInstances != null && connectionInstances) {

                        if (componentDefinition.getConnection() == null) {
                            return false;
                        } else {
                            return connections.stream()
                                .anyMatch(connection -> Objects.equals(connection.getComponentName(),
                                    componentDefinition.getName()));
                        }
                    } else if (connectionDefinitions != null && connectionDefinitions) {
                        return componentDefinition.getConnection() != null;
                    } else if (connectionInstances != null && connectionInstances) {
                        return connections.stream()
                            .anyMatch(connection -> Objects.equals(connection.getComponentName(),
                                componentDefinition.getName()));
                    } else {
                        return false;
                    }
                })
                .distinct()
                .toList());
    }
}
