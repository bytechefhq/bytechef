
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

package com.bytechef.hermes.connection.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.connection.web.rest.model.ConnectionModel;
import com.bytechef.hermes.connection.web.rest.model.PutConnectionRequestModel;
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
public class ConnectionController implements ConnectionsApi {

    private final ConversionService conversionService;
    private final ConnectionService connectionService;

    public ConnectionController(ConversionService conversionService, ConnectionService connectionService) {
        this.conversionService = conversionService;
        this.connectionService = connectionService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteConnection(String id, ServerWebExchange exchange) {
        connectionService.delete(id);

        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> getConnection(String id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
            conversionService.convert(connectionService.getConnection(id), ConnectionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionModel>>> getConnections(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(connectionService.getConnections()
            .stream()
            .map(connection -> conversionService.convert(connection, ConnectionModel.class))
            .toList())));
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> postConnection(
        Mono<ConnectionModel> connectionModelMono, ServerWebExchange exchange) {

        return connectionModelMono.map(connectionModel -> ResponseEntity.ok(conversionService.convert(
            connectionService.create(connectionModel.getName(), connectionModel.getComponentName(),
                connectionModel.getComponentVersion(), connectionModel.getAuthorizationName(),
                connectionModel.getParameters()),
            ConnectionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> putConnection(
        String id, Mono<PutConnectionRequestModel> putConnectionRequestModelMono, ServerWebExchange exchange) {
        return putConnectionRequestModelMono
            .map(putConnectionRequestModel -> ResponseEntity.ok(conversionService.convert(
                connectionService.update(id, putConnectionRequestModel.getName()), ConnectionModel.class)));
    }
}
