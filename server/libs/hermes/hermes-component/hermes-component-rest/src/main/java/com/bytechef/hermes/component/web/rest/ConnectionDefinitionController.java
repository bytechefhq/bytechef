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
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
@Tag(name = "connection-definitions")
public class ConnectionDefinitionController {

    private final List<ComponentDefinitionFactory> componentDefinitionFactories;

    public ConnectionDefinitionController(List<ComponentDefinitionFactory> componentDefinitionFactories) {
        this.componentDefinitionFactories = componentDefinitionFactories;
    }

    /**
     * GET /definitions/connections
     * Returns all connection definitions
     *
     * @return OK (status code 200)
     */
    @Operation(
            description = "Returns all connection definitions.",
            operationId = "getConnectionDefinitions",
            summary = "Returns all connection definitions.",
            tags = {"connection-definitions"},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "OK",
                        content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))
                        })
            })
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/definitions/connections",
            produces = {"application/json"})
    public Mono<ResponseEntity<Flux<ConnectionDefinition>>> getConnectionDefinitions(
            @Parameter(hidden = true) final ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(componentDefinitionFactories.stream()
                .map(ComponentDefinitionFactory::getDefinition)
                .filter(componentDefinition -> componentDefinition.getConnection() != null)
                .map(ComponentDefinition::getConnection)
                .toList())));
    }
}
