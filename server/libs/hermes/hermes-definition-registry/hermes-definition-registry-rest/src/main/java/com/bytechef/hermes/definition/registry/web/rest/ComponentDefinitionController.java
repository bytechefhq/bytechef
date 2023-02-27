
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

package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionWithBasicActionsModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
@SuppressFBWarnings("EI")
public class ComponentDefinitionController implements ComponentDefinitionsApi {

    private final ActionDefinitionService actionDefinitionService;
    private final ConversionService conversionService;
    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ComponentDefinitionService componentDefinitionService;

    public ComponentDefinitionController(
        ActionDefinitionService actionDefinitionService, ConversionService conversionService,
        ComponentDefinitionFacade componentDefinitionFacade, ComponentDefinitionService componentDefinitionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.componentDefinitionFacade = componentDefinitionFacade;
        this.conversionService = conversionService;
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public Mono<ResponseEntity<ActionDefinitionModel>> getActionDefinition(
        String componentName, Integer componentVersion, String actionName, ServerWebExchange exchange) {

        return actionDefinitionService.getComponentDefinitionActionMono(componentName, componentVersion, actionName)
            .mapNotNull(actionDefinition -> conversionService.convert(actionDefinition, ActionDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ComponentDefinitionWithBasicActionsModel>> getComponentDefinition(
        String name, Integer version, ServerWebExchange exchange) {

        return componentDefinitionService.getComponentDefinitionMono(name, version)
            .mapNotNull(componentDefinition -> conversionService.convert(
                componentDefinition, ComponentDefinitionWithBasicActionsModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitions(
        Boolean connectionDefinitions, Boolean connectionInstances, ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                componentDefinitionFacade.getComponentDefinitions(connectionDefinitions, connectionInstances)
                    .mapNotNull(componentDefinitions -> componentDefinitions.stream()
                        .map(componentDefinition -> conversionService.convert(
                            componentDefinition, ComponentDefinitionBasicModel.class))
                        .toList())
                    .flatMapMany(Flux::fromIterable)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitionVersions(
        String name, ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                componentDefinitionService.getComponentDefinitionsMono(name)
                    .mapNotNull(componentDefinitions -> componentDefinitions.stream()
                        .map(componentDefinition -> conversionService.convert(
                            componentDefinition, ComponentDefinitionBasicModel.class))
                        .toList())
                    .flatMapMany(Flux::fromIterable)));
    }
}
