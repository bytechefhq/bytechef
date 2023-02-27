
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
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceImpl implements ActionDefinitionService {

    private final List<ComponentDefinitionFactory> componentDefinitionFactories;

    @SuppressFBWarnings("EI2")
    public ActionDefinitionServiceImpl(List<ComponentDefinitionFactory> componentDefinitionFactories) {
        this.componentDefinitionFactories = componentDefinitionFactories;
    }

    @Override
    public Mono<ActionDefinition> getComponentDefinitionActionMono(
        String componentName, int componentVersion, String actionName) {

        return Mono.just(
            componentDefinitionFactories.stream()
                .map(ComponentDefinitionFactory::getDefinition)
                .filter(componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    componentVersion == componentDefinition.getVersion())
                .flatMap(componentDefinition -> componentDefinition.getActions()
                    .stream())
                .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
                .map(actionDefinition -> (ActionDefinition) actionDefinition)
                .findFirst()
                .orElseThrow());
    }
}
