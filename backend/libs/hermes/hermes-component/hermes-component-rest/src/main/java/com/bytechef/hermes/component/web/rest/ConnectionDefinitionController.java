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

package com.bytechef.hermes.component.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.web.rest.model.ConnectionDefinitionModel;
import com.bytechef.hermes.component.web.rest.model.ConnectionsModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@SuppressFBWarnings("EI")
public class ConnectionDefinitionController implements ConnectionDefinitionControllerApi {

    private final ConversionService conversionService;
    private final List<ComponentDefinitionFactory> componentDefinitionFactories;

    public ConnectionDefinitionController(
            ConversionService conversionService, List<ComponentDefinitionFactory> componentDefinitionFactories) {
        this.conversionService = conversionService;
        this.componentDefinitionFactories = componentDefinitionFactories;
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionsModel>>> getConnectionDefinitions(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(componentDefinitionFactories.stream()
                .map(ComponentDefinitionFactory::getDefinition)
                .filter(componentDefinition -> componentDefinition.getConnections() != null)
                .map(componentDefinition -> new ConnectionsModel()
                        .connections(componentDefinition.getConnections().stream()
                                .map(connectionDefinition -> conversionService.convert(
                                        connectionDefinition, ConnectionDefinitionModel.class))
                                .toList())
                        .name(componentDefinition.getName())
                        .version(componentDefinition.getVersion()))
                .toList())));
    }
}
