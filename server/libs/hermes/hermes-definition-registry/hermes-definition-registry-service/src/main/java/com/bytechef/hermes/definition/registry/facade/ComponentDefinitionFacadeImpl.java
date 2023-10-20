
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

package com.bytechef.hermes.definition.registry.facade;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionFacadeImpl implements ComponentDefinitionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions) {

        List<Connection> connections = connectionService.getConnections();

        return componentDefinitionService.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> {
                if (actionDefinitions != null && CollectionUtils.isEmpty(componentDefinition.actions())) {
                    return false;
                }

                if (connectionDefinitions != null && !OptionalUtils.isPresent(componentDefinition.connection())) {
                    return false;
                }

                if (connectionInstances != null && noneMatch(connections, componentDefinition)) {
                    return false;
                }

                if (triggerDefinitions != null && CollectionUtils.isEmpty(componentDefinition.triggers())) {
                    return false;
                }

                return true;
            })
            .distinct()
            .toList();
    }

    private static boolean noneMatch(List<Connection> connections, ComponentDefinitionDTO componentDefinition) {
        return connections.stream()
            .noneMatch(connection -> Objects.equals(connection.getComponentName(), componentDefinition.name()));
    }
}
