
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
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

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
    public Mono<ComponentDefinition> getComponentDefinitionMono(String name, Integer version) {
        return Mono.just(
            componentDefinitionFactories.stream()
                .map(ComponentDefinitionFactory::getDefinition)
                .filter(componentDefinition -> name.equalsIgnoreCase(componentDefinition.getName())
                    && version == componentDefinition.getVersion())
                .findFirst()
                .orElseThrow());
    }

    @Override
    public Mono<List<ComponentDefinition>> getComponentDefinitionsMono() {
        return Mono.just(
            componentDefinitionFactories.stream()
                .map(ComponentDefinitionFactory::getDefinition)
                .collect(Collectors.toList()));
    }

    @Override
    public Mono<List<ComponentDefinition>> getComponentDefinitionsMono(String name) {
        return Mono.just(
            componentDefinitionFactories.stream()
                .map(ComponentDefinitionFactory::getDefinition)
                .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
                .collect(Collectors.toList()));
    }
}
