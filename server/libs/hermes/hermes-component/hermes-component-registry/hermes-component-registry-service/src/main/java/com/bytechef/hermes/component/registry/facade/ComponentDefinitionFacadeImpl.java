
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

package com.bytechef.hermes.component.registry.facade;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import com.bytechef.commons.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Ivica Cardic
 */
@Service
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
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions, List<String> include) {

        List<Connection> connections = connectionService.getConnections();

        List<ComponentDefinition> components = componentDefinitionService.getComponentDefinitions()
            .stream()
            .filter(
                filter(
                    actionDefinitions, connectionDefinitions, connectionInstances, triggerDefinitions, include,
                    connections))
            .distinct()
            .toList();

        if (include != null && !include.isEmpty()) {
            components = new ArrayList<>(components);

            components.sort(Comparator.comparing(component -> include.indexOf(component.getName())));
        }

        return components;
    }

    private static Predicate<ComponentDefinition> filter(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions, List<String> include, List<Connection> connections) {

        return componentDefinition -> {
            if (include != null && !include.isEmpty() && !include.contains(componentDefinition.getName())) {
                return false;
            }

            if (actionDefinitions != null && CollectionUtils.isEmpty(componentDefinition.getActions())) {
                return false;
            }

            if (connectionDefinitions != null && !OptionalUtils.isPresent(componentDefinition.getConnection())) {
                return false;
            }

            if (connectionInstances != null && noneMatch(connections, componentDefinition)) {
                return false;
            }

            if (triggerDefinitions != null && CollectionUtils.isEmpty(componentDefinition.getTriggers())) {
                return false;
            }

            return true;
        };
    }

    private static boolean noneMatch(List<Connection> connections, ComponentDefinition componentDefinition) {
        return connections.stream()
            .noneMatch(connection -> Objects.equals(connection.getComponentName(), componentDefinition.getName()));
    }
}
