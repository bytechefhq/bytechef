
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

import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ComponentDefinitionService {

    Mono<ComponentDefinition> getComponentDefinition(String name, Integer version);

    Mono<List<ComponentDefinition>> getComponentDefinitions();

    Mono<List<ComponentDefinition>> getComponentDefinitions(String name);

    Mono<ActionDefinition> getComponentDefinitionAction(
        String componentName, int componentVersion, String actionName);

    Mono<ConnectionDefinition> getConnectionDefinition(String componentName, Integer componentVersion);

    Mono<List<ConnectionDefinition>> getConnectionDefinitions();

    Mono<List<ConnectionDefinition>> getConnectionDefinitions(String componentName);
}
