
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
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionModel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
@ConditionalOnExpression("'${spring.application.name}'=='server-app' or '${spring.application.name}'=='worker-service-app'")
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class LocalConnectionDefinitionController implements ConnectionDefinitionsApi {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConversionService conversionService;

    public LocalConnectionDefinitionController(
        ComponentDefinitionService componentDefinitionService, ConversionService conversionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.conversionService = conversionService;
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionDefinitionModel>>> getConnectionDefinition(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return ConnectionDefinitionsApi.super.getConnectionDefinition(componentName, componentVersion, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionDefinitionBasicModel>>> getConnectionDefinitions(
        ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                componentDefinitionService.getConnectionDefinitions()
                    .mapNotNull(connectionDefinition -> conversionService.convert(connectionDefinition,
                        ConnectionDefinitionBasicModel.class))));
    }
}
