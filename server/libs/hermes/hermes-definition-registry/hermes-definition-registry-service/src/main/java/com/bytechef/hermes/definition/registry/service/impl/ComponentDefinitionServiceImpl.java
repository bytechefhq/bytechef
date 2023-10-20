
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

package com.bytechef.hermes.definition.registry.service.impl;

import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceImpl implements ComponentDefinitionService {

    private final List<ComponentDefinitionFactory> componentDefinitionFactories;

    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(List<ComponentDefinitionFactory> componentDefinitionFactories) {
        this.componentDefinitionFactories = componentDefinitionFactories;
    }

    @Override
    public Mono<ComponentDefinition> getComponentDefinition(String name, Integer version) {
        return getComponentDefinitions()
            .flatMapMany(Flux::fromIterable)
            .filter(componentDefinition -> name.equalsIgnoreCase(componentDefinition.getName())
                && version == componentDefinition.getVersion())
            .next();
    }

    @Override
    public Mono<List<ComponentDefinition>> getComponentDefinitions(String name) {
        return Mono.just(componentDefinitionFactories.stream()
            .map(ComponentDefinitionFactory::getDefinition)
            .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
            .collect(Collectors.toList()));
    }

    @Override
    public Mono<List<ComponentDefinition>> getComponentDefinitions() {
        return Mono.just(componentDefinitionFactories.stream()
            .map(ComponentDefinitionFactory::getDefinition)
            .collect(Collectors.toList()));
    }

    @Override
    public Mono<ActionDefinition> getComponentDefinitionAction(
        String componentName, int componentVersion, String actionName) {

        return getComponentDefinitions()
            .flatMapMany(Flux::fromIterable)
            .filter(componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                componentVersion == componentDefinition.getVersion())
            .next()
            .map(ComponentDefinition::getActions)
            .flatMapMany(Flux::fromIterable)
            .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
            .map(actionDefinition -> (ActionDefinition) actionDefinition)
            .next();
    }

    @Override
    public Mono<ConnectionDefinition> getConnectionDefinition(String componentName, Integer componentVersion) {
        return getComponentDefinition(componentName, componentVersion).map(ComponentDefinition::getConnection);
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitions() {
        return Mono.just(
            new ArrayList<>(
                componentDefinitionFactories.stream()
                    .map(ComponentDefinitionFactory::getDefinition)
                    .map(ComponentDefinition::getConnection)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())));
    }
}
