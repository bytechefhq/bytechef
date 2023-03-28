package com.bytechef.hermes.connection.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.connection.facade.ConnectionFacade;
import com.bytechef.tag.web.rest.model.TagModel;
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
public class ConnectionTagController implements ConnectionTagsApi {

    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;

    public ConnectionTagController(ConnectionFacade connectionFacade, ConversionService conversionService) {
        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
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
}
