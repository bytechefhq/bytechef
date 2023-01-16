
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

package com.bytechef.hermes.component.service.local;

import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Component
public class LocalComponentDefinitionService implements ComponentDefinitionService {

    private final List<ComponentDefinitionFactory> componentDefinitionFactories;

    @SuppressFBWarnings("EI2")
    public LocalComponentDefinitionService(List<ComponentDefinitionFactory> componentDefinitionFactories) {
        this.componentDefinitionFactories = componentDefinitionFactories;
    }

    @Override
    public Flux<ComponentDefinition> getComponentDefinitions() {
        return Flux.fromIterable(componentDefinitionFactories.stream()
            .map(ComponentDefinitionFactory::getDefinition)
            .collect(Collectors.toList()));
    }

    @Override
    public Mono<ComponentDefinition> getComponentDefinition(String name) {
        return getComponentDefinitions()
            .collectList()
            .flatMapMany(Flux::fromIterable)
            .filter(componentDefinition -> name.equalsIgnoreCase(componentDefinition.getName()))
            .next();
    }

    @Override
    public Mono<ActionDefinition> getComponentDefinitionAction(String componentName, String actionName) {
        return getComponentDefinitions()
            .collectList()
            .flatMapMany(Flux::fromIterable)
            .filter(componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()))
            .next()
            .map(ComponentDefinition::getActions)
            .flatMapMany(Flux::fromIterable)
            .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
            .map(actionDefinition -> (ActionDefinition) actionDefinition)
            .next();
    }

    @Override
    public Flux<ConnectionDefinition> getConnectionDefinitions() {
        return Flux.fromIterable(
            componentDefinitionFactories.stream()
                .map(ComponentDefinitionFactory::getDefinition)
                .map(ComponentDefinition::getConnection)
                .filter(Objects::nonNull)
                .toList());
    }
}
