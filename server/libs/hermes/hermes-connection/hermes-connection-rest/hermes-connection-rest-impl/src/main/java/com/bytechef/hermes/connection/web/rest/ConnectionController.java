
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
import com.bytechef.hermes.connection.config.OAuth2Properties;
import com.bytechef.hermes.connection.dto.ConnectionDTO;
import com.bytechef.hermes.connection.facade.ConnectionFacade;
import com.bytechef.hermes.connection.web.rest.model.ConnectionModel;
import com.bytechef.hermes.connection.web.rest.model.OAuth2AuthorizationParametersModel;
import com.bytechef.hermes.connection.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;
    private final OAuth2Properties oAuth2Properties;

    public ConnectionController(
        ConnectionDefinitionService connectionDefinitionService, ConnectionFacade connectionFacade,
        ConversionService conversionService, OAuth2Properties oAuth2Properties) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
        this.oAuth2Properties = oAuth2Properties;
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> createConnection(
        Mono<ConnectionModel> connectionModelMono, ServerWebExchange exchange) {

        return connectionModelMono.map(connectionModel -> ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.create(
                    conversionService.convert(connectionModel, ConnectionDTO.class)),
                ConnectionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteConnection(Long id, ServerWebExchange exchange) {
        connectionFacade.delete(id);

        return Mono.empty();
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<ConnectionModel>> getConnection(Long id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
            conversionService.convert(connectionFacade.getConnection(id), ConnectionModel.class)
                .parameters(null)));
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<Flux<ConnectionModel>>> getConnections(
        List<String> componentNames, List<Long> tagIds, ServerWebExchange exchange) {

        return Mono.just(ResponseEntity.ok(Flux.fromIterable(connectionFacade.getConnections(componentNames, tagIds)
            .stream()
            .map(connection -> conversionService.convert(connection, ConnectionModel.class)
                .parameters(null))
            .toList())));
    }

    @Override
    public Mono<ResponseEntity<OAuth2AuthorizationParametersModel>> getConnectionOAuth2AuthorizationParameters(
        Mono<ConnectionModel> connectionModelMono, ServerWebExchange exchange) {

        return connectionModelMono
            .map(connectionModel -> conversionService.convert(connectionModel, ConnectionDTO.class))
            .map(ConnectionDTO::toConnection)
            .map(connection -> connectionDefinitionService.getOAuth2Parameters(
                oAuth2Properties.checkPredefinedApp(connection)))
            .map(oAuth2AuthorizationParametersDTO -> conversionService.convert(
                oAuth2AuthorizationParametersDTO, OAuth2AuthorizationParametersModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ConnectionModel>> updateConnection(
        Long id, Mono<ConnectionModel> connectionModelMono, ServerWebExchange exchange) {

        return connectionModelMono.map(connectionModel -> ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.update(
                    conversionService.convert(connectionModel.id(id), ConnectionDTO.class)),
                ConnectionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Void>> updateConnectionTags(
        Long id, Mono<UpdateTagsRequestModel> updateConnectionTagsRequestModelMono,
        ServerWebExchange exchange) {

        return updateConnectionTagsRequestModelMono.map(putConnectionTagsRequestModel -> {
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
