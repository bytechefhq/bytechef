
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
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.facade.ConnectionFacade;
import com.bytechef.hermes.connection.web.rest.model.ConnectionModel;
import com.bytechef.hermes.connection.web.rest.model.PutConnectionTagsRequestModel;
import com.bytechef.hermes.connection.web.rest.model.TagModel;
import com.bytechef.tag.domain.Tag;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class ConnectionController implements ConnectionsApi {

    private final ConversionService conversionService;
    private final ConnectionFacade connectionFacade;

    public ConnectionController(ConversionService conversionService, ConnectionFacade connectionFacade) {
        this.conversionService = conversionService;
        this.connectionFacade = connectionFacade;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteConnection(Long id, ServerWebExchange exchange) {
        connectionFacade.delete(id);

        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> getConnection(Long id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
            conversionService.convert(connectionFacade.getConnection(id), ConnectionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionModel>>> getConnections(
        List<String> componentNames, List<Long> tagIds, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(connectionFacade.getConnections(componentNames, tagIds)
            .stream()
            .map(connection -> conversionService.convert(connection, ConnectionModel.class))
            .toList())));
    }

    @Override
    public Mono<ResponseEntity<Flux<TagModel>>> getConnectionTags(ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    connectionFacade.getConnectionTags()
                        .stream()
                        .map(tag -> conversionService.convert(tag, TagModel.class))
                        .toList())));
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> postConnection(
        Mono<ConnectionModel> connectionModelMono, ServerWebExchange exchange) {

        return connectionModelMono.map(connectionModel -> ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.create(
                    conversionService.convert(connectionModel, Connection.class)),
                ConnectionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> putConnection(
        Long id, Mono<ConnectionModel> connectionModelMono, ServerWebExchange exchange) {
        return connectionModelMono.map(connectionModel -> ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.update(
                    conversionService.convert(connectionModel.id(id), Connection.class)),
                ConnectionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Void>> putConnectionTags(
        Long id, Mono<PutConnectionTagsRequestModel> putConnectionTagsRequestModelMono, ServerWebExchange exchange) {

        return putConnectionTagsRequestModelMono.map(putConnectionTagsRequestModel -> {
            List<TagModel> tagModels = putConnectionTagsRequestModel.getTags();

            connectionFacade.update(
                id,
                tagModels.stream()
                    .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                    .toList());

            return ResponseEntity.noContent()
                .build();
        });
    }
}
