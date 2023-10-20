
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
import com.bytechef.hermes.component.service.ComponentDefinitionService;
import com.bytechef.hermes.component.web.rest.model.GetComponentDefinitionRequestModel;
import com.bytechef.hermes.component.web.rest.model.GetComponentDefinitionsRequestModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.stream.Collectors;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "component-definitions")
public class ComponentDefinitionController {

    private final ConversionService conversionService;
    private final ComponentDefinitionService componentDefinitionService;

    public ComponentDefinitionController(ConversionService conversionService,
        ComponentDefinitionService componentDefinitionService) {
        this.conversionService = conversionService;
        this.componentDefinitionService = componentDefinitionService;
    }

    /**
     * GET /definitions/components Get all component definitions
     *
     * @return OK (status code 200)
     */
    @Operation(
        description = "Get all component definitions.",
        operationId = "getComponentDefinitions",
        summary = "Get all component definitions.",
        tags = {
            "component-definitions"
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content = {
                    @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = GetComponentDefinitionsRequestModel.class))
                })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/definitions/components",
        produces = {
            "application/json"
        })
    public Mono<ResponseEntity<Flux<GetComponentDefinitionsRequestModel>>> getComponentDefinitions(
        @Parameter(hidden = true) final ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                componentDefinitionService.getComponentDefinitions()
                    .collectList()
                    .flatMapMany(Flux::fromIterable)
                    .map(componentDefinition -> conversionService.convert(componentDefinition,
                        GetComponentDefinitionsRequestModel.class))
                    .collect(Collectors.toList())
                    .flatMapMany(Flux::fromIterable)));
    }

    /**
     * GET /definitions/components/{name} Returns a component definition by name
     *
     * @return OK (status code 200)
     */
    @Operation(
        description = "Get a component definition by name.",
        operationId = "getComponentDefinition",
        summary = "Get a component definition by name.",
        tags = {
            "component-definitions"
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = {
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GetComponentDefinitionRequestModel.class))
            })
        })
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/definitions/components/{name}",
        produces = {
            "application/json"
        })
    public Mono<ResponseEntity<GetComponentDefinitionRequestModel>> getComponentDefinition(
        @Parameter(
            name = "name", description = "The name of the component to get.",
            required = true) @PathVariable("name") String name,
        @Parameter(hidden = true) final ServerWebExchange exchange) {
        return componentDefinitionService.getComponentDefinition(name)
            .map(componentDefinition -> conversionService.convert(componentDefinition,
                GetComponentDefinitionRequestModel.class))
            .map(ResponseEntity::ok);
    }
}
