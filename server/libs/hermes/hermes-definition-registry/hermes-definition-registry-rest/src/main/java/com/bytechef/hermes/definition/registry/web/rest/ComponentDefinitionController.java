
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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentOperationRequestModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
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

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ActionDefinitionService actionDefinitionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConversionService conversionService;
    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionController(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        ConnectionDefinitionService connectionDefinitionService, ConversionService conversionService,
        ComponentDefinitionFacade componentDefinitionFacade, ComponentDefinitionService componentDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.actionDefinitionService = actionDefinitionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.componentDefinitionFacade = componentDefinitionFacade;
        this.conversionService = conversionService;
        this.componentDefinitionService = componentDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public Mono<ResponseEntity<ComponentDefinitionModel>> getComponentDefinition(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            conversionService.convert(
                componentDefinitionService.getComponentDefinition(componentName, componentVersion),
                ComponentDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ActionDefinitionModel>> getComponentActionDefinition(
        String componentName, Integer componentVersion, String actionName, ServerWebExchange exchange) {

        return Mono.just(
            conversionService.convert(
                actionDefinitionService.getComponentActionDefinition(actionName, componentName, componentVersion),
                ActionDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ActionDefinitionBasicModel>>> getComponentActionDefinitions(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            actionDefinitionService.getComponentActionDefinitions(componentName, componentVersion)
                .stream()
                .map(actionDefinition -> conversionService.convert(
                    actionDefinition, ActionDefinitionBasicModel.class))
                .toList())
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<String>> getComponentActionEditorDescription(
        String componentName, Integer componentVersion, String actionName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> actionDefinitionFacade.executeEditorDescription(
                actionName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<PropertyModel>>> getComponentActionOutputSchema(
        String componentName, Integer componentVersion, String actionName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> CollectionUtils.map(
                actionDefinitionFacade.executeOutputSchema(
                    actionName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                    componentOperationRequestModel.getConnectionId()),
                property -> conversionService.convert(property, PropertyModel.class)))
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Object>> getComponentActionSampleOutput(
        String componentName, Integer componentVersion, String actionName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> triggerDefinitionFacade.executeSampleOutput(
                actionName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<PropertyModel>>> getComponentActionPropertyDynamicProperties(
        String componentName, Integer componentVersion, String actionName, String propertyName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> CollectionUtils.map(
                actionDefinitionFacade.executeDynamicProperties(
                    propertyName, actionName, componentName, componentVersion,
                    componentOperationRequestModel.getParameters(),
                    componentOperationRequestModel.getConnectionId()),
                option -> conversionService.convert(option, PropertyModel.class)))
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<OptionModel>>> getComponentActionPropertyOptions(
        String componentName, Integer componentVersion, String actionName, String propertyName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> CollectionUtils.map(
                actionDefinitionFacade.executeOptions(
                    propertyName, actionName, componentName, componentVersion,
                    componentOperationRequestModel.getParameters(), componentOperationRequestModel.getConnectionId()),
                option -> conversionService.convert(option, OptionModel.class)))
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ConnectionDefinitionModel>> getComponentConnectionDefinition(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            conversionService.convert(
                connectionDefinitionService.getConnectionDefinition(componentName, componentVersion),
                ConnectionDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionDefinitionBasicModel>>> getComponentConnectionDefinitions(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion)
                .stream()
                .map(connectionDefinition -> conversionService.convert(
                    connectionDefinition, ConnectionDefinitionBasicModel.class))
                .toList())
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions, ServerWebExchange exchange) {

        return Mono.just(
            componentDefinitionFacade.getComponentDefinitions(
                actionDefinitions, connectionDefinitions, connectionInstances, triggerDefinitions)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList())
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitionVersions(
        String componentName, ServerWebExchange exchange) {

        return Mono.just(
            componentDefinitionService.getComponentDefinitions(componentName)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList())
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TriggerDefinitionModel>> getComponentTriggerDefinition(
        String componentName, Integer componentVersion, String triggerName, ServerWebExchange exchange) {

        return Mono.just(
            conversionService.convert(
                triggerDefinitionService.getTriggerDefinition(triggerName, componentName, componentVersion),
                TriggerDefinitionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<TriggerDefinitionBasicModel>>> getComponentTriggerDefinitions(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion)
                .stream()
                .map(triggerDefinition -> conversionService.convert(
                    triggerDefinition, TriggerDefinitionBasicModel.class))
                .toList())
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<String>> getComponentTriggerEditorDescription(
        String componentName, Integer componentVersion, String triggerName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> triggerDefinitionFacade.executeEditorDescription(
                triggerName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<PropertyModel>>> getComponentTriggerOutputSchema(
        String componentName, Integer componentVersion, String triggerName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> CollectionUtils.map(
                triggerDefinitionFacade.executeOutputSchema(
                    triggerName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                    componentOperationRequestModel.getConnectionId()),
                property -> conversionService.convert(property, PropertyModel.class)))
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Object>> getComponentTriggerSampleOutput(
        String componentName, Integer componentVersion, String triggerName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> triggerDefinitionFacade.executeSampleOutput(
                triggerName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<PropertyModel>>> getComponentTriggerPropertyDynamicProperties(
        String componentName, Integer componentVersion, String triggerName, String propertyName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> CollectionUtils.map(
                triggerDefinitionFacade.executeDynamicProperties(
                    propertyName, triggerName, componentName, componentVersion,
                    componentOperationRequestModel.getParameters(),
                    componentOperationRequestModel.getConnectionId()),
                option -> conversionService.convert(option, PropertyModel.class)))
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<OptionModel>>> getComponentTriggerPropertyOptions(
        String componentName, Integer componentVersion, String triggerName, String propertyName,
        Mono<ComponentOperationRequestModel> componentOperationRequestModelMono, ServerWebExchange exchange) {

        return componentOperationRequestModelMono.map(
            componentOperationRequestModel -> CollectionUtils.map(
                triggerDefinitionFacade.executeOptions(
                    propertyName, triggerName, componentName, componentVersion,
                    componentOperationRequestModel.getParameters(),
                    componentOperationRequestModel.getConnectionId()),
                option -> conversionService.convert(option, OptionModel.class)))
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok);
    }
}
