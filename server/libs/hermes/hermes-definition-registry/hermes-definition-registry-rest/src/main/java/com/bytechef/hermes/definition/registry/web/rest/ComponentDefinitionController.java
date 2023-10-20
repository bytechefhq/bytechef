
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

import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TriggerDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TriggerDefinitionModel;
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
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class ComponentDefinitionController implements ComponentDefinitionsApi {

    private final ActionDefinitionService actionDefinitionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConversionService conversionService;
    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final TriggerDefinitionService triggerDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionController(
        ActionDefinitionService actionDefinitionService, ConnectionDefinitionService connectionDefinitionService,
        ConversionService conversionService, ComponentDefinitionFacade componentDefinitionFacade,
        ComponentDefinitionService componentDefinitionService, TriggerDefinitionService triggerDefinitionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.componentDefinitionFacade = componentDefinitionFacade;
        this.conversionService = conversionService;
        this.componentDefinitionService = componentDefinitionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public Mono<ResponseEntity<ComponentDefinitionModel>> getComponentDefinition(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return componentDefinitionService.getComponentDefinitionMono(componentName, componentVersion)
            .mapNotNull(
                componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ActionDefinitionModel>> getComponentActionDefinition(
        String componentName, Integer componentVersion, String actionName, ServerWebExchange exchange) {

        return actionDefinitionService.getComponentActionDefinitionMono(actionName, componentName, componentVersion)
            .mapNotNull(actionDefinition -> conversionService.convert(actionDefinition, ActionDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ActionDefinitionBasicModel>>> getComponentActionDefinitions(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            actionDefinitionService.getComponentActionDefinitionsMono(componentName, componentVersion)
                .mapNotNull(
                    connectionDefinitions -> connectionDefinitions.stream()
                        .map(
                            actionDefinition -> conversionService.convert(
                                actionDefinition, ActionDefinitionBasicModel.class))
                        .toList())
                .flatMapMany(Flux::fromIterable))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ConnectionDefinitionModel>> getComponentConnectionDefinition(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return connectionDefinitionService.getConnectionDefinitionMono(componentName, componentVersion)
            .mapNotNull(
                connectionDefinition -> conversionService.convert(
                    connectionDefinition, ConnectionDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionDefinitionBasicModel>>> getComponentConnectionDefinitions(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            connectionDefinitionService.getConnectionDefinitionsMono(componentName, componentVersion)
                .mapNotNull(
                    connectionDefinitions -> connectionDefinitions.stream()
                        .map(
                            connectionDefinition -> conversionService.convert(
                                connectionDefinition, ConnectionDefinitionBasicModel.class))
                        .toList())
                .flatMapMany(Flux::fromIterable))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions, ServerWebExchange exchange) {

        return Mono.just(
            componentDefinitionFacade
                .getComponentDefinitionsMono(
                    actionDefinitions, connectionDefinitions, connectionInstances, triggerDefinitions)
                .mapNotNull(
                    componentDefinitions -> componentDefinitions.stream()
                        .map(
                            componentDefinition -> conversionService.convert(
                                componentDefinition, ComponentDefinitionBasicModel.class))
                        .toList())
                .flatMapMany(Flux::fromIterable))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitionVersions(
        String componentName, ServerWebExchange exchange) {

        return Mono.just(
            componentDefinitionService.getComponentDefinitionsMono(componentName)
                .mapNotNull(
                    componentDefinitions -> componentDefinitions.stream()
                        .map(
                            componentDefinition -> conversionService.convert(
                                componentDefinition, ComponentDefinitionBasicModel.class))
                        .toList())
                .flatMapMany(Flux::fromIterable))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TriggerDefinitionModel>> getComponentTriggerDefinition(
        String componentName, Integer componentVersion, String triggerName, ServerWebExchange exchange) {

        return triggerDefinitionService.getTriggerDefinitionMono(triggerName, componentName, componentVersion)
            .mapNotNull(triggerDefinition -> conversionService.convert(triggerDefinition, TriggerDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<TriggerDefinitionBasicModel>>> getComponentTriggerDefinitions(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion)
                .mapNotNull(
                    connectionDefinitions -> connectionDefinitions.stream()
                        .map(
                            triggerDefinition -> conversionService.convert(
                                triggerDefinition, TriggerDefinitionBasicModel.class))
                        .toList())
                .flatMapMany(Flux::fromIterable))
            .map(ResponseEntity::ok);
    }
}
